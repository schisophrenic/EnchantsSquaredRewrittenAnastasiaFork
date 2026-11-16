package me.athlaeos.enchantssquared.nativeenchants;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Small, standalone custom enchantment YAML.
 *
 * Both Paper bootstrap and the normal onEnable call this same translator.
 * Only the canonical native registration fields are merged into an in-memory
 * config; no user's native enchant configuration file is overwritten.
 *
 * Changing IDs, material tags or max levels requires a complete restart
 * because the Paper enchant registry freezes before onEnable().
 */
public final class NativeSimpleEnchantConfig {
    private static final Pattern KEY = Pattern.compile("[a-z0-9_./-]+");
    public static final String FILE_NAME = "config_custom_enchants.yml";
    private NativeSimpleEnchantConfig() {}

    public static YamlConfiguration load(Path folder) {
        Path path = folder.resolve(FILE_NAME);
        try {
            Files.createDirectories(folder);
            if (!Files.exists(path)) {
                try (InputStream in = NativeSimpleEnchantConfig.class.getClassLoader()
                        .getResourceAsStream(FILE_NAME)) {
                    if (in == null) throw new IllegalStateException("Missing bundled " + FILE_NAME);
                    Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return YamlConfiguration.loadConfiguration(path.toFile());
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot read " + path, ex);
        }
    }

    public static YamlConfiguration merge(Path dataFolder, YamlConfiguration nativeConfig) {
        YamlConfiguration custom = load(dataFolder);
        ConfigurationSection entries = custom.getConfigurationSection("enchants");
        if (entries == null) return custom;
        for (String id : entries.getKeys(false)) {
            if (!KEY.matcher(id).matches() || id.contains("..")) {
                throw new IllegalArgumentException("Invalid custom enchant id: " + id);
            }
            if (nativeConfig.contains("enchantments." + id)) {
                throw new IllegalArgumentException("Custom enchant id conflicts with existing native enchant: " + id);
            }
            ConfigurationSection entry = entries.getConfigurationSection(id);
            if (entry == null) continue;
            String root = "enchantments." + id + ".";
            String kind = entry.getString("type", "weapon").toLowerCase(Locale.ROOT);
            Map<String, String> tags = Map.of(
                    "weapon", "#minecraft:enchantable/weapon",
                    "armor", "#minecraft:enchantable/armor",
                    "boots", "#minecraft:enchantable/foot_armor",
                    "bow", "#minecraft:enchantable/bow",
                    "mining", "#minecraft:enchantable/mining",
                    "fishing", "#minecraft:enchantable/fishing"
            );
            String tag = tags.get(kind);
            List<String> supported = entry.getStringList("items");
            if (supported.isEmpty()) {
                if (tag == null) throw new IllegalArgumentException("Unknown type for " + id + ": " + kind);
                supported = List.of(tag);
            }
            List<String> slots = entry.getStringList("slots");
            if (slots.isEmpty()) slots = List.of(kind.equals("armor") || kind.equals("boots") ? "armor" : "mainhand");
            String rarity = entry.getString("rarity", "rare").toLowerCase(Locale.ROOT);
            int rarityWeight = switch (rarity) {
                case "common" -> 20;
                case "uncommon" -> 10;
                case "rare" -> 5;
                case "epic" -> 2;
                case "legendary" -> 1;
                default -> throw new IllegalArgumentException("Unknown rarity for " + id + ": " + rarity);
            };
            nativeConfig.set(root + "enabled", entry.getBoolean("enabled", true));
            nativeConfig.set(root + "display_name", entry.getString("name", id.replace('_', ' ')));
            nativeConfig.set(root + "enchant_type", kind);
            nativeConfig.set(root + "rarity", rarity);
            nativeConfig.set(root + "supported_items", supported);
            nativeConfig.set(root + "slots", slots);
            nativeConfig.set(root + "weight", Math.max(1, Math.min(1024, entry.getInt("weight", rarityWeight))));
            nativeConfig.set(root + "max_level", Math.max(1, Math.min(255, entry.getInt("max_level", 3))));
            nativeConfig.set(root + "min_cost_base", entry.getInt("min_cost_base", 10));
            nativeConfig.set(root + "min_cost_per_level", entry.getInt("min_cost_per_level", 8));
            nativeConfig.set(root + "max_cost_base", entry.getInt("max_cost_base", 45));
            nativeConfig.set(root + "max_cost_per_level", entry.getInt("max_cost_per_level", 8));
            nativeConfig.set(root + "anvil_cost", entry.getInt("anvil_cost", 4));
            // Use explicit per-source acquisition to avoid bypassing the
            // configured rarity, NG or level-roll probabilities.
            nativeConfig.set(root + "table_obtainable", false);
            nativeConfig.set(root + "tradeable", false);
            nativeConfig.set(root + "treasure", entry.getBoolean("treasure", false));
            nativeConfig.set(root + "exclusive_set", entry.getStringList("conflicts"));
            // upgrade_of is a directed evolution, but BOTH sides must
            // exclude each other as normal enchantments in Paper's registry.
            // An anvil with the appropriate enchanted book handles the
            // conversion; ordinary enchanting never stacks them.
            String upgradeOf = entry.getString("upgrade_of", "");
            if (!upgradeOf.isBlank()) {
                org.bukkit.NamespacedKey parent = org.bukkit.NamespacedKey.fromString(upgradeOf);
                if (parent == null || parent.equals(new org.bukkit.NamespacedKey("enchantssquared", id)))
                    throw new IllegalArgumentException("Invalid upgrade_of for " + id + ": " + upgradeOf);
                nativeConfig.set(root + "upgrade_of", parent.toString());
            }
            // Passive attributes are also supported by the already existing
            // Bukkit + ValhallaMMO NativeAttributeEnchantEngine.
            ConfigurationSection attributes = entry.getConfigurationSection("attribute");
            if (attributes != null) {
                for (String key : attributes.getKeys(false)) {
                    nativeConfig.set(root + "attribute." + key, attributes.get(key));
                }
                nativeConfig.set(root + "scale_per_level",
                        entry.getDouble("scale_per_level", 0));
                nativeConfig.set(root + "attribute_operation",
                        entry.getString("attribute_operation", "ADD_NUMBER"));
            }
            ConfigurationSection acquisition = entry.getConfigurationSection("acquisition");
            if (acquisition != null) {
                for (String source : List.of("enchanting","villager_trade","fishing","structure_chest","mob_drop")) {
                    nativeConfig.set(root + "acquisition." + source,
                            clamp(acquisition.getDouble(source, 0)));
                }
            }
        }

        // A custom evolution group is mutually exclusive at registration
        // time (in addition to runtime repair of invalid old item stacks).
        // Compute the complete chain, not just immediate parents, so the
        // highest stage cannot be combined with any prior stage or sibling.
        java.util.Map<String, String> links = new java.util.LinkedHashMap<>();
        ConfigurationSection tiers = nativeConfig.getConfigurationSection("tier_families");
        if (tiers != null) for (String name : tiers.getKeys(false)) {
            ConfigurationSection fam = tiers.getConfigurationSection(name);
            if (fam == null) continue;
            String previous = fam.getString("base", "");
            for (java.util.Map<?, ?> row : fam.getMapList("tiers")) {
                Object raw = row.get("enchantment");
                if (raw == null) continue;
                String next = raw.toString();
                links.put(next, previous);
                previous = next;
            }
        }
        for (String id : entries.getKeys(false)) {
            ConfigurationSection entry = entries.getConfigurationSection(id);
            if (entry == null || !entry.getBoolean("enabled", true)) continue;
            String upgrade = entry.getString("upgrade_of", "");
            if (upgrade.isBlank()) continue;
            links.put("enchantssquared:" + id, upgrade);
        }
        java.util.Map<String, java.util.List<String>> groups = new java.util.LinkedHashMap<>();
        for (String id : links.keySet()) {
            java.util.Set<String> visited = new java.util.HashSet<>();
            String root = id;
            while (links.containsKey(root) && visited.add(root)) root = links.get(root);
            if (!visited.add(root)) throw new IllegalArgumentException("Cyclic upgrade_of chain at " + id);
            groups.computeIfAbsent(root, ignored -> new java.util.ArrayList<>()).add(id);
        }
        for (var group : groups.entrySet()) {
            java.util.List<String> members = new java.util.ArrayList<>(group.getValue());
            members.add(group.getKey());
            // Vanilla base enchantments cannot be edited in our registry.
            // All custom participants exclude every other member.
            for (String id : members) {
                if (!id.startsWith("enchantssquared:")) continue;
                String nativeId = id.substring("enchantssquared:".length());
                if (!nativeConfig.getBoolean("enchantments." + nativeId + ".enabled", false)) continue;
                String key = "enchantments." + nativeId + ".exclusive_set";
                java.util.Set<String> excluded = new java.util.LinkedHashSet<>(nativeConfig.getStringList(key));
                for (String other : members) if (!id.equals(other)) excluded.add(other);
                excluded.remove(id);
                nativeConfig.set(key, new java.util.ArrayList<>(excluded));
            }
        }
        return custom;
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
    }
}
