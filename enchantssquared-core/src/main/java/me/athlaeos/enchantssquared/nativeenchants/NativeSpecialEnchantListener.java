package me.athlaeos.enchantssquared.nativeenchants;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/** Distinct native enchantments; existing Poisoning/Withering and Rapid Shot are not duplicated. */
public final class NativeSpecialEnchantListener implements Listener {
    private final YamlConfiguration config;
    private final Enchantment disorient;
    private final Enchantment evasion;
    private final Enchantment splitshot;
    private final Enchantment strafe;

    public NativeSpecialEnchantListener(YamlConfiguration config) {
        this.config = config;
        disorient = find("disorientating_blade");
        evasion = find("evasion");
        splitshot = find("splitshot");
        strafe = find("strafe");
    }

    private static Enchantment find(String id) {
        return Registry.ENCHANTMENT.get(new NamespacedKey("enchantssquared", id));
    }

    private static int level(ItemStack item, Enchantment enchantment) {
        return item == null || enchantment == null ? 0 : item.getEnchantmentLevel(enchantment);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (evasion != null && event.getEntity() instanceof LivingEntity defender
                && defender.getEquipment() != null) {
            double chance = 0;
            ConfigurationSection settings = config.getConfigurationSection("enchantments.evasion");
            double perLevel = settings == null ? 0.04 : settings.getDouble("dodge_chance_per_level", 0.04);
            double cap = settings == null ? 0.50 : settings.getDouble("maximum_dodge_chance", 0.50);
            for (ItemStack piece : defender.getEquipment().getArmorContents()) {
                chance += Math.max(0, level(piece, evasion)) * perLevel;
            }
            chance = Math.min(0.90, Math.max(0, Math.min(chance, cap)));
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                // Evasion is a real chance to dodge a hit, not a resistance
                // multiplier, and is intentionally distinct from Shielding.
                event.setCancelled(true);
                return;
            }
        }
        if (disorient == null
                || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || !(event.getDamager() instanceof LivingEntity attacker)
                || attacker.getEquipment() == null
                || !(event.getEntity() instanceof LivingEntity target)) return;
        int lvl = level(attacker.getEquipment().getItemInMainHand(), disorient);
        if (lvl < 1) return;
        double chance = Math.min(0.80, 0.08 + 0.03 * Math.max(0, lvl - 1));
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;
        int ticks = (int) Math.min(200, 60L + 20L * lvl);
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, ticks, 0));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShot(EntityShootBowEvent event) {
        if (event.getBow() == null || !(event.getProjectile() instanceof AbstractArrow arrow)) return;
        int strafeLevel = level(event.getBow(), strafe);
        if (strafeLevel > 0 && event.getForce() < 1.0f) {
            // Bukkit does not expose Forge's bow-use-duration tick mutation.
            // Faster effective draw is approximated by boosting an early
            // released arrow, without changing a fully charged arrow.
            double boost = 1.0 + Math.min(1.0 - event.getForce(),
                    0.06 * Math.max(1, strafeLevel));
            arrow.setVelocity(arrow.getVelocity().multiply(boost));
            arrow.setDamage(arrow.getDamage() * boost);
        }

        int splitLevel = level(event.getBow(), splitshot);
        if (splitLevel < 1 || !(event.getProjectile() instanceof Arrow original)) return;
        LivingEntity shooter = event.getEntity();
        int max = Math.max(0, config.getInt("enchantments.splitshot.max_secondary_arrows", 12));
        int count = Math.min(max, splitLevel); // VI+ gives more arrows than III.
        if (count < 1) return;
        Vector initial = original.getVelocity();
        if (initial.lengthSquared() < 0.0001) return;
        double baseDamage = original.getDamage();
        double fraction = Math.max(0, Math.min(1,
                config.getDouble("enchantments.splitshot.secondary_damage_fraction", 0.35)));
        for (int i = 0; i < count; i++) {
            // Alternate left/right with widening spread, so the extra
            // arrows can hit separate targets instead of stacking perfectly.
            double angle = Math.toRadians(((i / 2) + 1) * 5.0 * (i % 2 == 0 ? -1 : 1));
            Vector spread = initial.clone().rotateAroundY(angle);
            Arrow extra = shooter.getWorld().spawnArrow(shooter.getEyeLocation(),
                    spread.normalize(), (float) initial.length(), 0);
            extra.setShooter(shooter);
            extra.setDamage(baseDamage * fraction);
            extra.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            if (original.getFireTicks() > 0) extra.setFireTicks(original.getFireTicks());
        }
    }
}
