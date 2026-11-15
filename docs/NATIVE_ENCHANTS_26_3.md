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
| 1 | Lesser | I-X | 650 | 830 |
| 2 | Lesser | XI-XXX | 855 | 1083 |
| 3 | Advanced | I-X | 1090 | 1288 |
| 4 | Advanced | XI-XXX | 1295 | 1504 |
| 5 | Supreme | I-X | 1520 | 1871 |

NG stage is a hard ceiling; effective quality is computed from
\`ENCHANTING_QUALITY * (1 + ENCHANTING_FRACTION_QUALITY)\`.
The actual enchanted level in an eligible band is
\`min(cap, first_level + floor((quality - quality_at_first_level) / quality_per_level))\`.
If the player hasn't reached the newest band's quality threshold, the previous
eligible tier remains available.

All three tier families are tied to ValhallaMMO's existing
\`enchanting_perk_sharpnesses\`: Sharpness, Smite and Bane of Arthropods.
Their custom native levels are 30 for Lesser and Advanced, 10 for Supreme.

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
