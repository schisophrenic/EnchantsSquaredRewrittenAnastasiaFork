package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.enchantssquared.config.ConfigManager;
import me.athlaeos.enchantssquared.domain.MaterialClassType;
import me.athlaeos.enchantssquared.enchantments.CustomEnchant;
import me.athlaeos.enchantssquared.managers.CustomEnchantManager;
import me.athlaeos.enchantssquared.utility.ChatUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Compiles YAML enchantment definitions into genuine Minecraft 26.3 datapack
 * registry entries. This runs at plugin enable, after worlds have loaded.
 *
 * A newly created or modified registry requires a SERVER RESTART to take effect.
 * The plugin cannot dynamically register data-driven enchantments during play.
 */
public final class NativeYamlRegistryGenerator {
    private static final Pattern SAFE_ID = Pattern.compile("[a-z0-9_./-]+");
    private static final String NAMESPACE = "enchantssquared";
    private final EnchantsSquared plugin;
    private final YamlConfiguration config;

    public NativeYamlRegistryGenerator(EnchantsSquared plugin) {
        this.plugin = plugin;
        this.config = ConfigManager.getInstance().getConfig("config_native_enchants.yml").get();
    }

    public YamlConfiguration config() {
        return config;
    }

    public void generate() {
        if (!config.getBoolean("settings.generate_datapack", true)) return;
        if (plugin.getServer().getWorlds().isEmpty()) {
            plugin.getLogger().warning("Cannot generate native enchants: no world loaded");
            return;
        }
        String packName = config.getString("settings.datapack_name", "EnchantsSquaredNativeAuto");
        if (!packName.matches("[A-Za-z0-9_-]+")) {
            plugin.getLogger().severe("Invalid datapack_name in config_native_enchants.yml");
            return;
        }

        Path folder = plugin.getServer().getWorlds().get(0).getWorldFolder().toPath()
                .resolve("datapacks").resolve(packName);
        Map<String, String> output = new LinkedHashMap<>();
        output.put("pack.mcmeta", "{\"pack\":{\"description\":\"EnchantsSquared 26.3 generated registry\",\"min_format\":[121,0],\"max_format\":[121,0]}}\n");

        LinkedHashSet<String> table = new LinkedHashSet<>();
        LinkedHashSet<String> trade = new LinkedHashSet<>();
        LinkedHashSet<String> nonTreasure = new LinkedHashSet<>();
        Set<String> customKeys = new HashSet<>();

        ConfigurationSection definitions = config.getConfigurationSection("enchantments");
        if (definitions != null) for (String id : definitions.getKeys(false)) {
            if (!safe(id)) {
                plugin.getLogger().warning("Ignored invalid native enchantment key: " + id);
                continue;
            }
            ConfigurationSection section = definitions.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) continue;
            String qualified = NAMESPACE + ":" + id;
            customKeys.add(id);
            List<String> supported = section.getStringList("supported_items");
            List<String> slots = section.getStringList("slots");
            List<String> conflicts = section.getStringList("exclusive_set");
            if (supported.isEmpty()) {
                plugin.getLogger().warning("Missing supported_items on native enchantment " + id);
                continue;
            }
            String json = enchantJson(
                    section.getString("display_name", id),
                    supported, slots.isEmpty() ? List.of("mainhand") : slots, conflicts,
                    section.getInt("weight", 10), section.getInt("max_level", 1),
                    section.getInt("min_cost_base", 5), section.getInt("min_cost_per_level", 10),
                    section.getInt("max_cost_base", 50), section.getInt("max_cost_per_level", 10),
                    section.getInt("anvil_cost", 4));
            output.put("data/" + NAMESPACE + "/enchantment/" + id + ".json", json);
            if (section.getBoolean("table_obtainable", false)) table.add(qualified);
            if (section.getBoolean("tradeable", false)) trade.add(qualified);
            if (!section.getBoolean("treasure", false)) nonTreasure.add(qualified);
        }

        if (config.getBoolean("settings.include_existing_enchantssquared", true)) {
            for (CustomEnchant enchant : CustomEnchantManager.getInstance().getAllEnchants().values()) {
                String id = enchant.getType().toLowerCase(Locale.ROOT);
                if (customKeys.contains(id) || !safe(id)) continue;
                List<String> supported = resolveExistingItems(enchant.getCompatibleItems());
                String name = ChatColor.stripColor(ChatUtils.chat(enchant.getDisplayEnchantment()));
                if (name == null || name.isBlank()) name = id;
                output.put("data/" + NAMESPACE + "/enchantment/" + id + ".json",
                        enchantJson(name, supported, List.of("any"), List.of(),
                                enchant.getWeight(), enchant.getMaxLevel(), 5, 10, 50, 10, 4));
                String qualified = NAMESPACE + ":" + id;
                if (!enchant.isTreasure() && !enchant.isBookOnly() && !enchant.getCompatibleItems().isEmpty()) {
                    table.add(qualified);
                }
                if (enchant.isTradingEnabled()) trade.add(qualified);
                if (!enchant.isTreasure()) nonTreasure.add(qualified);
            }
        }

