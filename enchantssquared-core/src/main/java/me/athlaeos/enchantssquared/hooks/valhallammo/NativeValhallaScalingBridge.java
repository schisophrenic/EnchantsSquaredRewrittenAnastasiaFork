package me.athlaeos.enchantssquared.hooks.valhallammo;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.enchantssquared.config.ConfigManager;
import me.athlaeos.valhallammo.dom.Scaling;
import me.athlaeos.valhallammo.item.EnchantingItemPropertyManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;

/**
 * Connects Paper's real custom enchantments to ValhallaMMO's public
 * EnchantingItemPropertyManager scaling registry. No ValhallaMMO fork needed.
 *
 * Native tiered enchants are registered with the same ValhallaMMO
 * scaling expressions as their vanilla families, including the ability
 * to exceed the registry maxLevel V when enchanting.
 *
 * The plugin's tier listener converts the already-scaled vanilla roll
 * without applying amplification a second time. Custom formula overrides
 * are supported in the native bridge YAML and ValhallaMMO enchanting YAML.
 */
public final class NativeValhallaScalingBridge {
    private NativeValhallaScalingBridge() { }

    public static int register() {
        Plugin valhalla = EnchantsSquared.getPlugin().getServer().getPluginManager().getPlugin("ValhallaMMO");
        if (valhalla == null || !valhalla.isEnabled()) {
            EnchantsSquared.logWarning("Cannot register native enchanting scalings: ValhallaMMO is not enabled");
            return 0;
        }

        YamlConfiguration local = ConfigManager.getInstance()
                .getConfig("config_native_enchanting.yml").get();
        if (!local.getBoolean("native_scalings_enabled", true)) return 0;

        File enchantingFile = new File(valhalla.getDataFolder(), "skills/enchanting.yml");
        YamlConfiguration valhallaConfig = enchantingFile.exists()
                ? YamlConfiguration.loadConfiguration(enchantingFile) : new YamlConfiguration();

        ConfigurationSection scalingDefinitions = local.getConfigurationSection("native_scalings");
        // The user's previously installed config_native_enchanting.yml does not
        // yet contain native_scalings. Discover the new registry tiers directly
        // from config_native_enchants.yml, so updating the JAR is sufficient.
        Set<String> scalingIds = new LinkedHashSet<>();
        if (scalingDefinitions != null) scalingIds.addAll(scalingDefinitions.getKeys(false));
        YamlConfiguration nativeDefinitions = ConfigManager.getInstance()
                .getConfig("config_native_enchants.yml").get();
        ConfigurationSection families = nativeDefinitions.getConfigurationSection("tier_families");
        if (families != null) for (String family : families.getKeys(false)) {
            for (Map<?, ?> tier : nativeDefinitions.getMapList("tier_families." + family + ".tiers")) {
                Object key = tier.get("enchantment");
                if (key instanceof String id && id.startsWith("enchantssquared:")) {
                    scalingIds.add(id.substring("enchantssquared:".length()));
                }
            }
        }

        int count = 0;
        Set<String> warned = new HashSet<>();
        for (String id : scalingIds) {
            NamespacedKey key = NamespacedKey.fromString("enchantssquared:" + id);
            if (key == null) continue;
            Enchantment enchantment = Registry.ENCHANTMENT.get(key);
            if (enchantment == null) {
                if (warned.add(id)) EnchantsSquared.logWarning("Native scaling skipped (unregistered enchantment): " + key);
                continue;
            }
            String pluginFormula = scalingDefinitions == null ? null : scalingDefinitions.getString(id);
            String externalFormula = valhallaConfig.getString("scalings.enchantssquared:" + id);
            if (externalFormula == null) {
                externalFormula = valhallaConfig.getString("scalings." + id);
            }

            // Respect the same formula as the corresponding vanilla enchant:
            // e.g. (2/100) * %rating% - 2,ADD_ON_DEFAULT,1,45.
            // The upper bound limits the bonus, NOT the total level.
            String vanillaId = id.endsWith("_bane_of_arthropods") ? "bane_of_arthropods"
                    : id.endsWith("_smite") ? "smite"
                    : id.endsWith("_sharpness") ? "sharpness" : null;
            String vanillaFormula = vanillaId == null ? null
                    : valhallaConfig.getString("scalings." + vanillaId);
            if (vanillaFormula == null || vanillaFormula.isBlank()) {
                vanillaFormula = "(2/100) * %rating% - 2,ADD_ON_DEFAULT,1,45";
            }
            String formula = externalFormula != null && !externalFormula.isBlank()
                    ? externalFormula : pluginFormula;
            // Auto-migrate the previous test's wrong level-V scaling formula
            // without requiring the user to delete their installed YAML.
            if (formula == null || formula.isBlank()
                    || formula.replace(" ", "").startsWith("(1/100)*%rating%,ADD_ON_DEFAULT,0,5")) {
                formula = vanillaFormula;
            }
            try {
                EnchantingItemPropertyManager.registerScaling(enchantment, Scaling.fromString(formula, null));
                count++;
            } catch (RuntimeException ex) {
                EnchantsSquared.logWarning("Invalid native ValhallaMMO scaling " + key + ": " + ex.getMessage());
            }
        }

        EnchantsSquared.logInfo("Registered " + count +
                " native enchantment scaling formulas with ValhallaMMO."
                + " Tier formulas inherit vanilla scaling and can amplify past V.");
        return count;
    }
}
