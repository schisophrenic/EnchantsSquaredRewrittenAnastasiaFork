package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.enchantssquared.config.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

/** Admin command: /esnative generate (restart server after generation). */
public final class NativeEnchantsCommand implements CommandExecutor {
    private final EnchantsSquared plugin;

    public NativeEnchantsCommand(EnchantsSquared plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("es.native.generate")) {
            sender.sendMessage("You do not have permission to generate native enchantments.");
            return true;
        }
        if (args.length != 1 || !args[0].equalsIgnoreCase("generate")) {
            sender.sendMessage("Usage: /" + label + " generate");
            return true;
        }
        ConfigManager.getInstance().reloadConfig("config_native_enchants.yml");
        YamlConfiguration reloaded = ConfigManager.getInstance().getConfig("config_native_enchants.yml").get();
        new NativeYamlRegistryGenerator(plugin).generate();
        sender.sendMessage("Native enchantment generation attempted using config_native_enchants.yml."
                + " Check console for errors, then FULLY RESTART to load registry changes.");
        return true;
    }
}
