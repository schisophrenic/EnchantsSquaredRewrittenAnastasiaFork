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
    private final double magicPerLevel;
    private final double magicMaximum;

    public NativeProtectionEnchantListener(YamlConfiguration nativeConfig) {
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

        long vanillaEpf = 0;
        long nativeEpf = 0;
        long magicLevels = 0;
        for (ItemStack item : armor) {
            if (item == null || item.getType().isAir()) continue;
            if (general) {
                vanillaEpf += vanilla(item, vanillaTypes.get("protection"));
                nativeEpf += total(item, nativeTypes.get("protection"));
            }
            if (fire) {
                vanillaEpf += 2L * vanilla(item, vanillaTypes.get("fire_protection"));
                nativeEpf += 2L * total(item, nativeTypes.get("fire_protection"));
            }
            if (explosion) {
                vanillaEpf += 2L * vanilla(item, vanillaTypes.get("blast_protection"));
                nativeEpf += 2L * total(item, nativeTypes.get("blast_protection"));
            }
            if (projectile) {
                vanillaEpf += 2L * vanilla(item, vanillaTypes.get("projectile_protection"));
                nativeEpf += 2L * total(item, nativeTypes.get("projectile_protection"));
            }
            if (falling) {
                vanillaEpf += 3L * vanilla(item, vanillaTypes.get("feather_falling"));
            }
            if (elemental) magicLevels += total(item, nativeTypes.get("magic_protection"));
        }
        if (nativeEpf <= 0 && magicLevels <= 0) return;

        // Vanilla handles the vanilla EPF itself; only add the missing
        // percentage to emulate a combined EPF cap without double counting.
        double already = Math.min(0.80, vanillaEpf * 0.04);
        double allStandard = Math.min(0.80, (vanillaEpf + nativeEpf) * 0.04);
        double desired = elemental
                ? Math.min(magicMaximum, allStandard + magicLevels * magicPerLevel)
                : allStandard;
        desired = Math.max(already, desired);
        if (desired <= already) return;
        double additionalFraction = (desired - already) / (1.0 - already);
        event.setDamage(Math.max(0.0, event.getDamage() * (1.0 - additionalFraction)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim) || victim.getEquipment() == null) return;
        int levels = 0;
        for (ItemStack armor : victim.getEquipment().getArmorContents()) {
            levels += total(armor, nativeTypes.get("fire_protection"));
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
