# Eterna, Quanta & Arcana — Compatibility Assessment

**Date:** 2026-11-16  
**Status:** **Feasible, not implemented.** This assessment does not change enchantment rolls, registration, levels or server menus.  
**Target:** EnchantsSquared fork + ValhallaMMO Premium + Purpur 26.3, based on `feature/enchantment-evolution-no-stacking-26-3`.  
**Primary reference:** User-supplied binary `ApothicEnchanting-1.21.1-1.6.4.jar` (NeoForge). Inspected `EnchantmentTableStats`, `EnchantingStatRegistry`, `ApothEnchantmentHelper`, `Arcana`, `ApothEnchantmentMenu` class signatures/bytecode and the bundled `enchanting_stats/*.json` resources.

## Verdict

**All three are implementable as independent server-side enchanting statistics.** They are NOT drop-in classes or a runtime integration with the NeoForge JAR. Recreate the required data and RNG logic with Paper, keeping ValhallaMMO responsible for existing skill bonuses and New Game+ tier eligibility.

| Stat | Observed role in supplied Apothic JAR | Compatibility | Correct ownership |
|---|---|---|---|
| **Eterna** | Enchanting power/offer cost, gathered from nearby stat-providing blocks; table Eterna is capped by a player Max Eterna attribute | **Yes, moderate effort** | Controls *base offer power*, not the ValhallaMMO `ENCHANTING_QUALITY` or item quality stat |
| **Quanta** | Randomizes effective enchantment **power**; stable tables make it positive-only | **Yes, moderate effort** | Modify the *initial power roll*, before ValhallaMMO adjusts final enchantment levels |
| **Arcana** | Reweights enchantment rarities and grants additional compatible enchantment picks at configured thresholds | **Yes, moderate-to-high effort** | Applied to eligible native selection; never bypasses NG/perk gates, item restrictions or evolution conflicts |

### Findings from the exact 1.6.4 JAR

**Block statistics**: 26 `data/apothic_enchanting/enchanting_stats/*.json` definitions were present. They can use individual block IDs or tags, each adding `eterna`, `maxEterna`, `quanta`, `arcana`, and/or `clues`. A few source examples:

- `apothic_enchanting:basic_bookshelf` gives **Eterna +2**.
- `apothic_enchanting:glowing_hellshelf` gives **Max Eterna 60, Eterna +5, Quanta +5, Arcana +3.3333**.
- `apothic_enchanting:beeshelf` supplies **Eterna −30 and Quanta +100**.
- `minecraft:amethyst_cluster` gives **Max Eterna 40, Eterna +1, Quanta −1**.

The Apothic-specific shelves **do not exist as vanilla Purpur blocks**. To use equivalent server-side bookshelf types, we must define our own Bukkit Material mappings or optional Oraxen custom-block identification. Copying the mod's JSON IDs directly will not place its blocks in the world.

**Eterna**: `EnchantmentTableStats.gatherStats` scans valid enchanting shelf positions with a transmitter/line-of-sight check. The Builder accumulates Eterna by Max Eterna ceilings. `eterna(player)` returns `min(tableEterna, player MAX_ETERNA)`. A full reimplementation should preserve accumulation and cap rules instead of simply adding 100 to ValhallaMMO enchanting quality.

**Quanta**: The supplied `ApothEnchantmentHelper.getQuantaFactor` computes:
- *Unstable:* `1 + (Quanta/100) × clamp(gaussian / 3, −1, 1)`.
- *Stable:* `1 + (Quanta/100) × randomUniform(0, 1)`.

This factor multiplies the enchanting power and gets bounded by the mod's own selection logic. It must **not** multiply the final ValhallaMMO-enchanted item level, or the same power boost will be applied twice. The preview/actual selection must share a stable roll seed so the user sees consistent results.

**Arcana**: The 1.6.4 helper chooses a rarity weight table by Arcana thresholds (the `Arcana` class includes steps of 10) and attempts additional guaranteed enchantment selections at **0, 25 and 75 Arcana**, subject to eligible enchantment availability and incompatibilities; it also contains further probabilistic selection logic. Source `Arcana` weights are associated with the mod's `LegacyRarity` groups; they are not interchangeable with arbitrary custom YAML rarity names without a mapping.

## Current-system collision audit

The following methods were checked in the current branch and ValhallaMMO source:

1. **ValhallaMMO** `EnchantingSkill.onPrepareEnchant` at **HIGHEST** reads `ENCHANTING_QUALITY`, `ENCHANTING_FRACTION_QUALITY`, and `ENCHANTING_AMPLIFY_CHANCE`, amplifies offers, applies perk bonuses, and **caches the offered enchantments keyed by player, item material and bookshelf bonus**. `onEnchantItem` also runs **HIGHEST**, amplifies the final result and matches the cached offer. Changing offers after ValhallaMMO's listener would cause mismatches. Changing shelf stats without including them in the cache key could yield stale results.
2. **EnchantsSquared** `ValhallaTierEnchantingListener` uses ValhallaMMO's resolved effective enchanting quality **only** to decide the unlocked NG tier, then keeps the full original enchanted level (including levels above the Paper registry max). This calculation must remain the authority for tier access.
3. `NativeEnchantLootListener` already has per-acquisition chances and rarity/weight selection. Arcana should influence these *candidate weights*, rather than running an unrelated, second enchantment lottery.
4. `NativeEnchantingPerkBridge` and `NativeValhallaScalingBridge` already have skip/deduplication rules to avoid double-level amplification on tiers. Quanta and Eterna should stay upstream of those steps.
5. `NativeEnchantmentEvolution` treats vanilla Sharpness and Lesser/Advanced/Supreme variants as a **single exclusive family**. Arcana's multi-enchantment picks must run before this normalization and may never grant a second member of the same family.
6. `settings.maximum_bonus_enchantments: 1` currently limits perk-gated bonus enchants. Arcana needs an explicit, separate capped system **or** a clear combined limit so a high-Arcana table does not unintentionally bypass current balancing.
7. Paper's `PrepareItemEnchantEvent` exposes **three editable offers**; `EnchantItemEvent` exposes the final enchantment map. This supports a server-side approximation, but the original mod's enhanced client/table UI and truly expanded enchanting experience are **not** provided by those events alone.

