package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.valhallammo.utility.EntityUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Native Java enchantment effects for the four protection families and
 * the new Magic Protection, using the ACTUAL enchanted item level (VI+).
 *
 * Vanilla protection contributes to the 80% EPF cap already. We apply only
 * the extra native-enchantment benefit, avoiding double reduction.
 * For elemental damage the separate Magic Protection family can raise the
 * combined enchant-based cap to 85%, never reaching 90%, across all armor.
 *
 * Other systems (e.g. ValhallaMMO standalone resistance stats, armor/toughness,
 * potion effects) remain separate; this listener does not override them.
 */
public final class NativeProtectionEnchantListener implements Listener {
    private static final String[] FAMILIES = {
            "protection", "fire_protection", "blast_protection",
            "projectile_protection", "magic_protection"
    };
    private static final Set<String> CUSTOM_ELEMENTAL = Set.of(
            "FIRE", "MAGIC", "FREEZING", "FREEZE", "LIGHTNING",
            "POISON", "NECROTIC", "WITHER", "RADIANT",
            "ENTITY_EXPLOSION", "BLOCK_EXPLOSION", "EXPLOSION", "DRAGON_BREATH");
    private final Map<String, Enchantment[]> nativeTypes = new HashMap<>();
    private final Map<String, Enchantment> vanillaTypes = new HashMap<>();
    private final EnchantmentPriority priority;
    private final double magicPerLevel;
    private final double magicMaximum;

    public NativeProtectionEnchantListener(YamlConfiguration nativeConfig) {
        priority = new EnchantmentPriority(nativeConfig.getConfigurationSection("priority_scaling"));
        magicPerLevel = Math.max(0.0, Math.min(0.1,
                nativeConfig.getDouble("settings.magic_protection_percent_per_level", 0.03)));
        // Hard safety ceiling: even a malicious/accidental YAML value of 1
        // can never make Magic Protection provide 90% or more reduction.
        magicMaximum = Math.max(0.0, Math.min(0.85,
                nativeConfig.getDouble("settings.magic_protection_max_reduction", 0.85)));
        for (String family : FAMILIES) {
            nativeTypes.put(family, new Enchantment[] {
                    find("enchantssquared", family),
                    find("enchantssquared", "lesser_" + family),
                    find("enchantssquared", "advanced_" + family),
                    find("enchantssquared", "supreme_" + family)
            });
            vanillaTypes.put(family, find("minecraft", family));
        }
        vanillaTypes.put("feather_falling", find("minecraft", "feather_falling"));
    }

    private static Enchantment find(String namespace, String key) {
        return Registry.ENCHANTMENT.get(new NamespacedKey(namespace, key));
    }

    private static int total(ItemStack item, Enchantment[] entries) {
        if (item == null || item.getType().isAir()) return 0;
        // Each family can appear at most once on a normal item. If a test
        // item forcibly combines its tiers, take the strongest tier instead
        // of counting duplicates from the same family.
        int max = 0;
        for (Enchantment e : entries) {
            if (e != null) max = Math.max(max, item.getEnchantmentLevel(e));
        }
        return max;
    }

