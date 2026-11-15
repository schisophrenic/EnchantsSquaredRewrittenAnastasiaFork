package me.athlaeos.enchantssquared.hooks.valhallammo;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.valhallammo.item.ItemBuilder;
import me.athlaeos.valhallammo.item.ItemAttributesRegistry;
import me.athlaeos.valhallammo.item.SmithingItemPropertyManager;
import me.athlaeos.valhallammo.item.item_attributes.AttributeWrapper;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Low-quality naturally looted equipment. No Smithing perk or player object
 * is required: the real ValhallaMMO smithing tags and attributes are used.
 *
 * ValhallaEquipment's crafted class/model rolls are NOT automatically
 * invoked; this is independent natural loot with actual low quality and
 * optional rolled Valhalla stats, compatible with either plugin installed.
 */
public final class NativeLowQualityEquipmentLoot {
    private NativeLowQualityEquipmentLoot() {}

    public static ItemStack create(YamlConfiguration cfg) {
        ConfigurationSection loot = cfg.getConfigurationSection("loot_equipment");
        if (loot == null || !loot.getBoolean("enabled", false)) return null;
        List<Material> candidates = new ArrayList<>();
        for (String name : loot.getStringList("item_pool")) {
            Material material = Material.matchMaterial(name);
            if (material != null && material.isItem() && material.getMaxStackSize() == 1) {
                candidates.add(material);
            }
        }
        if (candidates.isEmpty()) return null;
        ItemStack item = new ItemStack(candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())));
        if (!EnchantsSquared.isValhallaHooked()) return item;
        try {
            ItemBuilder builder = new ItemBuilder(item);
            int neutral = neutralQuality(item.getType());
            double min = Math.max(0.05, Math.min(1, loot.getDouble("quality_fraction_min", 0.55)));
            double max = Math.max(min, Math.min(1, loot.getDouble("quality_fraction_max", 0.80)));
            int actual = (int) Math.round(neutral *
                    ThreadLocalRandom.current().nextDouble(min, max + 0.00001));
            SmithingItemPropertyManager.setNeutralQuality(builder, neutral);
            SmithingItemPropertyManager.setQuality(builder, actual);

            List<String> attributes = loot.getStringList("attribute_pool");
            int count = Math.max(0, Math.min(4, loot.getInt("max_random_attributes", 2)));
            double minRoll = loot.getDouble("attribute_min", 0.01);
            double maxRoll = Math.max(minRoll, loot.getDouble("attribute_max", 0.05));
            List<String> available = new ArrayList<>(attributes);
            for (int i = 0; i < count && !available.isEmpty(); i++) {
                String id = available.remove(ThreadLocalRandom.current().nextInt(available.size()))
                        .toUpperCase(Locale.ROOT);
                AttributeWrapper copy = ItemAttributesRegistry.getCopy(id);
                if (copy == null) continue;
                copy.setValue(ThreadLocalRandom.current().nextDouble(minRoll, maxRoll + 0.00001));
                ItemAttributesRegistry.addDefaultStat(builder, copy);
            }
            return builder.get();
        } catch (RuntimeException err) {
            EnchantsSquared.logWarning("Could not build low-quality Valhalla loot: " + err.getMessage());
            return item;
        }
    }

    private static int neutralQuality(Material material) {
        String id = material.name();
        if (id.contains("NETHERITE")) return 190;
        if (id.contains("DIAMOND")) return 160;
        if (id.contains("COPPER")) return 100;
        if (id.contains("IRON") || id.contains("GOLD")) return 130;
        if (id.contains("STONE")) return 80;
        if (id.contains("WOOD") || id.contains("LEATHER")) return 70;
        if (id.contains("CHAINMAIL")) return 110;
        return 100;
    }
}
