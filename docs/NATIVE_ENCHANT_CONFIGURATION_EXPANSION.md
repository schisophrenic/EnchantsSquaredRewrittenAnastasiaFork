# Native enchant family switches, attribute effects and natural loot

**Status:** Test implementation on `feature/native-enchants-valhalla-26-3` for Purpur 26.3. Java compilation has passed. **Not yet validated on a running game server.** Keep the draft PR unmerged and back up worlds, plugins and YAML configs before testing.

## Disable a single tiered family without disabling vanilla ValhallaMMO scaling

Set `tier_families.protection.enabled: false` in `plugins/EnchantsSquared/config_native_enchants.yml`:

```yaml
tier_families:
  protection:
    enabled: false
    base: minecraft:protection
    required_perks: [enchanting_perk_protections]
    # Keep the existing tiers below; the plugin skips their selection
```

On the next full server restart, Protection rolls remain ordinary vanilla Protection and use ValhallaMMO's unaltered scaling formula, including amplified levels VI+. Only the **automatic conversion** to Lesser/Advanced/Supreme Protection is disabled. The dormant tier entries remain registered so old custom-enchanted equipment continues to work. Each of the **12 tier families** has an independent switch (sharpness, smite, bane_of_arthropods, power, punch, flame, fire_aspect, protection, fire_protection, blast_protection, projectile_protection, magic_protection).

## Approved name mapping

The **original internal enchantment IDs must not be renamed** because their PDC item data and existing handlers rely on them. These eight effects are now also registered as native enchants under their canonical old IDs:

| Internal ID | Display name | Effect source |
|---|---|---|
| `vampiric` | **Life Steal** | Original EnchantsSquared Vampiric (heal on hits and kills) |
| `sapping` | **Adept** | Original XP bonus on kills |
| `vitality` | **Strengthened Vitality** | Original healing-received improvement; **NOT max health**, which is the separate Vigorous enchant |
| `sunforged` | **Smelter** | Original auto-smelting block drops |
| `lava_walker` | **Lava Walker** | Original lava-to-magma traversal |
| `pulling` | **Dragging** | Original arrow-hit pulling |
| `piercer` | **Penetrating Edge** | Original piercing damage |
| `shielding` | **Natural Blocking** | Original projectile deflection |

- **Poisoning and Withering** remain separate. **Envenomed** is already native and applies both effects.
- **Blinding and Nausea** remain separate. **Disorientating Blade** is new native enchantment applying both debuffs.
- **Crushing and Perforating** stay separate counters to heavy vs light armor.
- **Evasion** is a separate dodge chance on equipped armor.
- **Splitshot** adds side projectiles on bow release. **Rapid Shot** retains its original EnchantsSquared sequential-arrow implementation and now also has a native registry ID. **Strafe** currently improves early-release arrow velocity/damage as an approximation; Forge's use-duration tick logic is not directly exposed by ordinary Bukkit bow events.
- **Damage Reduction** is disabled by `blocked_base_enchants: [damage_reduction]`. **Physical Protection** has a native YAML definition, default `enabled: false`, and no effect implementation yet.

## Write a custom attribute enchant (example: Graviton)

Add under `enchantments:` in `config_native_enchants.yml`:

```yaml
  graviton:
    enabled: true
    display_name: Graviton
    enchant_type: armor
    rarity: rare
    supported_items: ["#minecraft:enchantable/armor"]
    slots: [armor]
    max_level: 10
    weight: 3
    table_obtainable: false
    tradeable: false

    # 0.01 at level 1, 0.02 at level 2, 0.05 at level 5,
    # 0.11 at level 11, despite normal registered max X.
    attribute:
      GRAVITY: 0.01
    scale_per_level: 0.01
    attribute_operation: ADD_NUMBER

    acquisition:
      enchanting: 0.03
      villager_trade: 0.015
      fishing: 0.006
      structure_chest: 0.03
      mob_drop: 0.01
    effect:
      handler: none
```

**Formula for every attribute:**

`effective_value = base + (actual_item_enchant_level - 1) * scale_per_level`

It is NOT clamped to `max_level`. Normal registry maximum controls ordinary vanilla enchanting, but ValhallaMMO's quality amplification and enchanted item levels beyond that continue scaling. Minecraft's item enchantment data itself may still impose an encoding/engine limit; this does not promise physically unbounded integers.

`attribute` accepts multiple attributes and `scale_per_level` can be either a scalar or a per-attribute map. For example:

```yaml
    attribute:
      GRAVITY: -0.01                  # Bukkit GENERIC_GRAVITY
      MOVEMENT_SPEED: 0.002            # Bukkit GENERIC_MOVEMENT_SPEED
      "VALHALLA:MAGIC_RESISTANCE": 0.02
      AIR_DRAG: 0.01                   # Plugin pseudo-attribute, NOT a vanilla one
    scale_per_level:
      GRAVITY: -0.01
      MOVEMENT_SPEED: 0.002
      "VALHALLA:MAGIC_RESISTANCE": 0.01
      AIR_DRAG: 0.005
```

