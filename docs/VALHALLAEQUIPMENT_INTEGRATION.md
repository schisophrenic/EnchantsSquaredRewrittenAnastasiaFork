# ValhallaEquipment integration — natural gear + enchantments (Purpur 26.3)

This document reflects the actual sources in
`schisophrenic/valhallammo-Equipment` at commit `850ac882f3ac52bc7002fd517c954f5dc0b9b21f`
and this EnchantsSquared branch.

## What is integrated

When the installed `ValhallaEquipment` plugin exports
`public Object generateNaturalLoot(Object preparedItem)`, EnchantsSquared
calls it with an already ValhallaMMO-quality-tagged Bukkit ItemStack.
The equipment plugin runs its **normal** attribute, armor weight/class,
name, stored property, and configured model/texture generation pipeline,
without requiring a Smithing player/perk. The item is returned as a standard
ItemStack, so existing native enchantments and enchantment event handlers
can work on compatible materials as usual.

The natural item source and quality are still controlled by EnchantsSquared;
the ValhallaEquipment integration must not create a higher-quality item.
By default, quality is 55–80% of each material's NQ.

The external Equipment config has independent limits because poor-quality
items otherwise roll 0 normal property slots:

```yaml
# plugins/ValhallaEquipment/config.yml
external-loot:
  enabled: true
  minimum-property-slots: 1
  max-property-slots: 2
  strength-multiplier: 0.35
```

EnchantsSquared config:

```yaml
# plugins/EnchantsSquared/config_native_enchants.yml
loot_equipment:
  enabled: true
  use_valhalla_equipment: true
  native_enchant_chance: 0.20
  quality_fraction_min: 0.55
  quality_fraction_max: 0.80
  acquisition:
    structure_chest: 0.10
    fishing: 0.025
    mob_drop: 0.007
```

After the base equipment is generated, an additional EnchantsSquared
native enchantment may be added to compatible gear; a candidate must pass
its OWN acquisition source chance, be compatible with the underlying vanilla
item material, and not conflict with enchantments already present.

## Load order and fallbacks

- Both plugins need ValhallaMMO Premium running.
- EnchantsSquared uses `getPlugin("ValhallaEquipment")` and calls the
  external API reflectively at loot-generation time, so it has **no hard
  dependency** on the Equipment JAR.
- The reflection bridge is invoked only for loot, not during crafting
  enchantments or per-tick combat logic.
- If Equipment is absent/disabled, or its API returns null, the previous
  fallback generator adds ValhallaMMO attributes directly.
- An item that has already been generated with ValhallaEquipment's own
  PDC identity is never rerolled.
- The plugin's native enchantment logic still reads real ItemStack
  enchantment levels (including ValhallaMMO-overleveled VI+ levels).

## Important limitations

- This is **not** a complete ValhallaEquipment crafting or recipe importer:
  the natural loot pool supplies selected base vanilla materials. Custom
  scaled spear, halberd, javelin and other named item variants require a
  custom source/provider and are not instantiated by this generator.
- Enchantments whose Paper `supported_items` tags don't match the
  underlying item material cannot automatically be applied just because
  the item has a ValhallaEquipment texture/model.
- ValhallaTrinkets accessory slots are not part of native armor/hand
  enchant attribute scanning yet.
- The result uses Equipment's configured model rolling. Invalid or missing
  Oraxen model IDs can leave the original model intact; review that plugin's
  warnings.
- Existing manually crafted Equipment with NG5 Smithing perks remains
  unchanged by this integration.
- Purpur 26.3 in-game runtime tests are still needed.

## Suggested test

1. Back up both plugins' YAML files and the test world's inventories.
2. Install the two newly compiled test JARs (do not leave duplicate/older
   versions in `plugins/`). Keep the existing Premium ValhallaMMO plugin.
3. Fully restart the server; no /reload.
4. Check console for `ValhallaEquipment` enabled and EnchantsSquared's
   native-enchantment registry initialization.
5. Spawn or open newly generated structure chests repeatedly, and test
   fishing and ordinary mob drops. Gear should sometimes appear below NQ
   but with one or two genuine Equipment-rolled properties and optional
   configured weight/model/class. No Smithing skill is required.
6. Use `/valeq inspect` while holding generated gear to verify its saved
   property record. Inspect quality with ValhallaMMO.
7. Test that compatible native enchants can appear on generated gear and
   retain their levels, and that high-level effects continue scaling.
8. Test Smithing upgrades and anvil use on enchanted generated gear. The
   model, generated property PDC and enchantments must survive.
9. Disable `loot_equipment.use_valhalla_equipment`: newly dropped gear
   should revert to EnchantsSquared's old Valhalla-only generator.
10. Disable `external-loot.enabled` in Equipment and verify fallback again.

### Integration contract

```java
// Called by EnchantsSquared using reflection:
// getMethod("generateNaturalLoot", Object.class).invoke(equipment, prepared)
public Object generateNaturalLoot(Object preparedItem)
```

The runtime input/output is a Bukkit ItemStack. The deliberately Object-
typed method is to keep the ValhallaEquipment repository's lightweight
compile-stubs build self-contained; do not treat it as a general serialization
or custom item generator API.
