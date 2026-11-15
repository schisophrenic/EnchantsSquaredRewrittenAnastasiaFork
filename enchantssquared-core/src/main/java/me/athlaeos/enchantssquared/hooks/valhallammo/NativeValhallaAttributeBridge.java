package me.athlaeos.enchantssquared.hooks.valhallammo;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.enchantssquared.nativeenchants.NativeAttributeEnchantEngine;
import me.athlaeos.valhallammo.playerstats.AccumulativeStatManager;
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
            final boolean offensive = collector.isAttackerPossessive();
            AccumulativeStatManager.register(stat, new EvEAccumulativeStatSource() {
                @Override
                public double fetch(Entity entity, boolean use) {
                    return entity instanceof Player player
                            ? engine.attributeTotal(player, "VALHALLA:" + stat) : 0;
                }
                @Override
                public double fetch(Entity defender, Entity attacker, boolean use) {
                    Entity owner = offensive ? attacker : defender;
                    return owner instanceof Player player
                            ? engine.attributeTotal(player, "VALHALLA:" + stat) : 0;
                }
            });
            EnchantsSquared.logInfo("Enchantment attribute bridge registered Valhalla stat " + stat);
        }
    }
}