        // replace=false retains ordinary vanilla enchantments, including Sharpness.
        // Tiers from the YAML sample have table_obtainable=false and can only be
        // chosen by the per-player enchanting replacement listener.
        output.put("data/minecraft/tags/enchantment/in_enchanting_table.json", tagJson(table));
        output.put("data/minecraft/tags/enchantment/tradeable.json", tagJson(trade));
        output.put("data/minecraft/tags/enchantment/non_treasure.json", tagJson(nonTreasure));
        try {
            Files.createDirectories(folder);
            boolean changed = false;
            Set<Path> expected = new HashSet<>();
            for (Map.Entry<String, String> entry : output.entrySet()) {
                Path destination = folder.resolve(entry.getKey()).normalize();
                if (!destination.startsWith(folder)) throw new IOException("Invalid generated path");
                expected.add(destination);
                Files.createDirectories(destination.getParent());
                if (!Files.exists(destination) || !Files.readString(destination).equals(entry.getValue())) {
                    Files.writeString(destination, entry.getValue(), StandardCharsets.UTF_8);
                    changed = true;
                }
            }
            Path generatedEnchants = folder.resolve("data/enchantssquared/enchantment");
            if (Files.isDirectory(generatedEnchants)) try (Stream<Path> stream = Files.list(generatedEnchants)) {
                for (Path path : stream.filter(p -> p.toString().endsWith(".json")).toList()) {
                    if (!expected.contains(path)) {
                        Files.delete(path);
                        changed = true;
                    }
                }
            }
            if (changed) plugin.getLogger().warning("Generated native enchantment datapack at " + folder
                    + ". FULL SERVER RESTART REQUIRED for new or modified registry entries.");
            else plugin.getLogger().info("Native enchantment datapack is up to date (" + output.size() + " files).");
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not generate native enchantments: " + ex.getMessage());
        }
    }

    private static List<String> resolveExistingItems(Collection<String> classes) {
        LinkedHashSet<String> supported = new LinkedHashSet<>();
        for (String name : classes) {
            if (name.equals("ALL")) {
                supported.add("#minecraft:enchantable/durability");
                continue;
            }
            if (name.equals("ELYTRAS")) name = "ELYTRA";
            try {
                for (Material material : MaterialClassType.valueOf(name).getMatches()) {
                    if (material.isItem()) supported.add("minecraft:" + material.getKey().getKey());
                }
            } catch (IllegalArgumentException ignored) {
                try {
                    Material material = Material.valueOf(name);
                    if (material.isItem()) supported.add("minecraft:" + material.getKey().getKey());
                } catch (IllegalArgumentException ignoredAgain) {
                    // Unsupported classes cannot safely be used for native registration.
                }
            }
        }
        if (supported.isEmpty()) supported.add("#minecraft:enchantable/durability");
        return new ArrayList<>(supported);
    }

    private static String enchantJson(String title, List<String> items, List<String> slots,
                                      List<String> conflicts, int weight, int maxLevel,
                                      int minBase, int minPer, int maxBase, int maxPer, int anvil) {
        // Minecraft owns the metadata, Java listeners own gameplay effects.
        return "{\n"
                + "  \"description\": {\"text\": " + quote(title) + "},\n"
                + "  \"supported_items\": " + jsonStrings(items) + ",\n"
                + (conflicts.isEmpty() ? "" : "  \"exclusive_set\": " + jsonStrings(conflicts) + ",\n")
                + "  \"weight\": " + Math.max(1, Math.min(1024, weight)) + ",\n"
                + "  \"max_level\": " + Math.max(1, Math.min(255, maxLevel)) + ",\n"
                + "  \"min_cost\": {\"base\": " + Math.max(1, minBase) + ", \"per_level_above_first\": " + Math.max(0, minPer) + "},\n"
                + "  \"max_cost\": {\"base\": " + Math.max(minBase, maxBase) + ", \"per_level_above_first\": " + Math.max(0, maxPer) + "},\n"
                + "  \"anvil_cost\": " + Math.max(0, anvil) + ",\n"
                + "  \"slots\": " + jsonStrings(slots) + ",\n"
                + "  \"effects\": {}\n}\n";
    }

    private static String tagJson(Collection<String> values) {
        return "{\"replace\":false,\"values\":" + jsonStrings(values) + "}\n";
    }

    private static String jsonStrings(Collection<String> values) {
        return "[" + String.join(",", values.stream().map(NativeYamlRegistryGenerator::quote).toList()) + "]";
    }

    private static String quote(String raw) {
        return "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

    private static boolean safe(String id) {
        return SAFE_ID.matcher(id).matches() && !id.contains("..") && !id.startsWith("/");
    }
}
