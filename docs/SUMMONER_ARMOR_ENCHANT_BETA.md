# Summoner — native armor enchantment (BETA)

> ⚠️ **Experimental. Test on a separate Purpur 26.3 server.** Back up worlds,
> inventories, ValhallaMMO and EnchantsSquared YAML before testing. This source
> has been built by CI, but live mob AI/combat interactions still need testing.

## Effect

**Summoner** is a configurable **Epic** enchantment that can be applied to
helmets, chestplates, leggings and boots. It is a real Paper-native
`enchantssquared:summoner` enchantment, with default normal max **III**.

When a player wearing it is **damaged by another living entity** (including
arrows fired by an entity), the enchantment has a chance to summon loyal
mobs that defend the wearer. Its configured default summon types are Wolves
and Iron Golems. The summons automatically target the attacker. If the owner
hits another living target, their existing allies also attack that target.

The owner cannot damage an ally they have summoned, even with a melee
sweeping hit or projectile, and the ally cannot damage its owner. Summons
of the same owner cannot damage one another. Other players may attack
summons normally; this is not blanket invulnerability.

Summons are temporary, do not pick up dropped items, drop **no items or
experience** when killed, and are removed when their timer expires, when
their owner disconnects or dies, or when the plugin shuts down.

### Attribute mirroring

- **Minecraft/Paper (Bukkit) attributes:** copy the summoning player's
  effective attribute values including gear and enchants for every attribute
  supported by *both* player and selected mob. This includes armor, attack
  damage, movement speed, gravity, knockback resistance, max health, and
  other registered common attributes. Incompatible or unsupported mob
  attributes are skipped rather than forcing an invalid value.
- **ValhallaMMO combat stats:** the optional bridge calculates the owner's
  current resolved combat stats at summon time and provides the equivalent
  snapshot *delta* for the mob via ValhallaMMO's own stat collectors. The
  `copy_valhalla_stats` setting can be `none`, `combat` (default), or
  `all`. Snapshot values do not dynamically follow later owner buffs,
  and relational/target-dependent combat effects can vary by opponent.
- **Inventory safety:** actual armor, weapons and items are never cloned
  onto the summoned mob. The plugin copies effective stats, not items,
  avoiding player equipment duplication and unintended drops.

This is an **attribute snapshot**, not a complete clone of every potion
effect, enchant callback, player-only perk, skill tree effect or mob AI.

## Default YAML

In `plugins/EnchantsSquared/config_native_enchants.yml`:

```yaml
enchantments:
  summoner:
    enabled: true
    display_name: Summoner
    enchant_type: armor
    rarity: epic
    supported_items: ["#minecraft:enchantable/armor"]
    slots: [armor]
    max_level: 3
    weight: 2
    table_obtainable: false
    tradeable: false
    acquisition:
      enchanting: 0.012
      villager_trade: 0.008
      fishing: 0.005
      structure_chest: 0.025
      mob_drop: 0.007
    effect:
      handler: summon_ally
    summon:
      trigger: ON_DAMAGED
      mob_types: [WOLF, IRON_GOLEM]
      chance_at_level_1: 0.22
      chance_per_level: 0.06
      cooldown_seconds: 25
      initial_count: 1
      additional_count_per_level: 1
      max_per_trigger: 3
      max_active_per_owner: 6
      duration_seconds: 30
      duration_per_level: 5
      max_duration_seconds: 90
      follow_distance: 14.0
      dismiss_distance: 42.0
      help_owner_attack: true
      copy_bukkit_attributes: true
      copy_valhalla_stats: combat
      valhalla_stat_multiplier: 1.0
```

**Acquisition probabilities** are per eligible event, before rarity selection
and conflict/compatibility checks. Villager/fishing/structure/mob sources
deliver configured enchantment books, while enchanting can apply to armor.

### Scaling and limits

The actual Summoner enchantment levels across equipped armor are **added
together** (including levels beyond the normal enchant III maximum). If
multiple armor pieces have Summoner, their levels contribute to one proc
calculation. A single incoming hit never triggers more than one summon roll.

- Proc chance: `min(100%, chance_at_level_1 + (totalLevel-1) × chance_per_level)`
- Summons per proc: `min(max_per_trigger, initial_count + (totalLevel-1) × additional_count_per_level)`
- Duration: `min(max_duration_seconds, duration_seconds + (totalLevel-1) × duration_per_level)`
- Total living allies: at most `max_active_per_owner` per owner.
- Cooldown is counted once per player, not once per armor piece.
- Allies that idle more than `follow_distance` away teleport back to
  their owner. If they stray beyond `dismiss_distance`, they despawn.

These explicit per-player/per-proc ceilings prevent summon floods and
extreme lag; they are NOT a hidden cap on the enchantment's level.

## Testing on Purpur 26.3

1. **Back up** the test world, player data and YAML files.
2. Install the Summoner test JAR, stop the server, and restart completely
   (not /reload). Paper-native registry entries cannot hot-register.
3. Confirm `/esnative status` reports **53 enabled native enchantments** (54 declared, one disabled).
4. Equip an armor item with `/enchant @s enchantssquared:summoner 1`.
5. Have a test mob attack you until Summoner triggers; confirm it spawns a
   Wolf or Iron Golem that targets your attacker.
6. Try directly hitting, sweeping or shooting one of your own allies:
   your attacks should deal **zero damage** to it.
7. Compare the owner's maximum health, attack damage, armor and movement
   speed with the summoned mob's supported attributes. Check ValhallaMMO
   critical, elemental and resistance behavior via combat tests.
8. Change `copy_valhalla_stats` to `none` and restart: Bukkit stats should
   continue to mirror, but Valhalla combat snapshot should stop.
9. Test multiple armor pieces, higher-than-III enchanted levels, cooldown,
   maximum active mobs, expiration, logout, death, and plugin shutdown.
10. Check that no summoned mobs leave loot/XP and that in-game framerates
    remain acceptable under repeated damage.

If you use another custom-mob targeting/claims plugin, test its interactions
separately. This build does not bypass WorldGuard/PvP restrictions; combat
and spawning rules still need verification on your actual server.

## Project tracking

- Source development branch: `feature/summoner-armor-enchant-26-3`
- Beta release artifacts in the existing GitHub Releases **do not yet
  include Summoner**. Install the feature-branch CI test JAR for testing.
- The current EnchantsSquared/ValhallaEquipment interoperability remains
  optional; Summoner does not depend on ValhallaEquipment.
