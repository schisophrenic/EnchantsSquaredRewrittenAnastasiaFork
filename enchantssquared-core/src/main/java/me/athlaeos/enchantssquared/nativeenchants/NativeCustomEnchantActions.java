package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reusable, data-driven enchantment action engine.
 *
 * Server owners can add real native Paper enchantments entirely in
 * config_custom_enchants.yml (no Java edits or plugin recompilation).
 * Supported triggers and action names are documented in that file.
 *
 * Existing fixed handlers for vanilla tiers, Summoner, Valhalla scaling,
 * enchant attributes etc. continue unchanged.
 */
public final class NativeCustomEnchantActions implements Listener {
    private record Rule(String event, ConfigurationSection section) {}
    private record Definition(String id, Enchantment enchantment, String kind, List<Rule> rules) {}

    private final List<Definition> definitions = new ArrayList<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final NativeCapturedMobEggs eggs;

    public NativeCustomEnchantActions(EnchantsSquared plugin, YamlConfiguration configuration) {
        eggs = new NativeCapturedMobEggs(plugin);
        ConfigurationSection enchants = configuration.getConfigurationSection("enchants");
        if (enchants == null) return;
        for (String id : enchants.getKeys(false)) {
            ConfigurationSection definition = enchants.getConfigurationSection(id);
            if (definition == null || !definition.getBoolean("enabled", true)) continue;
            Enchantment enchant = Registry.ENCHANTMENT.get(new NamespacedKey("enchantssquared", id));
            if (enchant == null) {
                plugin.getLogger().warning("Custom enchant " + id + " is not registered. "
                        + "Check its YAML and fully restart the server.");
                continue;
            }
            ConfigurationSection triggerConfig = definition.getConfigurationSection("triggers");
            if (triggerConfig == null) continue;
            List<Rule> rules = new ArrayList<>();
            for (String trigger : triggerConfig.getKeys(false)) {
                ConfigurationSection options = triggerConfig.getConfigurationSection(trigger);
                if (options == null || options.getMapList("actions").isEmpty()) continue;
                String event = trigger.toUpperCase(Locale.ROOT);
                if (!List.of("ON_HIT", "ON_DAMAGED", "ON_KILL", "ON_MINE",
                        "ON_SHOOT", "ON_FISH", "ON_INTERACT").contains(event)) {
                    plugin.getLogger().warning("Unknown custom enchant trigger "
                            + trigger + " on " + id);
                    continue;
                }
                rules.add(new Rule(event, options));
            }
            if (!rules.isEmpty()) definitions.add(new Definition(id, enchant,
                    definition.getString("type", "weapon").toLowerCase(Locale.ROOT),
                    List.copyOf(rules)));
        }
        plugin.getLogger().info("Loaded " + definitions.size()
                + " YAML-driven native enchantments with action triggers.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity resolved = resolve(damager);
        if (!(resolved instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity target)) return;
        boolean projectile = damager instanceof Projectile;
        for (Definition definition : definitions) {
            for (Rule rule : definition.rules()) {
                if (!rule.event().equals("ON_HIT")) continue;
                if (projectile && !rule.section().getBoolean("allow_projectiles", false)) continue;
                tryEffect(definition, rule, player, target, event,
                        player.getInventory().getItemInMainHand());
                if (event.isCancelled()) return; // A mob was captured into an egg.
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Entity damager = resolve(event.getDamager());
        LivingEntity source = damager instanceof LivingEntity living ? living : null;
        for (Definition definition : definitions)
            for (Rule rule : definition.rules())
                if (rule.event().equals("ON_DAMAGED"))
                    tryEffect(definition, rule, player, source, event, null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damage))
            return;
        if (!(resolve(damage.getDamager()) instanceof Player player)) return;
        for (Definition definition : definitions)
            for (Rule rule : definition.rules())
                if (rule.event().equals("ON_KILL"))
                    tryEffect(definition, rule, player, event.getEntity(), null,
                            player.getInventory().getItemInMainHand());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) {
        Player player = event.getPlayer();
        for (Definition definition : definitions)
            for (Rule rule : definition.rules())
                if (rule.event().equals("ON_MINE"))
                    tryEffect(definition, rule, player, null, null,
                            player.getInventory().getItemInMainHand());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        for (Definition definition : definitions)
            for (Rule rule : definition.rules())
                if (rule.event().equals("ON_SHOOT"))
                    tryEffect(definition, rule, player, null, null, event.getBow());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        for (Definition definition : definitions)
            for (Rule rule : definition.rules())
                if (rule.event().equals("ON_FISH"))
                    tryEffect(definition, rule, player, null, null,
                            player.getInventory().getItemInMainHand());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || !event.getAction().name().startsWith("RIGHT_CLICK")) return;
        for (Definition definition : definitions)
            for (Rule rule : definition.rules())
                if (rule.event().equals("ON_INTERACT"))
                    tryEffect(definition, rule, event.getPlayer(), null, null,
                            event.getItem());
    }

    private void tryEffect(Definition definition, Rule rule, Player player,
                           LivingEntity target, EntityDamageByEntityEvent damage,
                           ItemStack held) {
        int level = enchantLevel(definition, player, held);
        if (level <= 0) return;
        ConfigurationSection options = rule.section();
        if (options.getBoolean("require_target", false) && target == null) return;
        List<String> includeTypes = options.getStringList("target_types");
        if (!includeTypes.isEmpty() && (target == null || includeTypes.stream()
                .noneMatch(s -> s.equalsIgnoreCase(target.getType().name())))) return;
        List<String> blocked = options.getStringList("blocked_targets");
        if (target != null && blocked.stream()
                .anyMatch(s -> s.equalsIgnoreCase(target.getType().name()))) return;

        String key = player.getUniqueId() + ":" + definition.id() + ":" + rule.event();
        long now = System.currentTimeMillis();
        if (cooldowns.getOrDefault(key, 0L) > now) return;
        double chance = Math.min(1, Math.max(0, options.getDouble("chance", 1)
                + (level - 1L) * options.getDouble("chance_per_level", 0)));
        if (!Double.isFinite(chance) || ThreadLocalRandom.current().nextDouble() >= chance)
            return;
        boolean executed = false;
        for (Map<?, ?> raw : options.getMapList("actions")) {
            YamlConfiguration action = new YamlConfiguration();
            for (var item : raw.entrySet()) {
                if (item.getKey() != null)
                    action.set(String.valueOf(item.getKey()), item.getValue());
            }
            double perAction = action.getDouble("chance", 1);
            if (!Double.isFinite(perAction) || ThreadLocalRandom.current().nextDouble()
                    >= Math.min(1, Math.max(0, perAction))) continue;
            executed |= performAction(definition, level, player, target, damage, action);
            if (damage != null && damage.isCancelled()) break;
        }
        if (executed) {
            double seconds = options.getDouble("cooldown_seconds", 0);
            if (Double.isFinite(seconds) && seconds > 0) {
                long duration = Math.round(Math.min(3600, seconds) * 1000);
                cooldowns.put(key, now + duration);
            }
        }
        // Avoid retaining departed players indefinitely on large servers.
        if (cooldowns.size() > 10000)
            cooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private boolean performAction(Definition definition, int level, Player owner,
                                   LivingEntity target, EntityDamageByEntityEvent damage,
                                   YamlConfiguration action) {
        String type = action.getString("type", "").toUpperCase(Locale.ROOT);
        LivingEntity destination = "SELF".equalsIgnoreCase(action.getString("target", "ENEMY"))
                ? owner : target;
        double amount = scale(action, "amount", "per_level", level, 0);
        switch (type) {
            case "CAPTURE_MOB":
                return damage != null && target instanceof org.bukkit.entity.Mob
                        && eggs.capture(owner, damage, action);
            case "HEAL":
                if (destination == null || amount <= 0) return false;
                destination.setHealth(Math.min(destination.getMaxHealth(),
                        destination.getHealth() + Math.min(1000, amount)));
                return true;
            case "DAMAGE":
                if (damage == null || damage.isCancelled() || amount <= 0
                        || destination != damage.getEntity()) return false;
                damage.setDamage(Math.max(0, damage.getDamage() + Math.min(1000, amount)));
                return true;
            case "POTION":
                if (destination == null) return false;
                PotionEffectType potion = PotionEffectType.getByName(
                        action.getString("potion", "").toUpperCase(Locale.ROOT));
                if (potion == null) return false;
                int ticks = (int) Math.min(72000, Math.max(1,
                        scale(action, "duration_ticks", "duration_per_level", level, 100)));
                int amplifier = (int) Math.min(20, Math.max(0,
                        scale(action, "amplifier", "amplifier_per_level", level, 0)));
                destination.addPotionEffect(new PotionEffect(potion, ticks, amplifier));
                return true;
            case "IGNITE":
                if (destination == null) return false;
                destination.setFireTicks(Math.min(12000, Math.max(1, (int)
                        scale(action, "ticks", "ticks_per_level", level, 60))));
                return true;
            case "KNOCKBACK":
                if (destination == null) return false;
                Vector push = destination.getLocation().toVector()
                        .subtract(owner.getLocation().toVector());
                if (push.lengthSquared() < 0.01) return false;
                double strength = Math.max(0, Math.min(2,
                        scale(action, "strength", "per_level", level, 0.3)));
                destination.setVelocity(destination.getVelocity()
                        .add(push.normalize().multiply(strength)));
                return true;
            case "EXPERIENCE":
                if (amount <= 0) return false;
                owner.giveExp((int) Math.min(10000, amount));
                return true;
            case "MESSAGE":
                String message = action.getString("message", "");
                if (message.isEmpty()) return false;
                owner.sendMessage(ChatColor.translateAlternateColorCodes('&', message
                        .replace("{level}", Integer.toString(level))
                        .replace("{enchant}", definition.id())
                        .replace("{target}", target == null ? "" : target.getType().name())));
                return true;
            default:
                return false;
        }
    }

    private static double scale(ConfigurationSection config, String baseKey,
                                 String levelKey, int level, double fallback) {
        double base = config.getDouble(baseKey, fallback);
        double increment = config.getDouble(levelKey, 0);
        double answer = base + (level - 1L) * increment;
        return Double.isFinite(answer) ? answer : 0;
    }

    private static Entity resolve(Entity source) {
        if (source instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity entity) return entity;
        }
        return source;
    }

    private static int enchantLevel(Definition def, Player player, ItemStack held) {
        if (def.kind().equals("armor") || def.kind().equals("boots")) {
            if (player.getEquipment() == null) return 0;
            long level = 0;
            for (ItemStack armor : player.getEquipment().getArmorContents()) {
                if (armor != null) level += Math.max(0, armor.getEnchantmentLevel(def.enchantment()));
            }
            return (int) Math.min(Integer.MAX_VALUE, level);
        }
        return held == null ? 0 : Math.max(0, held.getEnchantmentLevel(def.enchantment()));
    }
}
