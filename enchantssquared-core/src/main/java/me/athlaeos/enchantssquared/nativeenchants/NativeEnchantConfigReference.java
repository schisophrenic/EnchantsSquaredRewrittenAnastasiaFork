package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.valhallammo.item.ItemAttributesRegistry;
import me.athlaeos.valhallammo.playerstats.AccumulativeStatManager;
import org.bukkit.attribute.Attribute;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Appends a refreshable, exhaustive runtime key reference to the END of
 * config_native_enchants.yml as YAML comments. This contains the actual
 * Bukkit/ValhallaMMO installed-version attribute names, not a stale list.
 */
public final class NativeEnchantConfigReference {
    private NativeEnchantConfigReference() {}

    private static final String MARKER = "# === AUTO-GENERATED ATTRIBUTE / TRIGGER REFERENCE ===";

    public static void refresh(EnchantsSquared plugin) {
        Path path = plugin.getDataFolder().toPath().resolve("config_native_enchants.yml");
        try {
            if (!Files.exists(path)) return;
            String original = Files.readString(path, StandardCharsets.UTF_8);
            int old = original.indexOf(MARKER);
            if (old >= 0) original = original.substring(0, old).stripTrailing();
            StringBuilder guide = new StringBuilder(original);
            guide.append("\n\n").append(MARKER).append("\n");
            guide.append("# Values below are comments, not additional enchantment definitions.\n");
            guide.append("# This list is regenerated using the installed Paper + ValhallaMMO APIs.\n\n");
            guide.append("# ENCHANTMENT FIELDS:\n");
            guide.append("# enabled, display_name, enchant_type, rarity, supported_items, slots,\n");
            guide.append("# max_level, weight, min_cost_base, min_cost_per_level,\n");
            guide.append("# max_cost_base, max_cost_per_level, anvil_cost, table_obtainable,\n");
            guide.append("# tradeable, exclusive_set, attribute, scale_per_level,\n");
            guide.append("# attribute_operation, effect, acquisition.\n");
            guide.append("# max_level is a vanilla registry BASE max; effects apply the actual\n");
            guide.append("# item level even when ValhallaMMO has amplified past max_level.\n\n");
            guide.append("# ENCHANT TYPES: armor, sword, weapon, bow, crossbow, trident,\n");
            guide.append("# tool, fishing_rod, utility, curse, custom.\n");
            guide.append("# RARITIES: common, uncommon, rare, epic, legendary.\n");
            guide.append("# ITEM SLOTS: armor, mainhand, offhand, hand, any.\n");
            guide.append("# ATTRIBUTE OPERATIONS: ADD_NUMBER, ADD_SCALAR, MULTIPLY_SCALAR_1.\n");
            guide.append("# ACQUISITION CHANCE FIELDS (0.0 to 1.0): enchanting,\n");
            guide.append("# villager_trade, fishing, structure_chest, mob_drop.\n");
            guide.append("# LIVE EVENT TRIGGERS: ENCHANTING_TABLE, VILLAGER_TRADE,\n");
            guide.append("# FISHING, STRUCTURE_CHEST, MOB_DROP, EQUIPPED, ON_ATTACK,\n");
            guide.append("# ON_DAMAGED, ON_BOW_SHOT. Generic attributes currently support\n");
            guide.append("# EQUIPPED only; other effects use their own Java handlers.\n");
            guide.append("# SUMMONER ARMOR ENCHANT (handler: summon_ally; trigger: ON_DAMAGED):\n");
            guide.append("# summon.mob_types: list of Bukkit Mob types, e.g. WOLF, IRON_GOLEM\n");
            guide.append("# summon.chance_at_level_1, summon.chance_per_level (0-1),\n");
            guide.append("# summon.cooldown_seconds, summon.initial_count, summon.additional_count_per_level,\n");
            guide.append("# summon.max_per_trigger, summon.max_active_per_owner,\n");
            guide.append("# summon.duration_seconds, summon.duration_per_level, summon.max_duration_seconds,\n");
            guide.append("# summon.follow_distance, summon.dismiss_distance, summon.help_owner_attack,\n");
            guide.append("# summon.copy_bukkit_attributes, summon.copy_valhalla_stats (none/combat/all),\n");
            guide.append("# summon.valhalla_stat_multiplier (0-4). Real overlevels are respected.\n");
            guide.append("# Owner / summon friendly fire is blocked. Supported attributes are mirrored\n");
            guide.append("# at spawn; some player-only and opponent-dependent effects are not copied.\n\n");
            guide.append("# PSEUDO ATTRIBUTE: AIR_DRAG (airborne horizontal slowdown rate;\n");
            guide.append("# this is not a vanilla Attribute; max 25% per tick).\n");
            guide.append("# Explicit VALHALLA syntax: \"VALHALLA:MAGIC_RESISTANCE\": 0.02\n");
            guide.append("# Note GRAVITY maps to the real Bukkit GENERIC_GRAVITY attribute.\n\n");

            List<String> vanilla = new ArrayList<>();
            for (Attribute attr : Attribute.values()) vanilla.add(attr.name());
            vanilla.sort(String::compareTo);
            guide.append("# ALL BUKKIT ATTRIBUTE ENUM NAMES (valid with exact name):\n");
            for (String name : vanilla) guide.append("#   ").append(name).append("\n");
            guide.append("# GRAVITY / MOVEMENT_SPEED / MAX_HEALTH also allow GENERIC_ prefix omission.\n");

            if (EnchantsSquared.isValhallaHooked()) {
                guide.append("# ALL VALHALLAMMO STAT SOURCES (use prefix VALHALLA:):\n");
                List<String> stats = new ArrayList<>(AccumulativeStatManager.getSources().keySet());
                stats.sort(String::compareTo);
                for (String name : stats) guide.append("#   VALHALLA:").append(name).append("\n");
                guide.append("# ALL VALHALLAMMO ITEM ATTRIBUTE IDS (for random dropped gear):\n");
                List<String> itemAttrs = new ArrayList<>(ItemAttributesRegistry.getRegisteredAttributes().keySet());
                itemAttrs.sort(String::compareTo);
                for (String name : itemAttrs) guide.append("#   ").append(name).append("\n");
            }
            guide.append("# === END AUTO-GENERATED REFERENCE ===\n");
            Files.writeString(path, guide.toString(), StandardCharsets.UTF_8);
            refreshEasyEnchantGuide(plugin, vanilla);
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not write native enchant YAML reference: " + ex.getMessage());
        }

    /** Append API-generated attribute names and complete supported action keys to the easy YAML. */
    private static void refreshEasyEnchantGuide(EnchantsSquared plugin, List<String> vanilla) {
        Path path = plugin.getDataFolder().toPath().resolve(NativeSimpleEnchantConfig.FILE_NAME);
        final String marker = "# === AUTO-GENERATED EASY ENCHANT REFERENCE ===";
        try {
            if (!Files.exists(path)) return;
            String text = Files.readString(path, StandardCharsets.UTF_8);
            int old = text.indexOf(marker);
            if (old >= 0) text = text.substring(0, old).stripTrailing();
            StringBuilder out = new StringBuilder(text);
            out.append("\n\n").append(marker).append("\n");
            out.append("# These are COMMENTS only; generated from your installed APIs.\n");
            out.append("# CUSTOM ENCHANT FIELDS: enabled, name, type, items, slots, rarity,\n");
            out.append("# weight, max_level, conflicts, min_cost_base, min_cost_per_level,\n");
            out.append("# max_cost_base, max_cost_per_level, anvil_cost, acquisition,\n");
            out.append("# triggers, attribute, scale_per_level, attribute_operation.\n");
            out.append("# TYPES: weapon, armor, boots, bow, mining, fishing.\n");
            out.append("# RARITIES: common, uncommon, rare, epic, legendary.\n");
            out.append("# TRIGGERS: ON_HIT, ON_DAMAGED, ON_KILL, ON_MINE,\n");
            out.append("# ON_SHOOT, ON_FISH, ON_INTERACT.\n");
            out.append("# TRIGGER OPTIONS: chance, chance_per_level, cooldown_seconds,\n");
            out.append("# target_types, blocked_targets, allow_projectiles,\n");
            out.append("# require_target, actions (list).\n");
            out.append("# ACTIONS: CAPTURE_MOB, POTION, HEAL, DAMAGE, IGNITE,\n");
            out.append("# KNOCKBACK, EXPERIENCE, MESSAGE.\n");
            out.append("# ACTION OPTIONS: chance, target (SELF/ENEMY), amount,\n");
            out.append("# per_level, potion, duration_ticks, duration_per_level,\n");
            out.append("# amplifier, amplifier_per_level, ticks, ticks_per_level,\n");
            out.append("# strength, message.\n");
            out.append("# CAPTURE_MOB OPTIONS: allowed_mobs, blocked_mobs,\n");
            out.append("# capture_named_mobs, capture_tamed_mobs,\n");
            out.append("# capture_invulnerable_mobs, max_snapshot_chars.\n");
            out.append("# ACQUISITION: enchanting, villager_trade, fishing,\n");
            out.append("# structure_chest, mob_drop (all probabilities 0.0 to 1.0).\n");
            out.append("# ATTRIBUTE OPERATIONS: ADD_NUMBER, ADD_SCALAR, MULTIPLY_SCALAR_1.\n");
            out.append("# PSEUDO ATTRIBUTE: AIR_DRAG (not vanilla).\n");
            out.append("# ALL BUKKIT ATTRIBUTE ENUM NAMES:\n");
            for (String name : vanilla) out.append("#   ").append(name).append("\n");
            if (EnchantsSquared.isValhallaHooked()) {
                out.append("# ALL REGISTERED VALHALLAMMO STAT SOURCES (use VALHALLA: prefix):\n");
                List<String> stats = new ArrayList<>(AccumulativeStatManager.getSources().keySet());
                stats.sort(String::compareTo);
                for (String stat : stats) out.append("#   VALHALLA:").append(stat).append("\n");
                out.append("# ALL VALHALLAMMO ITEM ATTRIBUTE IDS (gear rolls):\n");
                List<String> gear = new ArrayList<>(ItemAttributesRegistry.getRegisteredAttributes().keySet());
                gear.sort(String::compareTo);
                for (String attr : gear) out.append("#   ").append(attr).append("\n");
            }
            out.append("# === END AUTO-GENERATED EASY ENCHANT REFERENCE ===\n");
            Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not write custom enchant reference: " + ex.getMessage());
        }
    }
    }
}
