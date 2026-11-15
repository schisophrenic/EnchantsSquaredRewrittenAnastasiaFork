package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.enchantssquared.config.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** Admin command: /esnative generate (restart server after generation). */
public final class NativeEnchantsCommand implements CommandExecutor {
    private final EnchantsSquared plugin;

    public NativeEnchantsCommand(EnchantsSquared plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!sender.hasPermission("es.native.generate")) {
            sender.sendMessage("You do not have permission to generate native enchantments.");
            return true;
        }
        if (args.length != 1 || !args[0].equalsIgnoreCase("generate")) {
            sender.sendMessage("Usage: /" + label + " generate");
            return true;
        }
        ConfigManager.getInstance().reloadConfig("config_native_enchants.yml");
        new NativeYamlRegistryGenerator(plugin).generate();
        sender.sendMessage("Native enchantment generation attempted using config_native_enchants.yml."
                + " Check console for errors, then FULLY RESTART to load registry changes.");
        return true;
    }
}
