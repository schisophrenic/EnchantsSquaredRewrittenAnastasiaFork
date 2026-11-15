package me.athlaeos.enchantssquared.nativeenchants;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryComposeEvent;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.registry.TypedKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * EnchantsSquared native enchantment registration through Paper's Java
 * registry lifecycle. NO generated datapack and NO separate JSON files.
 *
 * The registry freezes before the ordinary plugin onEnable method runs, so
 * YAML must be read here in bootstrap (and changes require a full restart).
 */
public final class NativeEnchantmentBootstrap implements PluginBootstrap {
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_./-]+");
    private static final String NAMESPACE = "enchantssquared";

    private record Entry(String name, String title, List<String> supportedItems,
                         List<String> conflicts, List<String> slots, int weight,
                         int maximumLevel, int minBase, int minStep,
                         int maxBase, int maxStep, int anvilCost,
                         boolean tableObtainable, boolean tradeable,
                         boolean treasure) {
        TypedKey<Enchantment> typedKey() {
            return EnchantmentKeys.create(Key.key(NAMESPACE, name));
        }
    }

    @Override
    public void bootstrap(BootstrapContext context) {
        YamlConfiguration configuration = loadConfig(context);
        ConfigurationSection entries = configuration.getConfigurationSection("enchantments");
        if (entries == null) {
            context.getLogger().warn("No native enchantments found in config_native_enchants.yml");
            return;
        }
        List<Entry> definitions = new ArrayList<>();
        for (String name : entries.getKeys(false)) {
            ConfigurationSection section = entries.getConfigurationSection(name);
            if (section == null || !section.getBoolean("enabled", true)) continue;
            if (!IDENTIFIER.matcher(name).matches() || name.contains("..")) {
                context.getLogger().warn("Invalid native enchantment ID: " + name);
                continue;
            }
            List<String> supported = section.getStringList("supported_items");
            if (supported.isEmpty()) {
                context.getLogger().warn("Native enchantment " + name + " has no supported_items");
                continue;
            }
            definitions.add(new Entry(
                    name, section.getString("display_name", name),
                    List.copyOf(supported),
                    List.copyOf(section.getStringList("exclusive_set")),
                    List.copyOf(section.getStringList("slots")),
                    Math.max(1, Math.min(1024, section.getInt("weight", 10))),
                    // Retain the vanilla maximum (V/II/I/IV) for each native
                    // family. This is only the normal registry limit; ValhallaMMO
                    // can amplify the level of enchanted items above this.
                    vanillaMaximum(name, section.getInt("max_level", 1)),
                    Math.max(1, section.getInt("min_cost_base", 5)),
                    Math.max(0, section.getInt("min_cost_per_level", 10)),
                    Math.max(1, section.getInt("max_cost_base", 50)),
                    Math.max(0, section.getInt("max_cost_per_level", 10)),
                    Math.max(0, section.getInt("anvil_cost", 4)),
                    section.getBoolean("table_obtainable", false),
                    section.getBoolean("tradeable", false),
                    section.getBoolean("treasure", false)));
        }
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose(), event -> {
            int registered = 0;
            for (Entry definition : definitions) {
                try {
                    RegistryKeySet<ItemType> supported = supportedItems(event, definition.supportedItems());
                    List<TypedKey<Enchantment>> conflicts = new ArrayList<>();
                    for (String id : definition.conflicts()) {
                        if (id.equals(NAMESPACE + ":" + definition.name())) continue;
                        conflicts.add(EnchantmentKeys.create(Key.key(id)));
                    }
                    List<EquipmentSlotGroup> slots = new ArrayList<>();
                    for (String rawSlot : definition.slots()) {
                        EquipmentSlotGroup slot = EquipmentSlotGroup.getByName(rawSlot.toLowerCase(Locale.ROOT));
                        if (slot != null) slots.add(slot);
                    }
                    if (slots.isEmpty()) slots.add(EquipmentSlotGroup.ANY);
                    var conflictSet = RegistrySet.keySet(RegistryKey.ENCHANTMENT, conflicts);
                    event.registry().register(definition.typedKey(), b -> b
                            .description(Component.text(definition.title()))
                            .supportedItems(supported)
                            .weight(definition.weight())
                            .maxLevel(definition.maximumLevel())
                            .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(definition.minBase(), definition.minStep()))
                            .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                    Math.max(definition.minBase(), definition.maxBase()), definition.maxStep()))
                            .anvilCost(definition.anvilCost())
                            .activeSlots(slots)
                            .exclusiveWith(conflictSet));
                    registered++;
                } catch (RuntimeException ex) {
                    // Bootstrap errors are fatal on Paper, so surface an explicit
                    // configuration error instead of silently hiding missing IDs.
                    throw new IllegalStateException("Failed registering native enchant " + definition.name(), ex);
                }
            }
            context.getLogger().info("Registered {} enchantments directly in Paper's registry (no datapack).", registered);
        });

        // Java API tag registration, also in bootstrap: no world datapack.
        // Tiered/perk-locked enchants remain outside enchanting-table and
        // trading tags so players cannot bypass the ValhallaMMO perk checks.
        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT), event -> {
                    List<TypedKey<Enchantment>> table = new ArrayList<>();
                    List<TypedKey<Enchantment>> trades = new ArrayList<>();
                    List<TypedKey<Enchantment>> ordinary = new ArrayList<>();
                    for (Entry definition : definitions) {
                        if (definition.tableObtainable()) table.add(definition.typedKey());
                        if (definition.tradeable()) trades.add(definition.typedKey());
                        if (!definition.treasure()) ordinary.add(definition.typedKey());
                    }
                    if (!table.isEmpty()) event.registrar().addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, table);
                    if (!trades.isEmpty()) event.registrar().addToTag(EnchantmentTagKeys.TRADEABLE, trades);
                    if (!ordinary.isEmpty()) event.registrar().addToTag(EnchantmentTagKeys.NON_TREASURE, ordinary);
                });
    }

    private static RegistryKeySet<ItemType> supportedItems(
            RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> event,
            List<String> requested) {
        // The sample YAML has exactly one Minecraft tag for each enchantment.
        // Support explicit item IDs as an alternative. Mixing multiple tags
        // and individual item IDs needs a separate custom item tag definition.
        if (requested.size() == 1 && requested.get(0).startsWith("#")) {
            return event.getOrCreateTag(TagKey.create(RegistryKey.ITEM, requested.get(0).substring(1)));
        }
        List<TypedKey<ItemType>> keys = new ArrayList<>();
        for (String raw : requested) {
            if (raw.startsWith("#")) throw new IllegalArgumentException(
                    "Use a single #tag OR explicit item IDs in supported_items; mixing is unsupported: " + requested);
            keys.add(RegistryKey.ITEM.typedKey(Key.key(raw)));
        }
        return RegistrySet.keySet(RegistryKey.ITEM, keys);
    }

    private static int vanillaMaximum(String id, int configured) {
        String name = id.replaceFirst("^(lesser|advanced|supreme)_", "");
        return switch (name) {
            case "sharpness", "smite", "bane_of_arthropods", "power" -> 5;
            case "punch", "fire_aspect" -> 2;
            case "flame" -> 1;
            case "protection", "fire_protection", "blast_protection",
                    "projectile_protection", "magic_protection" -> 4;
            default -> Math.max(1, Math.min(255, configured));
        };
    }

    private static YamlConfiguration loadConfig(BootstrapContext context) {
        Path destination = context.getDataDirectory().resolve("config_native_enchants.yml");
        try {
            Files.createDirectories(destination.getParent());
            if (!Files.exists(destination)) {
                try (InputStream in = NativeEnchantmentBootstrap.class.getClassLoader()
                        .getResourceAsStream("config_native_enchants.yml")) {
                    if (in == null) throw new IllegalStateException("Missing bundled config_native_enchants.yml");
                    Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not load native enchant configuration: " + destination, ex);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(destination.toFile());
        // A Paper registry cannot be patched after plugin enable. Merge
        // newly-added bundled enchantments HERE, before the registry is
        // constructed. Preserve existing user values; add only missing keys.
        // This also upgrades servers using the previous 10-enchant test JAR.
        try (InputStream in = NativeEnchantmentBootstrap.class.getClassLoader()
                .getResourceAsStream("config_native_enchants.yml")) {
            if (in == null) throw new IllegalStateException("Bundled native config missing");
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            int newKeys = 0;
            for (String root : List.of("enchantments", "tier_families", "perk_groups",
                    "settings", "display_aliases", "loot_equipment")) {
                ConfigurationSection bundled = defaults.getConfigurationSection(root);
                if (bundled == null) continue;
                for (var entry : bundled.getValues(true).entrySet()) {
                    if (entry.getValue() instanceof ConfigurationSection) continue;
                    String key = root + "." + entry.getKey();
                    if (config.contains(key)) continue;
                    config.set(key, entry.getValue());
                    newKeys++;
                }
            }
            if (!config.contains("blocked_base_enchants")) {
                config.set("blocked_base_enchants",
                        defaults.getStringList("blocked_base_enchants"));
                newKeys++;
            }
            if (newKeys > 0) {
                config.save(destination.toFile());
                context.getLogger().info("Merged {} missing native enchant config keys from bundled defaults "
                        + "(existing values preserved).", newKeys);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not merge native enchant YAML defaults: " + destination, ex);
        }
        return config;
    }
}
