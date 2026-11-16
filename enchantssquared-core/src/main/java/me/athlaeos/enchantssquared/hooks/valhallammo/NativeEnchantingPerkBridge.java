package me.athlaeos.enchantssquared.hooks.valhallammo;

import me.athlaeos.enchantssquared.config.ConfigManager;
import me.athlaeos.valhallammo.item.EnchantmentClassification;
import me.athlaeos.valhallammo.playerstats.profiles.ProfileCache;
import me.athlaeos.valhallammo.playerstats.profiles.implementations.EnchantingProfile;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import java.util.Map;

/** Translates existing ValhallaMMO enchant level perks to namespaced enchantments. */
public final class NativeEnchantingPerkBridge implements Listener {
    private final YamlConfiguration config = ConfigManager.getInstance().getConfig("config_native_enchanting.yml").get();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (!config.getBoolean("enabled", true)) return;
        EnchantingProfile profile = ProfileCache.getOrCache(event.getEnchanter(), EnchantingProfile.class);
        for (Map.Entry<Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet()){
            NamespacedKey key = entry.getKey().getKey();
            if (!key.getNamespace().equals("enchantssquared")) continue;
            // The tier substitutions inherit the complete Valhalla-amplified
            // vanilla roll and its specific/generic perk bonuses already.
            // Adding those perks a second time here would double-amplify tiers.
            if (key.getKey().matches("(lesser|advanced|supreme)_(sharpness|smite|bane_of_arthropods|power|punch|flame|fire_aspect|protection|fire_protection|blast_protection|projectile_protection|magic_protection)")) {
                continue;
            }
            int bonus = 0;
            String source = key.getKey().equals("magic_protection")
                    ? "protection" : config.getString("replacements." + key.getKey());
            if (source != null && !source.isBlank()){
                Enchantment vanilla = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(source.toLowerCase()));
                if (vanilla != null) bonus += profile.getEnchantmentBonus(vanilla);
            }
            String group = key.getKey().equals("magic_protection")
                    ? "DEFENSIVE" : config.getString("categories." + key.getKey(), "UNDEFINED").toUpperCase();
            try {
                bonus += profile.getEnchantmentBonus(EnchantmentClassification.valueOf(group));
            } catch (IllegalArgumentException ignored) {
                // Unknown category: no generic bonus.
            }
            if (bonus != 0) entry.setValue(Math.max(1, Math.min(entry.getKey().getMaxLevel(), entry.getValue() + bonus)));
        }
    }
}
