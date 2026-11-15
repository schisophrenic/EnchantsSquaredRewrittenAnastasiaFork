package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.config.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

/**
 * Cosmetic rename only: original numeric IDs, PDC keys, Java mechanics,
 * and persisted enchantments remain fully compatible with old items.
 */
public final class NativeEnchantAliases {
    private NativeEnchantAliases() { }

    public static void apply() {
        YamlConfiguration nativeConfig = ConfigManager.getInstance()
                .getConfig("config_native_enchants.yml").get();
        YamlConfiguration original = ConfigManager.getInstance().getConfig("config.yml").get();
        Map<String, String> legacyConfigKeys = Map.of(
                "vampiric", "vampiric",
                "sapping", "sapping",
                "vitality", "vitality",
                "sunforged", "sunforged",
                "lava_walker", "lava_walker",
                "pulling", "pulling",
                "piercer", "piercer",
                "shielding", "shielding");
        for (var entry : legacyConfigKeys.entrySet()) {
            String newName = nativeConfig.getString("display_aliases." + entry.getKey());
            if (newName == null || newName.isBlank()) continue;
            String path = "enchantment_configuration." + entry.getValue() + ".enchant_name";
            String before = original.getString(path, "");
            String prefix = before.startsWith("&") && before.length() >= 2
                    ? before.substring(0, 2) : "&7";
            original.set(path, prefix + newName + " %lv_roman%");
        }
    }
}
