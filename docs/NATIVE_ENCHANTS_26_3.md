# YAML-defined native enchantments, Purpur 26.3

This is a **draft implementation**. No successful full Maven compile or live server
test has been performed. Use a staging world and backup inventories.

## Goal

Every entry under config_native_enchants.yml -> enchantments becomes a **real
Minecraft registry enchantment**, with a stable ID like
enchantssquared:lesser_sharpness.

The Java server plugin handles gameplay behavior, rather than copying effects
into the datapack and risking duplicate damage or conflicting game logic.

## What to edit

File: plugins/EnchantsSquared/config_native_enchants.yml

- enchantments.<key>.display_name: the name shown on an enchanted item
- supported_items: vanilla item IDs or Minecraft item tags
- max_level, weight, min/max costs, anvil_cost, slots: vanilla enchant metadata
- table_obtainable: true for independent enchantments, false for gated tiers
- exclusive_set: other enchantment IDs that must not combine
- effect.handler: Java implementation name; effect.base/per_level: its parameters
- tier_families.<family>.base: vanilla or custom enchant to replace
- tiers[].enchantment: namespaced replacement enchant
- tiers[].minimum_enchanting_quality: effective ValhallaMMO quality threshold
- tiers[].required_perks: ALL named perks must be unlocked

Currently supplied Java effect handlers:

- melee_flat_bonus: attack event damage += base + (level - 1) * per_level
- melee_multiplier: attack event damage *= 1 + base + (level - 1) * per_level
- on_hit_potion: chance to apply configurable potion effects on melee hit
- none: register-only; add a Java handler for a special ability

These formulae are starter implementations, not copied from the 1.12.2 mod.
New complex effects can be implemented as additional Java handlers.

## Quality calculation from the user's actual ValhallaMMO configuration

The server's enchanting_progression.yml gives +1 enchanting skill per level.
Non-NG core quality perks add 50 + 50 + 80 + 100 + 150 = +430.
Further NG-level quality perks contribute:
- NG1 at level 125: +88 +110 = 753 total effective quality
- NG2 at level 150: +115 +92 = 985
- NG3 at level 175: +180 = 1190
- NG4 at level 200: +195 = 1410
- NG5 at level 200: +225 +150 = 1785

Pre-NG1 at level 100: 100 +430 = 530.

**Assumptions:** This assumes every relevant quality perk was bought,
and no fractional quality multiplier, other equipment bonuses or Sovereign
specialization (+50) were applied. Values are calibrated progress benchmarks,
not minimum or mandatory quality for that NG stage.

The uploaded enchanting.yml uses Sharpness/Smite/Bane scaling of
`0.02 * quality - 2, ADD_ON_DEFAULT, 1,45`. These are the *vanilla*
scaling parameters; custom enchantment levels instead follow the YAML NG
quality progression, independent of original vanilla Sharpness level.

## Current NG1–NG5 tier rules

| NG stage | Tier | Allowed native levels | Quality at first | Quality per +1 | Quality reaching cap |
|---|---|---|---:|---:|---:|
| 1 | Lesser | I–X | 550 | 20 | 730 |
| 2 | Lesser | XI–XXX | 755 | 12 | 983 |
| 3 | Advanced | I–X | 990 | 22 | 1188 |
| 4 | Advanced | XI–XXX | 1195 | 11 | 1404 |
| 5 | Supreme | I–X | 1420 | 39 | 1771 |

The player's **NG level is a hard cap** regardless of enchanting quality.
All three tier families (Sharpness, Smite, Bane of Arthropods) require
`enchanting_perk_sharpnesses`, which already exists in the provided
ValhallaMMO progression. NG checks use
`enchanting_perk_ng1` through `enchanting_perk_ng5` from that same file.
The plugin uses `ENCHANTING_QUALITY * (1 + ENCHANTING_FRACTION_QUALITY)`
like ValhallaMMO. It uses the highest unlocked band whose minimum quality
has been reached. If no band qualifies, it preserves the original vanilla
enchantment. Every final roll removes the base and lower tiers before
adding the chosen one.

## Perk-only enchantments for non-tier mechanics

