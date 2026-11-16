package me.athlaeos.enchantssquared.hooks.valhallammo;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.valhallammo.playerstats.AccumulativeStatManager;
import me.athlaeos.valhallammo.playerstats.AccumulativeStatSource;
import me.athlaeos.valhallammo.playerstats.EvEAccumulativeStatSource;
import me.athlaeos.valhallammo.playerstats.StatCollector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Copies resolved ValhallaMMO combat stats from a summoner at summon-time.
 *
 * ValhallaMMO's combat stat sources are not Bukkit Attributes: directly
 * setting an entity's base attributes cannot copy crit/bleed/elemental
 * resistances. An additional source is added to each selected Valhalla
 * collector and returns the SNAPSHOT difference for summoned mobs only.
 *
 * The delta makes the mob's own existing stat contribution + this source
 * equal the player's effective stat at spawn, without doubling it.
 * Relational victim/attacker stats are approximate snapshots and can change
 * depending on the opponent; such dynamic interaction is not copied exactly.
 */
public final class NativeSummonValhallaBridge {
    private final Map<String, StatCollector> mapped = new LinkedHashMap<>();
    private final Map<String, AccumulativeStatSource> sources = new HashMap<>();
    private final Map<UUID, Map<String, Double>> mobBonuses = new HashMap<>();
    private final double multiplier;

    public NativeSummonValhallaBridge(String mode, double multiplier) {
        this.multiplier = Double.isFinite(multiplier)
                ? Math.max(0, Math.min(4, multiplier)) : 1.0;
        if (!EnchantsSquared.isValhallaHooked()
                || "none".equalsIgnoreCase(mode)) return;

        boolean all = "all".equalsIgnoreCase(mode);
        for (var pair : AccumulativeStatManager.getSources().entrySet()) {
            String id = pair.getKey();
            StatCollector collector = pair.getValue();
            if (collector == null || (!all && !isCombatStat(id))) continue;
            SnapshotSource source = new SnapshotSource(id, collector.isAttackerPossessive());
            collector.getStatSources().add(source);
            mapped.put(id, collector);
            sources.put(id, source);
        }
        EnchantsSquared.logInfo("Summoner can snapshot " + mapped.size()
                + " ValhallaMMO " + (all ? "registered" : "combat") + " stat groups.");
    }

    private static boolean isCombatStat(String id) {
        String key = id.toUpperCase(Locale.ROOT);
        return key.contains("ARMOR") || key.contains("DAMAGE")
                || key.contains("HEALTH") || key.contains("RESIST")
                || key.contains("CRIT") || key.contains("DODGE")
                || key.contains("BLEED") || key.contains("STUN")
                || key.contains("ATTACK") || key.contains("REACH")
                || key.contains("REFLECT") || key.contains("IMMUNITY")
                || key.contains("LIFE_STEAL") || key.contains("KNOCKBACK")
                || key.contains("SPEED") || key.contains("GRAVITY")
                || key.contains("BLOCK") || key.contains("HEAL")
                || key.contains("ABSORPTION") || key.contains("FIRE")
                || key.contains("FREEZING") || key.contains("MAGIC")
                || key.contains("LIGHTNING") || key.contains("RADIANT")
                || key.contains("POISON") || key.contains("NECROTIC")
                || key.contains("MOVEMENT") || key.contains("VELOCITY");
    }

    public void capture(Player player, Mob summon) {
        if (mapped.isEmpty()) return;
        Map<String, Double> deltas = new HashMap<>();
        for (String stat : mapped.keySet()) {
            try {
                // 'false' means display/read only: do not consume perks,
                // potion charges or other one-use ValhallaMMO stat sources.
                double owner = AccumulativeStatManager.getStats(stat, player, false);
                double current = AccumulativeStatManager.getStats(stat, summon, false);
                double delta = owner * multiplier - current;
                if (Double.isFinite(delta) && delta != 0.0) deltas.put(stat, delta);
            } catch (RuntimeException | LinkageError ignored) {
                // Some stats are only valid for players or are relational.
                // Ignore those instead of breaking the summoning mechanic.
            }
        }
        mobBonuses.put(summon.getUniqueId(), Map.copyOf(deltas));
    }

    public void remove(UUID mobId) {
        mobBonuses.remove(mobId);
    }

    public void close() {
        mobBonuses.clear();
        for (var pair : sources.entrySet()) {
            StatCollector collector = mapped.get(pair.getKey());
            if (collector != null) collector.getStatSources().remove(pair.getValue());
        }
        mapped.clear();
        sources.clear();
    }

    private final class SnapshotSource implements AccumulativeStatSource, EvEAccumulativeStatSource {
        private final String key;
        private final boolean attackerPossessive;

        private SnapshotSource(String key, boolean attackerPossessive) {
            this.key = key;
            this.attackerPossessive = attackerPossessive;
        }

        @Override
        public double fetch(Entity possessor, boolean use) {
            if (possessor == null) return 0;
            return mobBonuses.getOrDefault(possessor.getUniqueId(), Map.of())
                    .getOrDefault(key, 0.0);
        }

        @Override
        public double fetch(Entity victim, Entity attacker, boolean use) {
            return fetch(attackerPossessive ? attacker : victim, use);
        }
    }
}
