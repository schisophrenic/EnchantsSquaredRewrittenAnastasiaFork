package me.athlaeos.enchantssquared.hooks.valhallammo;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.enchantssquared.nativeenchants.NativeAttributeEnchantEngine;
import me.athlaeos.valhallammo.playerstats.AccumulativeStatManager;
import me.athlaeos.valhallammo.playerstats.AccumulativeStatSource;
import me.athlaeos.valhallammo.playerstats.EvEAccumulativeStatSource;
import me.athlaeos.valhallammo.playerstats.StatCollector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

/** Registers real ValhallaMMO stat sources for explicitly prefixed attribute keys. */
public final class NativeValhallaAttributeBridge {
    private NativeValhallaAttributeBridge() {}

    public static void register(NativeAttributeEnchantEngine engine) {
        Set<String> added = new HashSet<>();
        for (String name : engine.usedValhallaStats()) {
            String stat = name.toUpperCase(java.util.Locale.ROOT);
            StatCollector collector = AccumulativeStatManager.getSources().get(stat);
            if (collector == null) {
                EnchantsSquared.logWarning("Unknown ValhallaMMO stat for native enchant attributes: " + stat);
                continue;
            }
            if (!added.add(stat)) continue;
            // Enchanting effects scale with actual enchant level; no maxLevel
            // check here. The source recomputes from currently equipped gear.
            AccumulativeStatManager.register(stat,
                    new EquippedEnchantStatSource(engine, stat, collector.isAttackerPossessive()));
            EnchantsSquared.logInfo("Enchantment attribute bridge registered Valhalla stat " + stat);
        }
    }
    /**
     * Some ValhallaMMO builds expose EvEAccumulativeStatSource as a separate
     * interface rather than extending AccumulativeStatSource. Implement both
     * so relational attacker/defender calculations use the correct owner.
     */
    private static final class EquippedEnchantStatSource
            implements AccumulativeStatSource, EvEAccumulativeStatSource {
        private final NativeAttributeEnchantEngine engine;
        private final String id;
        private final boolean attackerPossessive;

        private EquippedEnchantStatSource(NativeAttributeEnchantEngine engine,
                                          String id, boolean attackerPossessive) {
            this.engine = engine;
            this.id = id;
            this.attackerPossessive = attackerPossessive;
        }

        @Override
        public double fetch(Entity entity, boolean use) {
            return entity instanceof Player p ? engine.attributeTotal(p, "VALHALLA:" + id) : 0;
        }

        @Override
        public double fetch(Entity defender, Entity attacker, boolean use) {
            return fetch(attackerPossessive ? attacker : defender, use);
        }
    }

}