- Vanilla attributes are temporary **entity attribute modifiers**, refreshed on equipped gear rather than overwriting the item's own attribute modifiers.
- `VALHALLA:NAME` uses an installed AccumulativeStatManager stat collector. Offense/defense sources use their correct owner; other plugins' stat modifiers remain intact.
- `AIR_DRAG` is a custom, airborne horizontal slowdown while equipped, with a safety limit of 25% per tick. Real Bukkit attributes don't include an Air Drag enum.
- The Java engine supports **EQUIPPED** for generic attribute effects. Other events use dedicated Java enchantment effect listeners; arbitrary triggers are not silently treated as implemented.

## Per-enchant rarity and acquisition probabilities

`rarity`: `common`, `uncommon`, `rare`, `epic`, `legendary` (a selection weight).

`acquisition` keys (probabilities from `0` to `1`):
- `enchanting`: extra enchant roll on compatible items at the enchantment table.
- `villager_trade`: native enchanted book when a villager acquires a book trade.
- `fishing`: replace a caught item with a native enchanted book.
- `structure_chest`: append a native enchanted book to newly generated natural Minecraft chest loot (not player-placed regular chests).
- `mob_drop`: append a native enchanted book when a non-player entity dies.

The engine checks per-enchantment chance, then chooses one candidate with rarity weighting. Setting any chance to `0` disables **that source** for that enchant. The source-specific book level is ordinarily in the registry's default range; ValhallaMMO amplification from normal enchanting remains separate. **Special tiered enchantments do not bypass NG requirements** via these extra loot sources unless deliberately configured.

Existing original EnchantsSquared PDC enchants not yet native-registered continue using their original enchanting/trading/fishing systems; this patch migrates the eight approved aliases and Rapid Shot, not all 67 base enchants.

## Low-quality equipment available without Smithing perks

```yaml
loot_equipment:
  enabled: true
  quality_fraction_min: 0.55
  quality_fraction_max: 0.80
  item_pool: [IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS,
              DIAMOND_CHESTPLATE, IRON_SWORD, IRON_PICKAXE,
              DIAMOND_PICKAXE, BOW, SHIELD, FISHING_ROD]
  attribute_pool: [CRIT_CHANCE, CRIT_DAMAGE, STUN_CHANCE, LIFE_STEAL,
                   FIRE_RESISTANCE, MAGIC_RESISTANCE, BLEED_CHANCE,
                   DURABILITY_MULTIPLIER, ENTITY_DROPS]
  max_random_attributes: 2
  attribute_min: 0.01
  attribute_max: 0.05
  acquisition:
    structure_chest: 0.10
    fishing: 0.025
    mob_drop: 0.007
```

This requires ValhallaMMO but **does not check the Smithing skill or perks**. The loot generator uses `SmithingItemPropertyManager.setNeutralQuality/setQuality` with approved neutral-quality material baselines, plus `ItemAttributesRegistry.getCopy/addDefaultStat` for real ValhallaMMO custom item attributes. Default quality is between **55% and 80% of neutral quality**. This does not invoke ValhallaEquipment's crafting-only weight-class/texture roll API, which is a separate integration to implement before claiming fully generated random class/texture gear in natural loot.

Sources: natural structure chests, fishing and non-player mob drops. Other existing loot remains untouched. Loot is an additional roll; default probabilities are conservative and can be tuned.

## Full attribute and trigger reference inside the YAML

On startup, `NativeEnchantConfigReference` automatically appends a **comment-only, exhaustive reference section to the end** of `plugins/EnchantsSquared/config_native_enchants.yml`, collected from:

1. Installed Bukkit/Paper `Attribute.values()`.
2. Installed ValhallaMMO `AccumulativeStatManager.getSources().keySet()`.
3. Installed ValhallaMMO `ItemAttributesRegistry.getRegisteredAttributes().keySet()`.

It also lists every supported field, rarity, item slot, attribute operation, acquisition source, implemented event trigger, the custom AIR_DRAG effect and example YAML syntax. It refreshes on each plugin start to stay compatible with server upgrades.

## Staging-server checklist

1. Back up server world, existing plugins, ValhallaMMO item metadata and all YAML files.
2. Install the new experimental JAR. **Fully restart** due to native registry changes.
3. `/esnative status` should find **52 active native definitions** (53 total, one disabled).
4. Set `tier_families.protection.enabled: false`, restart, and verify Protection only uses vanilla ValhallaMMO scaling while Lesser Sharpness still works.
5. Check displayed names, old enchanted items, and the eight migrated native IDs. `/enchant @s enchantssquared:graviton 1` on a chestplate tests the new attribute system.
6. Test Gravity, an explicit `VALHALLA:...` stat, and a manual overlevel enchanted Graviton item; confirm effects scale beyond base X.
7. Test original Poisoning/Withering and Envenomed independently, Blinding/Nausea versus Disorientating Blade, Evasion dodge, Splitshot, Strafe and the original Rapid Shot sequential shots.
8. Generate fresh natural structure loot, fish and defeat mobs; check native books and low-quality ValhallaEquipment-compatible gear without Smithing perks.
9. Confirm Magic Protection enchantment-based damage mitigation stays at or below **85%** across all armor. Other independent ValhallaMMO resistances can reduce total damage further, and are **not** globally capped by this enchantment.
10. Confirm expected normal enchant I–V maxima, overlevels VI+, and no duplicate event effects. Report startup logs if the registry fails.

**Caution:** This is compiled test code, **not a proven runtime deployment**. The complete 1.12.2 Forge mod has not been ported in this batch. Preserve a known-good JAR for rollback.
