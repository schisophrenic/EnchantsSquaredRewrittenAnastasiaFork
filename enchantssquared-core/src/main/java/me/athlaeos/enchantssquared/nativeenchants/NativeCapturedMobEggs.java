package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Uses native Paper EntitySnapshot in a REAL vanilla spawn egg.
 *
 * Unlike rebuilding only selected attributes, SpawnEggMeta#setSpawnedEntity
 * stores the entity's NBT-backed snapshot as the egg's native entity data
 * component. Placing the egg is standard Minecraft/Paper behavior: no custom
 * spawn handler, extra mob head, or Slimefun dependency is necessary.
 *
 * Capturing consumes the existing mob WITHOUT invoking a death/loot event.
 * Bosses, plugin-owned NPCs/mobs and owner's summoned allies are protected.
 */
public final class NativeCapturedMobEggs {
    private final EnchantsSquared plugin;
    private final NamespacedKey captureKey;
    private final AtomicBoolean warnedElite = new AtomicBoolean();
    private final AtomicBoolean warnedMythic = new AtomicBoolean();

    public NativeCapturedMobEggs(EnchantsSquared plugin) {
        this.plugin = plugin;
        this.captureKey = new NamespacedKey(plugin, "captured_mob_id");
    }

    public boolean capture(Player player, EntityDamageByEntityEvent event,
                           ConfigurationSection action) {
        if (!(event.getEntity() instanceof Mob mob) || event.isCancelled()
                || !mob.isValid() || mob.isDead()) return false;
        if (!allowed(mob, action)) return false;

        Material material = Material.matchMaterial(mob.getType().name() + "_SPAWN_EGG");
        if (material == null || !material.name().endsWith("_SPAWN_EGG")) return false;

        EntitySnapshot state;
        try {
            state = mob.createSnapshot();
            if (state == null || state.getEntityType() != mob.getType()) return false;
            int maxChars = Math.max(4096, Math.min(1048576,
                    action.getInt("max_snapshot_chars", 131072)));
            if (state.getAsString().length() > maxChars) return false;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Cannot capture snapshot of " + mob.getType()
                    + ": " + exception.getMessage());
            return false;
        }

