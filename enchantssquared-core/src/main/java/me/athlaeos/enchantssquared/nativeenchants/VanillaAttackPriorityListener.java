package me.athlaeos.enchantssquared.nativeenchants;

import org.bukkit.Tag;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

/**
 * Compensates for built-in vanilla melee enchant bonuses on stacked weapons.
 * Custom EnchantsSquared damage is independently scaled in NativeYamlEffectListener.
 */
public final class VanillaAttackPriorityListener implements Listener {
    private final EnchantmentPriority priority;

    public VanillaAttackPriorityListener(YamlConfiguration configuration) {
        priority = new EnchantmentPriority(configuration.getConfigurationSection("priority_scaling"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK
                && event.getCause() != DamageCause.ENTITY_SWEEP_ATTACK) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)
                || attacker.getEquipment() == null) return;
        ItemStack weapon = attacker.getEquipment().getItemInMainHand();
        if (priority.weights(weapon, false).size() < 2) return;

        double correction = 0.0;
        for (String family : EnchantmentPriority.OFFENSE) {
            int level = vanillaLevel(weapon, family);
            if (level <= 0) continue;
            double naturalBonus = switch (family) {
                case "sharpness" -> 0.5 * level + 0.5;
                case "smite" -> Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE.isTagged(event.getEntityType())
                        ? 2.5 * level : 0.0;
                case "bane_of_arthropods" -> Tag.ENTITY_TYPES_SENSITIVE_TO_BANE_OF_ARTHROPODS.isTagged(event.getEntityType())
                        ? 2.5 * level : 0.0;
                default -> 0.0;
            };
            correction += naturalBonus * (1.0 - priority.multiplier(weapon, family, false));
        }
        if (correction > 0) event.setDamage(Math.max(0.0, event.getDamage() - correction));
    }

    private static int vanillaLevel(ItemStack weapon, String family) {
        for (var entry : weapon.getEnchantments().entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment.getKey().getNamespace().equals("minecraft")
                    && enchantment.getKey().getKey().equals(family)) return entry.getValue();
        }
        return 0;
    }
}