## Recommended implementation design (not yet coded)

**Independent stats snapshot**

Build a single immutable calculation result from an enchanting table location and player:

```text
ApothicEnchantingStats {
  eterna: double
  maxEterna: double
  quanta: double
  arcana: double
  stable: boolean
  statSourceSignature: string
}
```

Source: enabled vanilla shelves, configurable Bukkit materials and optionally Oraxen custom blocks. Only scan the normal relevant enchanting layout and honor obstructed blocks; cap negative/positive contributions and cache cautiously. Separate the block-powered stats from the player's enchanting skill.

**One coordinated roll pipeline**

1. Read table statistics and base vanilla offer power. Eterna sets the maximum/base enchanting power (subject to caps), but **does not convert directly to NG quality**.
2. Apply Quanta to the power of a deterministic per-offer roll **once**. Stable shelves use the positive-only variant.
3. Build eligible enchant candidates, excluding items/tags, prohibited enchantments, mutually exclusive family members, and NG/perk-locked tiers.
4. Apply Arcana to eligible rarity weights and additional compatible picks, subject to configured cap. This step must not bypass `tier_families` or native enchantment registration tags.
5. Apply ValhallaMMO's existing perk/level scaling **once**, using the existing skill rating.
6. Substitute the eligible NG family tier, preserve the actual level, then run evolution normalization.
7. Reconcile preview, cost, final enchantment and charged XP consistently. Do not run extra random rerolls when Paper calls its preview event repeatedly.

**Implementation strategies**

- **Recommended first milestone:** compute Eterna/Quanta/Arcana from actual block layouts and expose a clear debug command or action-bar display; keep `modify_offers: false` and `modify_results: false`. Confirm expected stats without impacting ValhallaMMO.
- **Next:** Quanta + Arcana in a deterministic, compatible enchanting session pipeline, with strict maximum additional rolls and explicit tier exclusions.
- **Finally:** Eterna-controlled high-level offers and an optional dedicated server-side UI. The vanilla client still has three enchantment slots and does not automatically gain the NeoForge mod interface; custom screens, models and shelves require separate work.

## Proposed configuration skeleton (illustrative only)

```yaml
apothic_enchanting:
  enabled: false
  stat_display:
    enabled: true
  block_stats:
    minecraft:bookshelf:
      eterna: 1
      quanta: 0
      arcana: 0
  eterna:
    enabled: true
    max: 100
    modify_offers: false
  quanta:
    enabled: true
    allow_negative: true
    stability_requires_block: true
    modify_results: false
  arcana:
    enabled: true
    max_bonus_enchantments: 1
    allow_ng_tiers_without_perks: false
    modify_results: false
  valhallammo:
    preserve_level_scaling: true
    preserve_ng_unlocks: true
  evolution:
    enforce_exclusivity: true
```

The numbers under `block_stats` are an **illustrative proposal**, not an exact Apothic 1.6.4 conversion. No config parser or feature implementation exists yet.

## Test matrix before any release

| Test | Expected |
|---|---|
| Integration disabled | All existing ValhallaMMO/EnchantsSquared enchanting unchanged |
| Ordinary bookshelf table | Correct Eterna/Quanta/Arcana data and capped totals |
| Quanta unstable vs stable | Negative/positive fluctuations vs positive-only fluctuations |
| Arcana high | Higher rare weighting / extra compatible enchants, but no conflicting tiers |
| NG0 without perks | **Never** receives Advanced/Supreme through Arcana |
| NG5 with tier perks | Can roll allowed Supreme tier; levels match ValhallaMMO exactly once |
| Invalid previously double-enchanted item | Highest family stage retained; no duplicate effects |
| Same offer preview repeated | No flickering, mismatched hints, stale caching or double charged XP |
| Old enchant books and anvil | Evolution metadata and levels preserved |
| ValhallaEquipment Netherite | Names, attribute PDC, models and custom lore preserved |
| Server without Oraxen | Graceful fallback; custom bookshelf lookup optional |
| Purpur 26.3 real server | Runtime behavior matches unit tests across installed plugin load order |

**Conclusion:** Stat calculation itself has low-to-moderate integration risk. The **main complexity is synchronizing preview, XP cost and selection with ValhallaMMO's HIGHEST-priority enchanting listeners/cache**. Proceed with the stats-only phase first; do not attempt a direct NeoForge-to-Paper JAR integration.

Reference API: https://jd.papermc.io/paper/1.21.11/org/bukkit/event/enchantment/PrepareItemEnchantEvent.html and https://jd.papermc.io/paper/1.21.11/org/bukkit/event/enchantment/EnchantItemEvent.html.
