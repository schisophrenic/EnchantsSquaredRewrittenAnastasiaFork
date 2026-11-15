# EnchantsSquared direct Java registry enchantments — Purpur 26.3

## Corrected architecture

Minecraft 26.3 supports registering new **real** enchantments directly from
a **Paper plugin bootstrapper** using the registry compose lifecycle event.
The fork now has:
- a \`paper-plugin.yml\` descriptor with a bootstrapper and optional dependency metadata
- \`NativeEnchantmentBootstrap.java\` registering actual enchantment keys
  using \`RegistryEvents.ENCHANTMENT.compose()\`
- Java registration of enchanting-table/trading tags via
  \`LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT)\`
- \`config_native_enchants.yml\` holding the native enchantment definitions
- \`ValhallaTierEnchantingListener.java\` for per-player NG/quality/perk upgrades
- \`NativeYamlEffectListener.java\` for the sample melee and status-effect handlers

**No external or generated datapack** is used. The obsolete generator and
\`native-datapack-26.3\` folder were removed from the branch.

## Installing and testing

This is a **compiled staging build**, not yet tested live on Purpur 26.3.

1. Back up world and player inventories and remove the old plugin JAR.
2. Install the new \`EnchantsSquared_JavaRegistry_NG_Test.jar\` from GitHub Actions.
3. Delete or disable any old \`EnchantsSquaredNativeAuto\` datapack that may
   remain from the *previous* test attempt.
4. Fully restart the server once. On first startup, the plugin bootstrapper
   copies \`config_native_enchants.yml\` into \`plugins/EnchantsSquared/\`
   and immediately registers the enchantments from it.
5. Run \`/esnative status\` or \`/esnative list\` as OP to verify the registry.
   You should see **10 configured native enchantments** (9 tiers, Envenomed).
6. With a test sword in hand, use:
   \`/enchant @s enchantssquared:lesser_sharpness 1\`
   Then verify its lore/registry effect and repeat with Advanced/Supreme levels.
7. Test a normal enchanting table using the real ValhallaMMO perks and quality.
   Confirm the preview and final roll agree, and verify vanilla Sharpness
   remains available without the offensive perk.
8. Test ancient PDC EnchantsSquared items, EnchantsSquared /es commands, anvils,
   grinding, item upgrades and ValhallaEquipment interactions. Existing
   EnchantsSquared enchanting rolls are deliberately preserved.

After changing or adding any \`enchantments:\` registry YAML entry, FULLY
RESTART the server. The registry freezes before onEnable; neither /reload nor
/esnative can register additional IDs in a running server.

## Quality thresholds (the user's +100 adjustment)

| Current enchanting NG | Tier | Allowed levels | Starting quality | Quality reaching level cap |
|---|---|---|---|---|
| 1 | Lesser | I-III | 650 | 830 |
| 2 | Lesser | IV-V | 855 | 1083 |
| 3 | Advanced | I-III | 1090 | 1288 |
| 4 | Advanced | IV-V | 1295 | 1504 |
| 5 | Supreme | I-V | 1520 | 1872 |

NG stage is a hard ceiling; effective quality is computed from
\`ENCHANTING_QUALITY * (1 + ENCHANTING_FRACTION_QUALITY)\`.
The actual enchanted level in an eligible band is
\`min(cap, first_level + floor((quality - quality_at_first_level) / quality_per_level))\`.
If the player hasn't reached the newest band's quality threshold, the previous
eligible tier remains available.

All three tier families are tied to ValhallaMMO's existing
\`enchanting_perk_sharpnesses\`: Sharpness, Smite and Bane of Arthropods.
Every tier has the same maximum level as its vanilla parent: **V**. The game registry enforces this limit, even for previously-installed YAML that still says 30/50.

## Native ValhallaMMO scaling and original effects

The EnchantsSquared scaling bridge calls ValhallaMMO's
`EnchantingItemPropertyManager.registerScaling(...)` for all nine native
tier keys. This is necessary because ValhallaMMO normally loads only a
hardcoded list of vanilla keys from its enchanting.yml.
Additional per-enchantment formulas can be configured under
`config_native_enchanting.yml` or as quoted `scalings` keys in
`plugins/ValhallaMMO/skills/enchanting.yml`.

Example:
```yaml
scalings:
  sharpness: (2/100) * %rating% - 2,ADD_ON_DEFAULT,1,5
  smite: (2/100) * %rating% - 2,ADD_ON_DEFAULT,1,5
  bane_of_arthropods: (2/100) * %rating% - 2,ADD_ON_DEFAULT,1,5
  'enchantssquared:lesser_sharpness': (1/100) * %rating%,ADD_ON_DEFAULT,0,5
```

The bridge forcibly bounds the registered custom scalings to V,
while the enchanting-table listener caps even post-perk vanilla
and native enchantment rolls at V. Family-specific + generic offensive
Valhalla perks translate to a moderate effective-quality boost:
default `tier_perk_quality_per_bonus: 5.0`. This avoids granting
the highest enchantment level instantly upon NG unlock.

The tier effects now follow the Java Edition vanilla family:
- Sharpness: +0.5 * level + 0.5 melee damage to any target.
- Smite: +2.5 * level melee damage against mobs sensitive to Smite.
- Bane of Arthropods: +2.5 * level melee damage to arthropods, with
  Slowness IV for a vanilla-like random duration.
There is no arbitrary extra damage multiplier for higher named tiers yet.

Both old server config formats are handled at runtime: bootstrap enforces
native V regardless of the saved registry YAML; the listener converts
obsolete NG 1-10 / 11-30 brackets into the new compact V brackets.
The plugin does not overwrite server YAML, so admins can replace
their own files when convenient.

The perk groups also include defensive, utility, elemental fire/freezing/poison,
Arcane, Sovereign and Spellforged. Envenomed is a Java status-effect test
enchantment gated by \`enchanting_perk_poison\`, NG2 and enchanting Q850.

## How YAML defines new native enchants

Under \`enchantments:\` add a unique key with display_name, supported_items,
slots, weight, max_level, min/max costs, anvil_cost and effect.handler.

\`supported_items\` currently accepts **one vanilla #minecraft:item_tag**
(e.g. \`#minecraft:enchantable/weapon\`) OR a list of direct item IDs.
Don't mix multiple tags/direct items in the same list for this test build.
The \`table_obtainable\` and \`tradeable\` flags are read at bootstrap
and tag membership is set in Java without any data files.

Native registry metadata does not by itself create gameplay effects.
Special mod effects still need Java handlers, registered in the effect listener.

## Known limitations

- Native Java registry bootstrap is new; server runtime has not yet been tested.
- The plugin now uses Paper plugin loading instead of legacy-only Bukkit
  metadata, and /es plus /esnative use Paper command registration.
- The old enchantment PDC system is left intact. A full migration of old PDC
  enchantments to native entries is a separate task.
- Original So Many Enchantments effects are NOT fully ported: only three
  offensive tier families and an Envenomed prototype are present.
- The sample Smite/Bane effects currently use generic bonus damage rather than
  their intended mob-specific behavior; rebalance before production.
- The tier substitution and perk-only bonus mechanics run in enchanting
  events; other ways of granting enchantments may bypass the player perk gates.
- Do not install this on a live server before checking the startup log,
  existing plugin dependencies, command behavior and registry results.