        ItemStack egg = new ItemStack(material);
        ItemMeta itemMeta = egg.getItemMeta();
        if (!(itemMeta instanceof SpawnEggMeta spawnEggMeta)) return false;
        try {
            // Native Minecraft entity_data component: stores mob NBT, custom
            // name, attributes, modifiers, equipment, potion effects, PDC, etc.
            spawnEggMeta.setSpawnedEntity(state);
            String text = mob.getCustomName() != null
                    ? ChatColor.stripColor(mob.getCustomName())
                    : pretty(mob.getType().name());
            text = text == null ? "Mob" : text;
            if (text.length() > 58) text = text.substring(0, 58);
            spawnEggMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Captured " + text);
            spawnEggMeta.getPersistentDataContainer().set(captureKey,
                    PersistentDataType.STRING, UUID.randomUUID().toString());
            egg.setItemMeta(spawnEggMeta);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Could not write native captured spawn egg: "
                    + exception.getMessage());
            return false;
        }

        // Do NOT kill with setHealth(0): doing so would cause a death drop,
        // creating a possible duplicate of the equipment retained in the egg.
        // Other plugins can still veto the original attack by cancelling it
        // before this handler runs.
        event.setCancelled(true);
        mob.remove();

        // Grant the egg only after the original has been removed.
        var overflow = player.getInventory().addItem(egg);
        for (ItemStack extra : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
        return true;
    }

    private boolean allowed(Mob mob, ConfigurationSection options) {
        String type = mob.getType().name();
        List<String> blocked = options.getStringList("blocked_mobs");
        if (blocked.isEmpty()) blocked = List.of("WITHER", "ENDER_DRAGON", "WARDEN");
        if (blocked.stream().anyMatch(name -> name.equalsIgnoreCase(type))) return false;
        List<String> allowed = options.getStringList("allowed_mobs");
        if (!allowed.isEmpty() && allowed.stream().noneMatch(name -> name.equalsIgnoreCase(type))) return false;
        if (!options.getBoolean("capture_named_mobs", true) && mob.getCustomName() != null) return false;
        if (!options.getBoolean("capture_tamed_mobs", false)
                && mob instanceof Tameable tame && tame.isTamed()) return false;
        if (!options.getBoolean("capture_invulnerable_mobs", false) && mob.isInvulnerable()) return false;
        if (mob.getVehicle() != null || !mob.getPassengers().isEmpty()) return false;

        if (mob.hasMetadata("NPC") || mob.hasMetadata("MythicMob")
                || mob.hasMetadata("EliteMob") || mob.hasMetadata("SuperMob")
                || mob.hasMetadata("Mythic_Mob") || mob.getScoreboardTags().contains("NPC")) return false;
        for (NamespacedKey key : mob.getPersistentDataContainer().getKeys()) {
            String ns = key.getNamespace().toLowerCase(Locale.ROOT);
            if (ns.equals("mythicmobs") || ns.equals("elitemobs")
                    || ns.equals("citizens") || ns.equals("znpcs")
                    || (ns.equals("enchantssquared") &&
                    key.getKey().equals("summoned_mob_owner"))) return false;
        }
        return !ownedByEliteMobs(mob) && !ownedByMythicMobs(mob);
    }

    private boolean ownedByEliteMobs(Mob mob) {
        Plugin other = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (other == null || !other.isEnabled()) return false;
        try {
            Class<?> tagger = Class.forName(
                    "com.magmaguy.elitemobs.tagger.PersistentTagger", false,
                    other.getClass().getClassLoader());
            int methods = 0;
            for (String name : List.of("isEliteEntity", "isNPC", "isSuperMob")) {
                Method method = optional(tagger, name, Entity.class);
                if (method == null) continue;
                methods++;
                if (Boolean.TRUE.equals(method.invoke(null, mob))) return true;
            }
            if (methods > 0) return false;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Fall through: if the integration isn't reliable, fail closed.
        }
        if (warnedElite.compareAndSet(false, true))
            plugin.getLogger().warning("EliteMobs API not available; native egg capture "
                    + "will refuse mobs while EliteMobs is enabled.");
        return true;
    }

    private boolean ownedByMythicMobs(Mob mob) {
        Plugin other = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (other == null || !other.isEnabled()) return false;
        try {
            Class<?> mythic = Class.forName("io.lumine.mythic.bukkit.MythicBukkit",
                    false, other.getClass().getClassLoader());
            Object instance = mythic.getMethod("inst").invoke(null);
            Object manager = instance.getClass().getMethod("getMobManager").invoke(instance);
            int methods = 0;
            for (String name : List.of("isMythicMob", "isActiveMob")) {
                Method entity = optional(manager.getClass(), name, Entity.class);
                if (entity != null) {
                    methods++;
                    if (Boolean.TRUE.equals(entity.invoke(manager, mob))) return true;
                }
            }
            Method uuidCheck = optional(manager.getClass(), "isActiveMob", UUID.class);
            if (uuidCheck != null) {
                methods++;
                if (Boolean.TRUE.equals(uuidCheck.invoke(manager, mob.getUniqueId()))) return true;
            }
            Method lookup = optional(manager.getClass(), "getActiveMob", UUID.class);
            if (lookup != null) {
                methods++;
                Object result = lookup.invoke(manager, mob.getUniqueId());
                if (result instanceof Optional<?> option) {
                    if (option.isPresent()) return true;
                } else if (result != null) return true;
            }
            if (methods > 0) return false;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Fail closed on unknown MythicMobs API.
        }
        if (warnedMythic.compareAndSet(false, true))
            plugin.getLogger().warning("MythicMobs API not available; native egg capture "
                    + "will refuse mobs while MythicMobs is enabled.");
        return true;
    }

    private static Method optional(Class<?> owner, String method, Class<?> parameter) {
        try {
            return owner.getMethod(method, parameter);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static String pretty(String type) {
        String lower = type.toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder result = new StringBuilder();
        boolean capitalize = true;
        for (int index = 0; index < lower.length(); index++) {
            char c = lower.charAt(index);
            result.append(capitalize ? Character.toUpperCase(c) : c);
            capitalize = c == ' ';
        }
        return result.toString();
    }
}