    private static int vanilla(ItemStack item, Enchantment enchantment) {
        return item == null || enchantment == null ? 0 : item.getEnchantmentLevel(enchantment);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim) || victim.getEquipment() == null) return;
        ItemStack[] armor = victim.getEquipment().getArmorContents();
        EntityDamageEvent.DamageCause cause = event.getCause();
        boolean fire = switch (cause) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, CAMPFIRE, MELTING -> true;
            default -> false;
        };
        boolean explosion = cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
        boolean projectile = cause == EntityDamageEvent.DamageCause.PROJECTILE;
        boolean falling = cause == EntityDamageEvent.DamageCause.FALL;
        boolean general = switch (cause) {
            case STARVATION, VOID, KILL, SONIC_BOOM, WORLD_BORDER -> false;
            default -> true;
        };
        String custom = customDamageType(victim);
        if (custom != null) {
            fire |= custom.equals("FIRE");
            explosion |= custom.contains("EXPLOSION");
            projectile |= custom.equals("PROJECTILE");
        }
        boolean elemental = isElemental(cause, custom);

        double vanillaEpf = 0;
        double adjustedEpf = 0;
        double magicLevels = 0;
        boolean mixed = false;
        for (ItemStack item : armor) {
            if (item == null || item.getType().isAir()) continue;
            Map<String, Double> weights = priority.weights(item, true);
            mixed |= weights.size() > 1;
            if (general) {
                int old = vanilla(item, vanillaTypes.get("protection"));
                vanillaEpf += old;
                adjustedEpf += old * weights.getOrDefault("protection", 0.0)
                        + total(item, nativeTypes.get("protection")) * weights.getOrDefault("protection", 0.0);
            }
            if (fire) {
                int old = vanilla(item, vanillaTypes.get("fire_protection"));
                vanillaEpf += 2.0 * old;
                adjustedEpf += 2.0 * (old + total(item, nativeTypes.get("fire_protection")))
                        * weights.getOrDefault("fire_protection", 0.0);
            }
            if (explosion) {
                int old = vanilla(item, vanillaTypes.get("blast_protection"));
                vanillaEpf += 2.0 * old;
                adjustedEpf += 2.0 * (old + total(item, nativeTypes.get("blast_protection")))
                        * weights.getOrDefault("blast_protection", 0.0);
            }
            if (projectile) {
                int old = vanilla(item, vanillaTypes.get("projectile_protection"));
                vanillaEpf += 2.0 * old;
                adjustedEpf += 2.0 * (old + total(item, nativeTypes.get("projectile_protection")))
                        * weights.getOrDefault("projectile_protection", 0.0);
            }
            if (falling) {
                double fallingEpf = 3.0 * vanilla(item, vanillaTypes.get("feather_falling"));
                vanillaEpf += fallingEpf;
                adjustedEpf += fallingEpf;
            }
            if (elemental) {
                magicLevels += total(item, nativeTypes.get("magic_protection"))
                        * weights.getOrDefault("magic_protection", 0.0);
            }
        }
        if (!mixed && adjustedEpf == vanillaEpf && magicLevels <= 0) return;

        // Bukkit's vanilla enchantment mitigation is already part of the
        // pipeline. Convert from its unmodified EPF to our target EPF once;
        // unlike an extra reduction, this can also reduce stacked vanilla
        // protections without deleting their enchantments.
        double vanillaFraction = Math.min(0.80, vanillaEpf * 0.04);
        double desired = Math.min(0.80, adjustedEpf * 0.04);
        if (elemental) desired = Math.min(magicMaximum, desired + magicLevels * magicPerLevel);
        desired = Math.max(0.0, desired);
        double correction = (1.0 - desired) / (1.0 - vanillaFraction);
        event.setDamage(Math.max(0.0, event.getDamage() * correction));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim) || victim.getEquipment() == null) return;
        double levels = 0;
        for (ItemStack armor : victim.getEquipment().getArmorContents()) {
            levels += total(armor, nativeTypes.get("fire_protection"))
                    * priority.multiplier(armor, "fire_protection", true);
        }
        if (levels <= 0) return;
        // Vanilla Fire Protection also shortens burning duration by 15%
        // per level, capped at 80%; apply for native tiered versions.
        double reduction = Math.min(0.80, levels * 0.15);
        event.setDuration(Math.max(0, (int) Math.ceil(event.getDuration() * (1 - reduction))));
    }

    private static boolean isElemental(EntityDamageEvent.DamageCause cause, String custom) {
        if (custom != null && CUSTOM_ELEMENTAL.contains(custom)) return true;
        return switch (cause) {
            case MAGIC, DRAGON_BREATH, POISON, WITHER, FREEZE, LIGHTNING,
                    FIRE, FIRE_TICK, LAVA, HOT_FLOOR, CAMPFIRE, MELTING,
                    ENTITY_EXPLOSION, BLOCK_EXPLOSION -> true;
            default -> false;
        };
    }

    private static String customDamageType(LivingEntity entity) {
        if (!EnchantsSquared.isValhallaHooked()) return null;
        try {
            String type = EntityUtils.getCustomDamageType(entity);
            return type == null ? null : type.toUpperCase(Locale.ROOT);
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
