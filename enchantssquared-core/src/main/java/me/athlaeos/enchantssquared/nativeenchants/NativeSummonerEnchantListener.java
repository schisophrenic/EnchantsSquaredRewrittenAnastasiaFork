package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.enchantssquared.hooks.valhallammo.NativeSummonValhallaBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Native, armor-only Summoner enchant.
 *
 * After the owner is struck, summons protective allied mobs. They inherit
 * the owner's effective Bukkit attributes and, when installed, a snapshot
 * of ValhallaMMO's resolved combat stats (via NativeSummonValhallaBridge).
 *
 * Owners cannot hit their own summons, and the summons cannot hurt their
 * owner or one another. The snapshot intentionally does not duplicate the
 * owner's equipment/items, so summons cannot create item dupes on death.
 */
public final class NativeSummonerEnchantListener implements Listener, Runnable {
    private record Summon(UUID owner, long expiresAt) {}

    private final EnchantsSquared plugin;
    private final Enchantment summoner;
    private final NamespacedKey ownerKey;
    private final NativeSummonValhallaBridge valhalla;
    private final Map<UUID, Summon> active = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final List<EntityType> variants = new ArrayList<>();

    private final double baseChance;
    private final double additionalChance;
    private final int cooldownSeconds;
    private final int perTriggerBase;
    private final int perLevelExtra;
    private final int maxPerTrigger;
    private final int maxPerOwner;
    private final int baseDurationSeconds;
    private final int extraDurationSeconds;
    private final int maxDurationSeconds;
    private final double followDistance;
    private final double dismissDistance;
    private final boolean copyBukkitAttributes;
    private final boolean helpOwnerAttack;

    public NativeSummonerEnchantListener(EnchantsSquared plugin, YamlConfiguration config) {
        this.plugin = plugin;
        this.summoner = Registry.ENCHANTMENT.get(new NamespacedKey("enchantssquared", "summoner"));
        this.ownerKey = new NamespacedKey(plugin, "summoned_mob_owner");
        ConfigurationSection section = config.getConfigurationSection("enchantments.summoner.summon");
        baseChance = percent(section, "chance_at_level_1", 0.22);
        additionalChance = percent(section, "chance_per_level", 0.06);
        cooldownSeconds = integer(section, "cooldown_seconds", 25, 0, 3600);
        perTriggerBase = integer(section, "initial_count", 1, 1, 16);
        perLevelExtra = integer(section, "additional_count_per_level", 1, 0, 16);
        maxPerTrigger = integer(section, "max_per_trigger", 3, 1, 16);
        maxPerOwner = integer(section, "max_active_per_owner", 6, 1, 24);
        baseDurationSeconds = integer(section, "duration_seconds", 30, 1, 1800);
        extraDurationSeconds = integer(section, "duration_per_level", 5, 0, 300);
        maxDurationSeconds = integer(section, "max_duration_seconds", 90, 1, 1800);
        followDistance = number(section, "follow_distance", 14.0, 2, 128);
        dismissDistance = Math.max(followDistance + 1.0,
                number(section, "dismiss_distance", 42.0, 3, 256));
        copyBukkitAttributes = section == null || section.getBoolean("copy_bukkit_attributes", true);
        helpOwnerAttack = section == null || section.getBoolean("help_owner_attack", true);
        String statMode = section == null ? "combat"
                : section.getString("copy_valhalla_stats", "combat");
        double statMultiplier = number(section, "valhalla_stat_multiplier", 1, 0, 4);
        valhalla = new NativeSummonValhallaBridge(statMode, statMultiplier);

        List<String> requested = section == null ? List.of("WOLF", "IRON_GOLEM")
                : section.getStringList("mob_types");
        for (String raw : requested) {
            try {
                EntityType type = EntityType.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
                Class<? extends Entity> klass = type.getEntityClass();
                if (klass != null && Mob.class.isAssignableFrom(klass)) variants.add(type);
                else EnchantsSquared.logWarning("Summoner mob type cannot fight: " + raw);
            } catch (IllegalArgumentException ignored) {
                EnchantsSquared.logWarning("Invalid Summoner mob type: " + raw);
            }
        }
        if (variants.isEmpty()) variants.add(EntityType.WOLF);
    }

