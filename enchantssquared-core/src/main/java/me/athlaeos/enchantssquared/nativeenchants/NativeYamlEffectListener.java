package me.athlaeos.enchantssquared.nativeenchants;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Java-side behavior for enchantments declared in YAML.
 *
 * For special mod-style effects, implement a new NativeEffectHandler in Java
 * and register it here. The YAML remains responsible for native registration,
 * compatibility, levels, cost and triggering prerequisites.
 */
public final class NativeYamlEffectListener implements Listener {
    @FunctionalInterface
    public interface NativeEffectHandler {
        void apply(EntityDamageByEntityEvent event, int level, ConfigurationSection definition);
    }

    private record BoundEffect(String handler, ConfigurationSection definition) {}

    private final Map<Enchantment, BoundEffect> registered = new LinkedHashMap<>();
    private final Map<String, NativeEffectHandler> handlers = new LinkedHashMap<>();

    public NativeYamlEffectListener(YamlConfiguration configuration) {
        registerHandler("melee_flat_bonus", (event, level, def) -> {
            double bonus = value(def, level);
            event.setDamage(Math.max(0, event.getDamage() + bonus));
        });
        registerHandler("melee_multiplier", (event, level, def) -> {
            double bonus = value(def, level);
            event.setDamage(Math.max(0, event.getDamage() * (1 + bonus)));
        });

        ConfigurationSection enchantments = configuration.getConfigurationSection("enchantments");
        if (enchantments == null) return;
        for (String id : enchantments.getKeys(false)) {
            ConfigurationSection def = enchantments.getConfigurationSection(id);
            if (def == null || !def.getBoolean("enabled", true)) continue;
            ConfigurationSection effect = def.getConfigurationSection("effect");
            if (effect == null) continue;
            String handler = effect.getString("handler", "none");
            if (!handlers.containsKey(handler)) continue;
            Enchantment enchantment = Registry.ENCHANTMENT.get(new NamespacedKey("enchantssquared", id));
            // A missing enchantment means the datapack has not been loaded yet.
            if (enchantment != null) registered.put(enchantment, new BoundEffect(handler, effect));
        }
    }

    public void registerHandler(String name, NativeEffectHandler handler) {
        handlers.put(name, handler);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMeleeAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon.getType().isAir()) return;

        for (Map.Entry<Enchantment, BoundEffect> entry : registered.entrySet()) {
            int level = weapon.getEnchantmentLevel(entry.getKey());
            if (level <= 0) continue;
            BoundEffect effect = entry.getValue();
            handlers.get(effect.handler()).apply(event, level, effect.definition());
        }
    }

    private static double value(ConfigurationSection definition, int level) {
        return definition.getDouble("base", 0.0)
                + Math.max(0, level - 1) * definition.getDouble("per_level", 0.0);
    }
}
