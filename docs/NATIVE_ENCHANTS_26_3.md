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
- none: register-only; add a Java handler for a special ability

These formulae are starter implementations, not copied from the 1.12.2 mod.
New complex effects can be implemented as additional Java handlers.

## Example per-player Sharpness progression

1. No unlock: minecraft:sharpness is offered normally.
2. ENCHANTING_QUALITY >= 80 and enchanting_perk_sharpnesses: lesser_sharpness.
3. ENCHANTING_QUALITY >= 150 plus enchanting_perk_ng1 and sharpnesses perk: advanced_sharpness.
4. ENCHANTING_QUALITY >= 250 plus enchanting_perk_ng2 and sharpnesses perk: supreme_sharpness.

Thresholds are illustrative and are **configurable**. The plugin evaluates
ValhallaMMO's effective enchanting quality using ENCHANTING_QUALITY and
ENCHANTING_FRACTION_QUALITY, just as its own enchantment listener does.
On each roll it removes the base/lesser tiers from the resulting enchantment
map and inserts **only the highest eligible replacement**.

This per-player substitution does not delete minecraft:sharpness from the global
registry. Other players can still roll it, and previously enchanted items retain it.

Tier enchantments are excluded from the *normal* enchanting table tag: they
cannot independently appear before their required perks are unlocked.
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