    public boolean enabled() {
        return summoner != null;
    }

    private static int integer(ConfigurationSection s, String path, int fallback, int min, int max) {
        return s == null ? fallback : Math.max(min, Math.min(max, s.getInt(path, fallback)));
    }

    private static double number(ConfigurationSection s, String path, double fallback, double min, double max) {
        if (s == null) return fallback;
        double val = s.getDouble(path, fallback);
        return Double.isFinite(val) ? Math.max(min, Math.min(max, val)) : fallback;
    }

    private static double percent(ConfigurationSection s, String path, double fallback) {
        return number(s, path, fallback, 0, 1);
    }

    /** An owner and its summons cannot damage one another (melee or arrow). */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void preventFriendlyFire(EntityDamageByEntityEvent event) {
        Entity source = attacker(event.getDamager());
        Entity victim = event.getEntity();
        UUID attackerOwner = summonOwner(source);
        UUID victimOwner = summonOwner(victim);
        if (source instanceof Player player && player.getUniqueId().equals(victimOwner)) {
            event.setCancelled(true);
        } else if (victim instanceof Player player && player.getUniqueId().equals(attackerOwner)) {
            event.setCancelled(true);
        } else if (attackerOwner != null && attackerOwner.equals(victimOwner)) {
            event.setCancelled(true);
        }
    }

