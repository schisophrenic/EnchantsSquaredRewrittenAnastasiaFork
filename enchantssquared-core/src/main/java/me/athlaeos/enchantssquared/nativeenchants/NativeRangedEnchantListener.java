package me.athlaeos.enchantssquared.nativeenchants;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Native tier effects for bow Power/Punch/Flame and melee Fire Aspect.
 *
 * These enchantments are registered as true Paper Enchantment instances;
 * because they have no vanilla data-component effects, their mechanics
 * are implemented here using the final item levels, including overlevels
 * produced by ValhallaMMO.
 */
public final class NativeRangedEnchantListener implements Listener {
    private final Enchantment[] power = levels("power");
    private final Enchantment[] punch = levels("punch");
    private final Enchantment[] flame = levels("flame");
    private final Enchantment[] fireAspect = levels("fire_aspect");

    private static Enchantment[] levels(String id) {
        return new Enchantment[] {
                Registry.ENCHANTMENT.get(new NamespacedKey("enchantssquared", "lesser_" + id)),
                Registry.ENCHANTMENT.get(new NamespacedKey("enchantssquared", "advanced_" + id)),
                Registry.ENCHANTMENT.get(new NamespacedKey("enchantssquared", "supreme_" + id))
        };
    }

    private static int level(ItemStack item, Enchantment[] group) {
        if (item == null || item.getType().isAir()) return 0;
        int result = 0;
        for (Enchantment enchantment : group) {
            if (enchantment != null) result = Math.max(result, item.getEnchantmentLevel(enchantment));
        }
        return result;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (event.getBow() == null || !(event.getProjectile() instanceof AbstractArrow arrow)) return;
        ItemStack bow = event.getBow();
        int powerLevel = level(bow, power);
        if (powerLevel > 0) {
            // Vanilla Java Power: +0.5 * level + 0.5 projectile base damage.
            arrow.setDamage(Math.max(0.0, arrow.getDamage() + 0.5 * powerLevel + 0.5));
        }
        int punchLevel = level(bow, punch);
        if (punchLevel > 0) {
            arrow.setKnockbackStrength(Math.max(0, arrow.getKnockbackStrength()) + punchLevel);
        }
        if (level(bow, flame) > 0) {
            // Vanilla Flame ignites arrows; the arrow handles igniting its hit.
            arrow.setFireTicks(Math.max(arrow.getFireTicks(), 100));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        // Vanilla Fire Aspect affects the directly struck target,
        // not the secondary targets of a sweeping attack.
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)
                || !(event.getEntity() instanceof LivingEntity victim)) return;
        int fireLevel = level(attacker.getEquipment() == null
                ? null : attacker.getEquipment().getItemInMainHand(), fireAspect);
        if (fireLevel <= 0) return;
        long ticks = Math.min(Integer.MAX_VALUE, 80L * fireLevel);
        victim.setFireTicks((int) Math.max(victim.getFireTicks(), ticks));
    }
}
