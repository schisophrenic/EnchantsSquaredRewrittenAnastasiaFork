package me.athlaeos.enchantssquared.nativeenchants;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standalone shared priority calculation. Depends only on Bukkit, NOT ValhallaMMO.
 * Values are multipliers for an enchantment's normal contribution, never changes item metadata.
 */
public final class EnchantmentPriority {
    public static final List<String> DEFENSE = List.of("protection", "magic_protection",
            "projectile_protection", "blast_protection", "fire_protection");
    public static final List<String> OFFENSE = List.of("sharpness", "smite", "bane_of_arthropods");
    private final boolean enabled;
    private final Map<Integer, double[]> defense;
    private final Map<Integer, double[]> offense;

    public EnchantmentPriority(ConfigurationSection root) {
        enabled = root == null || root.getBoolean("enabled", true);
        defense = read(root == null ? null : root.getConfigurationSection("defense"),
                new int[][] {{10}, {5,5}, {5,3,2}, {4,2,2,2}, {3,2,2,2,1}});
        offense = read(root == null ? null : root.getConfigurationSection("offense"),
                new int[][] {{10}, {5,5}, {5,3,2}});
    }

    private static Map<Integer, double[]> read(ConfigurationSection section, int[][] defaults) {
        Map<Integer, double[]> result = new HashMap<>();
        for (int i = 0; i < defaults.length; i++) {
            List<Integer> value = section == null ? List.of()
                    : section.getIntegerList(Integer.toString(i + 1));
            if (value.size() != i + 1 || value.stream().anyMatch(v -> v < 0)
                    || value.stream().mapToInt(Integer::intValue).sum() != 10) {
                value = Arrays.stream(defaults[i]).boxed().toList();
            }
            double[] weights = new double[value.size()];
            for (int j = 0; j < weights.length; j++) weights[j] = value.get(j) / 10.0;
            result.put(i + 1, weights);
        }
        return result;
    }

    public Map<String, Double> weights(ItemStack item, boolean armor) {
        List<String> ordered = armor ? DEFENSE : OFFENSE;
        List<String> present = new ArrayList<>();
        if (item == null || item.getType().isAir()) return Map.of();
        for (String family : ordered) {
            if (level(item, family) > 0) present.add(family);
        }
        if (present.isEmpty()) return Map.of();
        double[] split = (armor ? defense : offense).get(present.size());
        Map<String, Double> result = new HashMap<>();
        for (int i = 0; i < present.size(); i++) {
            result.put(present.get(i), enabled ? split[i] : 1.0);
        }
        return result;
    }

    public double multiplier(ItemStack item, String family, boolean armor) {
        return weights(item, armor).getOrDefault(family, 0.0);
    }

    public static int level(ItemStack item, String family) {
        if (item == null || item.getType().isAir()) return 0;
        int highest = 0;
        for (Map.Entry<Enchantment, Integer> enchant : item.getEnchantments().entrySet()) {
            String key = enchant.getKey().getKey().getKey();
            String namespace = enchant.getKey().getKey().getNamespace();
            if ((namespace.equals("minecraft") && key.equals(family))
                    || (namespace.equals("enchantssquared")
                    && (key.equals(family) || key.equals("lesser_" + family)
                    || key.equals("advanced_" + family) || key.equals("supreme_" + family)))) {
                highest = Math.max(highest, enchant.getValue());
            }
        }
        return highest;
    }
}