    /** Summons only for a real, non-cancelled player hit from a living enemy. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerHit(EntityDamageByEntityEvent event) {
        if (summoner == null || !(event.getEntity() instanceof Player player)
                || event.getFinalDamage() <= 0 || player.isDead()) return;
        Entity source = attacker(event.getDamager());
        if (!(source instanceof LivingEntity target) || source.equals(player)) return;
        if (summonOwner(source) != null) return;
        int level = armorLevels(player);
        if (level < 1) return;
        long now = System.currentTimeMillis();
        UUID owner = player.getUniqueId();
        if (cooldowns.getOrDefault(owner, 0L) > now) return;
        double chance = Math.min(1.0, baseChance + (level - 1L) * additionalChance);
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;
        cooldowns.put(owner, now + 1000L * cooldownSeconds);

        int count = (int) Math.min((long) maxPerTrigger,
                (long) perTriggerBase + (long) (level - 1) * perLevelExtra);
        long seconds = Math.min(maxDurationSeconds,
                (long) baseDurationSeconds + (long) (level - 1) * extraDurationSeconds);
        int available = maxPerOwner - countOwner(owner);
        for (int i = 0; i < Math.min(count, available); i++) {
            spawn(player, target, now + seconds * 1000L, i);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerAttacks(EntityDamageByEntityEvent event) {
        if (!helpOwnerAttack || event.getFinalDamage() <= 0) return;
        Entity source = attacker(event.getDamager());
        if (!(source instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity target)) return;
        if (summonOwner(target) != null) return;
        UUID id = player.getUniqueId();
        for (var record : active.entrySet()) {
            if (!record.getValue().owner().equals(id)) continue;
            Entity entity = Bukkit.getEntity(record.getKey());
            if (entity instanceof Mob mob && mob.isValid()) mob.setTarget(target);
        }
    }

    private void spawn(Player player, LivingEntity attacker, long expiry, int offset) {
        EntityType kind = variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
        Location spawn = player.getLocation().clone().add(
                Math.cos(offset * 2.4) * 1.4, 0, Math.sin(offset * 2.4) * 1.4);
        if (!spawn.getBlock().isPassable() || !spawn.clone().add(0, 1, 0).getBlock().isPassable())
            spawn = player.getLocation();
        Entity created;
        try {
            created = player.getWorld().spawnEntity(spawn, kind);
        } catch (IllegalArgumentException exception) {
            EnchantsSquared.logWarning("Could not spawn Summoner ally " + kind + ": " + exception.getMessage());
            return;
        }
        if (!(created instanceof Mob mob)) {
            created.remove();
            return;
        }
        mob.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                player.getUniqueId().toString());
        mob.setPersistent(false);
        mob.setCanPickupItems(false);
        if (mob instanceof Tameable tameable) {
            tameable.setTamed(true);
            tameable.setOwner(player);
        }
        if (copyBukkitAttributes) copyAttributes(player, mob);
        valhalla.capture(player, mob);
        mob.setHealth(Math.max(1.0, mob.getMaxHealth()));
        mob.setCustomName(player.getName() + "'s Ally");
        mob.setCustomNameVisible(false);
        active.put(mob.getUniqueId(), new Summon(player.getUniqueId(), expiry));
        mob.setTarget(attacker);
    }

    private static void copyAttributes(Player owner, Mob mob) {
        // getValue() includes the player's equipment, effects and configured
        // enchant modifiers; summon does NOT equip copied items (no dupes).
        for (Attribute attr : Attribute.values()) {
            AttributeInstance source = owner.getAttribute(attr);
            AttributeInstance destination = mob.getAttribute(attr);
            if (source == null || destination == null) continue;
            try {
                double effective = source.getValue();
                if (Double.isFinite(effective))
                    destination.setBaseValue(effective);
            } catch (IllegalArgumentException | UnsupportedOperationException ignored) {
                // The two entity types do not necessarily have the same valid
                // attribute range or writable attributes.
            }
        }
    }

    private int armorLevels(Player player) {
        if (player.getEquipment() == null) return 0;
        long count = 0;
        for (ItemStack armor : player.getEquipment().getArmorContents()) {
            if (armor != null) count += Math.max(0, armor.getEnchantmentLevel(summoner));
        }
        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    private int countOwner(UUID owner) {
        int count = 0;
        for (Summon info : active.values()) if (info.owner().equals(owner)) count++;
        return count;
    }

    private Entity attacker(Entity source) {
        if (source instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Entity entity ? entity : source;
        }
        return source;
    }

    private UUID summonOwner(Entity entity) {
        if (entity == null || !entity.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING))
            return null;
        String text = entity.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        try { return text == null ? null : UUID.fromString(text); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    /** Runs every second, expiring summons and keeping idle allies close. */
    @Override
    public void run() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Summon>> iter = active.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            Player owner = Bukkit.getPlayer(entry.getValue().owner());
            Entity entity = Bukkit.getEntity(entry.getKey());
            boolean remove = now >= entry.getValue().expiresAt()
                    || owner == null || !owner.isOnline() || owner.isDead()
                    || !(entity instanceof Mob mob) || !mob.isValid()
                    || !entity.getWorld().equals(owner.getWorld());
            if (!remove && entity instanceof Mob mob) {
                double d2 = entity.getLocation().distanceSquared(owner.getLocation());
                if (d2 > dismissDistance * dismissDistance) remove = true;
                else if (d2 > followDistance * followDistance
                        && (mob.getTarget() == null || mob.getTarget().isDead())) {
                    mob.teleport(owner.getLocation());
                }
            }
            if (remove) {
                if (entity != null && entity.isValid()) entity.remove();
                valhalla.remove(entry.getKey());
                iter.remove();
            }
        }
        cooldowns.entrySet().removeIf(e -> e.getValue() < now);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSummonDeath(EntityDeathEvent event) {
        if (summonOwner(event.getEntity()) == null) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        active.remove(event.getEntity().getUniqueId());
        valhalla.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onOwnerLeave(PlayerQuitEvent event) {
        despawnOwner(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onOwnerDeath(PlayerDeathEvent event) {
        despawnOwner(event.getEntity().getUniqueId());
    }

    private void despawnOwner(UUID owner) {
        for (var iter = active.entrySet().iterator(); iter.hasNext();) {
            var entry = iter.next();
            if (!entry.getValue().owner().equals(owner)) continue;
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity != null && entity.isValid()) entity.remove();
            valhalla.remove(entry.getKey());
            iter.remove();
        }
        cooldowns.remove(owner);
    }

    public void shutdown() {
        for (UUID id : new ArrayList<>(active.keySet())) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity.isValid()) entity.remove();
        }
        active.clear();
        cooldowns.clear();
        valhalla.close();
    }
}
