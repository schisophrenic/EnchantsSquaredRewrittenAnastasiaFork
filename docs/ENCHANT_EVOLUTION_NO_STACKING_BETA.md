# Enchantment Evolution + No Double-Stacking (BETA)

> **Experimental feature for the EnchantsSquared fork on Purpur 26.3.** Test
> on a staging server and back up items/world/player inventories before using.
> This is not in the previous public beta release and requires a FULL server
> restart (Paper registry exclusivity cannot be changed on /reload).

## The issue it fixes

An old item can sometimes receive both **vanilla Sharpness** and
**Supreme Sharpness**, which should be versions of the *same* enchantment,
not two independent damage bonuses. The current tiered native Java registry
has `exclusive_set` but older or plugin-generated items may bypass it.
This feature adds an evolution graph and a cleanup pass for existing items.

### Built-in evolution families

All configured `tier_families` (Sharpness, Smite, Power, Punch,
Protection, Fire Aspect, Magic Protection, etc.) now form mutually-exclusive
chains:

```text
minecraft:sharpness
    -> enchantssquared:lesser_sharpness
    -> enchantssquared:advanced_sharpness
    -> enchantssquared:supreme_sharpness
```

- **Only the strongest stage is kept on the same item.**
- If an existing test item has Sharpness V and Supreme Sharpness III,
  it is normalized to **Supreme Sharpness V**, not both.
- Item level is preserved from the highest present version, including
  ValhallaMMO VI+, without capping down to the vanilla maximum.
- All other enchantments (e.g. Unbreaking, Looting) stay unchanged unless
  they are explicitly incompatible by Minecraft's own enchantment rules.
- The native registry enforces incompatibility during enchanting/anvil
  where possible, and runtime normalizing repairs illegal item stacks.
- This works with item enchantments and enchanted-book stored enchantments.
- NG tier family `enabled: false` still stops the native tier replacement
  on new enchanting rolls; existing overlevelled items keep normal ValhallaMMO
  scaling and no longer stack duplicate versions.
- A compatible higher-tier enchanted book can promote an existing earlier
  tier on a weapon in the anvil, preserving the actual enchant level.
  This does not create books or bypass their loot/perk sources.

## Create custom evolution chains with YAML

Edit `plugins/EnchantsSquared/config_custom_enchants.yml`:

```yaml
enchants:
  greater_soul_capture:
    enabled: true
    name: Greater Soul Capture
    type: weapon
    rarity: legendary
    max_level: 5
    upgrade_of: enchantssquared:soul_capture
    upgrade_method: ANVIL
    required_level: 3
    result_level: 1
    inherit_level: false
    acquisition:
      enchanting: 0.0
      villager_trade: 0.001
      fishing: 0.0
      structure_chest: 0.01
      mob_drop: 0.001
    triggers:
      ON_HIT:
        chance: 0.05
        chance_per_level: 0.02
        actions:
          - type: CAPTURE_MOB
            blocked_mobs: [WITHER, ENDER_DRAGON, WARDEN]
```

This example is included but **disabled by default**. Enable it, fully
restart the server, and get a weapon with **Soul Capture III** and an
enchanted book carrying **Greater Soul Capture**.

Place Soul Capture III weapon on the left and Greater Soul Capture book on
the right of an anvil. The output gets **Greater Soul Capture I** and loses
the old Soul Capture enchantment, keeping the item's own equipment metadata.

### Evolution configuration

| YAML field | Function |
|---|---|
| `upgrade_of` | Full namespaced ID of the older enchant |
| `upgrade_method` | Currently `ANVIL` |
| `required_level` | Minimum ancestor level on the left-hand item |
| `result_level` | Initial new enchant level |
| `inherit_level` | `false` uses `result_level`; `true` keeps the previous item level if higher |
| `conflicts` | Additional unrelated incompatible IDs (as before) |

The `upgrade_of` relationship automatically adds Paper registry
mutual exclusions for **all members of the evolution family** (including
other intermediate tiers), so server owners do not need to manually list
every earlier version in `conflicts`.

Enchantments from different families are not merged. Upgrades with
`upgrade_method` other than `ANVIL` are currently not implemented.

## Compatibility and cleanup

- The EnchantsSquared native bonus-enchantment and ValhallaMMO NG tier
  enchanting systems remain active. After their selection, the evolution
  handler normalizes duplicate variants in the enchanting result.
- Anvil and Smithing previews are normalized too. Illegal versions on
  items in player inventories are repaired after login, when a player
  switches held slots, and every five seconds.
- Enchantment levels are not decreased when a weaker stage is removed.
- Existing lore, display names, persistent custom data, and custom
  ValhallaEquipment model/property attributes are preserved through
  `ItemMeta` rather than replacing the entire item.
- Since older items may have been manually made with multiple bonuses,
  players should take a backup before running the beta normalizer.

## Purpur staging test

1. Back up playerdata, inventories, world, and both enchant YAML files.
2. Replace the old EnchantsSquared test JAR and fully restart the server.
3. Create an intentionally combined Sharpness V + Supreme Sharpness III
   test sword. Verify normalization leaves Supreme Sharpness V alone.
4. Test an ordinary Sharpness-only sword: should not evolve until granted
   an eligible Supreme/Lesser/Advanced tier through normal progression.
5. Test NG enchantment selection with and without each tier family enabled.
6. Test anvil combinations and Smithing/Netherite upgrades; confirm quality
   and ValhallaEquipment PDC/models survive.
7. Enable the Greater Soul Capture example, restart, obtain its enchanted
   book, and test Soul Capture III -> Greater Soul Capture I.
8. Verify right-handed item, offhand and enchanted books are normalized,
   without deleting unrelated enchantments.
9. Check other plugins' enchant handling and console for errors.

**Status:** Maven compilation via GitHub Actions does not prove the live
server's anvil/menu behavior. Do not deploy this to production yet.