The example `envenomed` is a **separate** custom enchantment:
- It is a real registered enchantment with its own Java potion-effect handler.
- It has no independent enchanting-table entry.
- Enchanting can grant it as a bonus if the player has
  `enchanting_perk_poison`, has reached NG2 and quality >= 850.
- Base chance: 10% per qualifying enchanting session; enchantment levels scale
  +1 every 150 quality after 850, capped at the enchantment's maximum.
- The Java handler currently applies poison and wither with configurable
  durations and chance (starter values, not a verified 1.12.2 damage formula).

The user-provided ValhallaMMO config already contains useful perk IDs:
`enchanting_perk_sharpnesses`, `enchanting_perk_protections`,
`enchanting_perk_offensive`, `enchanting_perk_defensive`,
`enchanting_perk_utility`, `enchanting_perk_fire`,
`enchanting_perk_freezing`, `enchanting_perk_poison`,
`enchanting_perk_arcane`, `enchanting_perk_sovereign`,
and `enchanting_perk_spellforged`.

**No ValhallaMMO progression edit is required for these existing perk IDs.**
When introducing a genuinely new perk with a new ID, it must be added to
ValhallaMMO's progression and localization and tested for unlock behavior.

This per-player substitution does not delete minecraft:sharpness from the global
registry. Other players can still roll it, and previously enchanted items retain it.

Tier enchantments are excluded from the *normal* enchanting table tag: they
cannot independently appear before their required perks are unlocked.
Independent perk-only enchantments are similarly granted through configured
Java bonus rolls, not globally available table-tag entries.
The vanilla enchanting table tag is extended with replace=false. Existing core
EnchantsSquared enchants may still appear normally if their config marks
them as obtainable.

## Installation and lifecycle

1. Build an updated plugin JAR from this branch after addressing compilation.
2. Remove/disable any **old manually installed** native-datapack-26.3 pack from
   the previous draft. Do not install BOTH static and generated packs.
3. Start the server with the new plugin. It writes the generated datapack to
   the first loaded world's datapacks/EnchantsSquaredNativeAuto folder.
4. **Restart the entire server again**, then use /datapack list to verify the
   pack loaded. Newly added registry IDs are not hot-reloaded by the plugin.
5. Test /enchant @s enchantssquared:lesser_sharpness 1 on a supported item.
6. Grant/remove ValhallaMMO perks and modify quality on a TEST PLAYER to verify
   each enchanting-table preview and final result, including noneligible tiers.
7. Test books, anvil combining, grindstones, old PDC items, vanilla compatibility,
   scaled levels and collision with ValhallaEquipment item properties.

After YAML changes, run **/esnative generate** as an administrator. Check
the console for generation errors, then FULLY RESTART the server once for
the registry to load the new files. If you do not run the command, the plugin
generates the files on next startup, and you must restart again to activate
the registry changes.

## Scope and unresolved items

- The 1.12.2 So Many Enchantments JAR includes 130 named enchantment entries.
  This branch ports the tiered Sharpness **example**, not all of those effects.
- Only simple melee effect handlers exist; all specialized 1.12.2 mechanics
  need original Java implementations and balancing for ValhallaMMO.
- The old /es listing, admin command and custom-loot paths are built around
  existing Java CustomEnchant objects. YAML-only enchantments are native and
  do not yet appear in every legacy /es interface.
- The vanilla enchantment tag is global, whereas individual perk checks happen
  in server events. Other plugins that explicitly grant enchants, loot tables,
  and administrator commands may bypass perk eligibility unless separately gated.
- Custom-item compatibility needs a test when Oraxen/ValhallaEquipment stores
  equipment using a vanilla base item that differs from the listed item tags.
- Existing native entries generated for old EnchantsSquared classes don't yet
  translate all custom conflict rules or the original max_table_level.
- /reload is not a supported replacement for a full registry-reload test.
- This branch has not been validated against the exact Purpur 26.3 + premium
  ValhallaMMO dependencies or on a live test server.

## Source basis

SoManyEnchantments-1.0.9-1.12.2.jar provided by the user: internal
assets/somanyenchantments/lang/en_us.lang and mcmod.info provide enchantment
names and metadata. The archive also includes an MIT license. No Java bytecode
or audio/art assets from the mod have been copied into this implementation.
