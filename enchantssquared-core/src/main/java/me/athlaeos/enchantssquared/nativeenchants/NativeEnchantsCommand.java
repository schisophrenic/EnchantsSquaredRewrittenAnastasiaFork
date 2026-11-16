package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.enchantssquared.config.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;

/**
 * /esnative status  - Verify real enchantment registry entries after server boot.
 * /esnative list    - Report the missing/registered YAML enchantment keys.
 *
 * No datapack generation: entries are registered by Paper bootstrap.
 */
public final class NativeEnchantsCommand {
    private final EnchantsSquared plugin;

    public NativeEnchantsCommand(EnchantsSquared plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("es.native.generate")) {
            sender.sendMessage("You do not have permission to inspect native enchantments.");
            return;
        }
        if (args.length != 1 || (!args[0].equalsIgnoreCase("status")
                && !args[0].equalsIgnoreCase("list"))) {
            sender.sendMessage("Usage: /esnative <status|list>");
            return;
        }

        YamlConfiguration config = ConfigManager.getInstance().getConfig("config_native_enchants.yml").get();
        ConfigurationSection definitions = config.getConfigurationSection("enchantments");
        int configured = 0, registered = 0;
        if (definitions != null) {
            for (String id : definitions.getKeys(false)) {
                if (!definitions.getBoolean(id + ".enabled", true)) continue;
                configured++;
                Enchantment enchant = Registry.ENCHANTMENT.get(new NamespacedKey("enchantssquared", id));
                if (enchant != null) registered++;
                if (args[0].equalsIgnoreCase("list")) {
                    sender.sendMessage("enchantssquared:" + id + " - " +
                            (enchant != null ? "REGISTERED max=" + enchant.getMaxLevel() : "MISSING"));
                }
            }
        }
        sender.sendMessage("EnchantsSquared native registry: " + registered + "/" + configured +
                " YAML enchantments registered directly in Paper (no datapack).");
        if (configured != registered) {
            sender.sendMessage("For newly added YAML IDs, FULLY RESTART the server. Check bootstrap errors in console.");
        } else {
            sender.sendMessage("Try /enchant on a test item with enchantssquared:lesser_sharpness.");
        }
    }
}
