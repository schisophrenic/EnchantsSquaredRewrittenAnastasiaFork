package me.athlaeos.enchantssquared.nativeenchants;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
        // Match the Java Edition melee enchantment effects exactly, rather
        // than treating all tiered enchantments as a generic damage bonus.
        registerHandler("vanilla_sharpness", (event, level, def) ->
                event.setDamage(event.getDamage() + (0.5 * level + 0.5)));
        registerHandler("vanilla_smite", (event, level, def) -> {
            if (Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE.isTagged(event.getEntityType())) {
                event.setDamage(event.getDamage() + 2.5 * level);
            }
        });
        registerHandler("vanilla_bane_of_arthropods", (event, level, def) -> {
            if (!Tag.ENTITY_TYPES_SENSITIVE_TO_BANE_OF_ARTHROPODS.isTagged(event.getEntityType())) return;
            event.setDamage(event.getDamage() + 2.5 * level);
            if (event.getEntity() instanceof LivingEntity victim) {
                // Vanilla Bane: Slowness IV, random 1..(1 + 0.5*level) seconds.
                int duration = ThreadLocalRandom.current().nextInt(20, 21 + 10 * level);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 3));
            }
        });
        registerHandler("on_hit_potion", (event, level, def) -> {
            if (!(event.getEntity() instanceof LivingEntity victim)) return;
            double chance = Math.max(0, Math.min(1, def.getDouble("chance", 0)));
            if (ThreadLocalRandom.current().nextDouble() >= chance) return;
            for (Map<?, ?> entry : def.getMapList("effects")) {
                Object type = entry.get("type");
                if (type == null) continue;
                PotionEffectType potion = PotionEffectType.getByName(type.toString());
                if (potion == null) continue;
                int duration = number(entry.get("duration_ticks"), 60);
                int amplifier = number(entry.get("amplifier"), 0);
                if (duration > 0) victim.addPotionEffect(new PotionEffect(potion, duration, Math.max(0, amplifier)));
            }
        });

        ConfigurationSection enchantments = configuration.getConfigurationSection("enchantments");
        if (enchantments == null) return;
        for (String id : enchantments.getKeys(false)) {
            ConfigurationSection def = enchantments.getConfigurationSection(id);
            if (def == null || !def.getBoolean("enabled", true)) continue;
            ConfigurationSection effect = def.getConfigurationSection("effect");
            if (effect == null) continue;
            String handler = effect.getString("handler", "none");
            if (handler.equals("none")) continue;
            Enchantment enchantment = Registry.ENCHANTMENT.get(new NamespacedKey("enchantssquared", id));
            // The registry entry must have been registered during Paper bootstrap.
            if (enchantment != null) registered.put(enchantment, new BoundEffect(handler, effect));
        }
    }

    public void registerHandler(String name, NativeEffectHandler handler) {
        handlers.put(name, handler);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMeleeAttack(EntityDamageByEntityEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK
                && event.getCause() != DamageCause.ENTITY_SWEEP_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon.getType().isAir()) return;

        for (Map.Entry<Enchantment, BoundEffect> entry : registered.entrySet()) {
            int level = weapon.getEnchantmentLevel(entry.getKey());
            if (level <= 0) continue;
            BoundEffect effect = entry.getValue();
            NativeEffectHandler handler = handlers.get(effect.handler());
            if (handler != null) handler.apply(event, level, effect.definition());
        }
    }

    private static int number(Object number, int fallback) {
        if (number instanceof Number n) return n.intValue();
        return fallback;
    }

    private static double value(ConfigurationSection definition, int level) {
        return definition.getDouble("base", 0.0)
                + Math.max(0, level - 1) * definition.getDouble("per_level", 0.0);
    }
}
