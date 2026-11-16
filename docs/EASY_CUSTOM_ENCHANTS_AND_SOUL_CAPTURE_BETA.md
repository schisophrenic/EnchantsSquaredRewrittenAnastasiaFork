# Easy Custom Enchantments + Soul Capture — BETA

> ⚠️ **EXPERIMENTAL — test on a backup world using Purpur 26.3.**
>
> These changes are not in the existing beta release. Use only the test JAR
> from the \`feature/configurable-enchants-mob-capture-26-3\` branch.
> Do not reload the server; Paper enchantment registration requires a restart.

## Create a new enchantment without modifying Java

Create or edit **\`plugins/EnchantsSquared/config_custom_enchants.yml\`**.
The file is created with examples automatically on first launch.

Every entry under \`enchants:\` becomes a real enchantment
(\`enchantssquared:<key>\`) at Paper's native registry bootstrap.
The server handles the native maximum level, tags/eligible equipment, anvil
cost, rarity weighting, villager/fishing/chest/mob-drop acquisition and
combat/action logic from this single file. Existing \`config_native_enchants.yml\`
enchantments and ValhallaMMO tiers are unchanged.

Small example: a weapon enchant that slows enemies.

\`\`\`yaml
enchants:
  frostbite:
    enabled: true
    name: Frostbite
    type: weapon
    rarity: rare
    max_level: 5
    acquisition:
      enchanting: 0.03
      villager_trade: 0.01
      fishing: 0.005
      structure_chest: 0.03
      mob_drop: 0.01
    triggers:
      ON_HIT:
        chance: 0.15
        chance_per_level: 0.04
        cooldown_seconds: 0
        actions:
          - type: POTION
            potion: SLOWNESS
            target: ENEMY
            duration_ticks: 60
            duration_per_level: 20
            amplifier: 0
\`\`\`

You can edit an existing YAML enchant's numbers without touching source
code. Adding/removing/retyping any enchant requires a **full restart**.

### Enchantment fields

| Field | Meaning |
|---|---|
| \`enabled\` | Register or disable this enchant |
| \`name\` | Visible name |
| \`type\` | \`weapon\`, \`armor\`, \`boots\`, \`bow\`, \`mining\`, \`fishing\` |
| \`items\` | Optional list of Paper item tags/IDs overriding the type default |
| \`slots\` | Optional \`mainhand\` / \`armor\` active slots |
| \`rarity\` | \`common\`, \`uncommon\`, \`rare\`, \`epic\`, \`legendary\` |
| \`weight\` | Optional numeric rarity-selection override |
| \`max_level\` | Native maximum (overleveled items still scale) |
| \`min_cost_base\`, \`min_cost_per_level\` | Minimum enchantment level cost |
| \`max_cost_base\`, \`max_cost_per_level\` | Maximum enchantment level cost |
| \`anvil_cost\` | Anvil cost |
| \`conflicts\` | List of namespaced incompatible enchantment IDs |
| \`acquisition\` | Per-event probabilities from 0 to 1 |
| \`attribute\` | Optional passive Bukkit/ValhallaMMO attributes |
| \`scale_per_level\` | Passive attribute increment for each enchant level |
| \`attribute_operation\` | \`ADD_NUMBER\` / \`ADD_SCALAR\` / \`MULTIPLY_SCALAR_1\` |
| \`triggers\` | Event-based action definitions |

#### Trigger list

| Trigger | When it runs |
|---|---|
| \`ON_HIT\` | Player hits a living entity with an enchanted held item |
| \`ON_DAMAGED\` | Player with the enchant is hit by an entity |
| \`ON_KILL\` | Player kills a living entity |
| \`ON_MINE\` | Player successfully breaks a block |
| \`ON_SHOOT\` | Player fires a bow or crossbow |
| \`ON_FISH\` | Player successfully catches an item/fish |
| \`ON_INTERACT\` | Player right-clicks using enchanted held equipment |

Use \`chance\`, \`chance_per_level\`, \`cooldown_seconds\`,
\`target_types\`, \`blocked_targets\`, \`allow_projectiles\`, or
\`require_target\` under a trigger. For **armor enchants**, actual
enchantment levels across worn armor pieces are summed. Chance
is \`clamp(chance + (level-1)*chance_per_level, 0, 1)\`.

#### Action list

| Action | Parameters |
|---|---|
| \`CAPTURE_MOB\` | Capture a valid mob into a native spawn egg |
| \`POTION\` | \`potion\`, \`target\`, \`duration_ticks\`, \`duration_per_level\`, \`amplifier\` |
| \`HEAL\` | \`target: SELF\` or \`ENEMY\`, \`amount\`, \`per_level\` |
| \`DAMAGE\` | \`amount\`, \`per_level\` on original damage event |
| \`IGNITE\` | \`target\`, \`ticks\`, \`ticks_per_level\` |
| \`KNOCKBACK\` | \`target\`, \`strength\`, \`per_level\` |
| \`EXPERIENCE\` | \`amount\`, \`per_level\` |
| \`MESSAGE\` | \`message\` with \`{level}\`, \`{target}\`, \`{enchant}\` |

Each action may also specify its own \`chance\` between 0 and 1.
Passives are handled by the existing attribute enchant engine:
\`attribute: { GRAVITY: 0.01 }\`, \`scale_per_level: 0.01\`, etc.
The existing Summoner armor enchant supports custom mob lists and separate
summon-specific settings; it is not automatically recreated by generic actions.

## Soul Capture — Legendary

**ID:** \`enchantssquared:soul_capture\`

**Equipment:** weapons with Minecraft's standard enchantable/weapon tag

**Normal maximum:** III (extra levels continue to increase the chance)

**Trigger:** direct player hits on eligible mobs

| Level | Capture chance per hit |
|---:|---:|
| I | 2% |
| II | 3% |
| III | 4% |
| V (overleveled) | 6% |

**Result:** the mob is captured and removed from the world, and the player
receives a **real mob spawn egg**. The original mob does **not** drop its
normal loot or experience; doing so could duplicate equipment retained
in the egg. The mob is not damaged to zero health: it is removed as part
of a capture transaction. This intentionally differs from a normal kill.

### NBT and attribute preservation

This uses Paper's native **\`Entity.createSnapshot()\` +
\`SpawnEggMeta.setSpawnedEntity(snapshot)\`**. The snapshot is stored in
the egg's Minecraft entity data component. Using the egg spawns the stored
entity state without a separate restoration handler, including supported:
- custom name, health and remaining effects
- native entity attributes and attribute modifiers
- inventories/equipment and entity-specific NBT
- persistent plugin data (PDC) included in the entity snapshot

As with SF_MobCapturer, not every *external plugin's runtime-only state*
can be reconstructed. For instance, a custom mob managed by MythicMobs
may have state outside entity NBT. For safety, Mob Capture refuses
MythicMobs, EliteMobs, NPCs and Summoner allies; if its ownership APIs
cannot be checked reliably while the plugin is installed, capture is blocked.

### Mob eligibility controls

\`\`\`yaml
- type: CAPTURE_MOB
  allowed_mobs: []
  blocked_mobs: [WITHER, ENDER_DRAGON, WARDEN]
  capture_named_mobs: true
  capture_tamed_mobs: false
  capture_invulnerable_mobs: false
  max_snapshot_chars: 131072
\`\`\`

The mob must have a normal matching \`*_SPAWN_EGG\` item.
Mobs in vehicles or carrying passengers are rejected to avoid duplication.
The original damage event must not already be cancelled by other plugins.
This enchant does not bypass region protections.

#### Available default acquisition probabilities

| Enchanting | Villager | Fishing | Structure chests | Mob drops |
|---:|---:|---:|---:|---:|
| 0.5% | 0.3% | 0.1% | 1.5% | 0.3% |

These chances are applied per eligible event and are affected by rarity
weights, compatible items and other configured acquisition rules.

## Compatibility and testing

- Requires Paper's modern native enchant registration and entity snapshots.
- Designed for Purpur 26.3; Paper servers with the same compatible APIs may
  work, but have not been tested.
- ValhallaEquipment loot and enchants can coexist: the egg stores entity
  state, not equipment reward drops. Captured custom mobs remain protected.
- Player, boss, NPC and plugin-owned custom-mob capture are intentionally
  restricted by default.
- Capture is performed in a single ordinary main-thread event callback.
  **Live behavior must still be tested**, especially config bootstrap
  and attribute preservation on placing a captured spawn egg.

### Test checklist

1. Back up worlds, player inventories and both enchant YAML files.
2. Install the new beta JAR and restart fully.
3. Verify \`/esnative status\`, which should find 54 enabled native
   enchantments when Soul Capture is enabled and examples are disabled.
4. Hold an eligible enchanted weapon, e.g. \`/enchant @s enchantssquared:soul_capture 1\`.
5. Hit a normal Zombie several times; wait until Soul Capture triggers.
6. Check the inventory contains a **Zombie Spawn Egg**, not a head, with
   the captured state and name. Spawn it and verify any custom attributes,
   equipment and potion effects.
7. Test tamed mobs, named mobs, blocked mob types, full inventory,
   region-protected mobs and mythic/elite plugin-managed mobs.
8. Try a new custom enchant by enabling the \`frostbite\` example.
   Restart, apply it, and verify Slowness scales with actual level.
9. Check the plugin startup console for YAML validation errors.
10. Keep it on a staging server until no item loss or unexpected duplication
    occurs in real play.

### Source acknowledgment

The ideas for capture preservation and protected-mob checks were compared
with [SF_MobCapturer](https://github.com/wickidcow/SF_MobCapturer),
but this plugin's capture code is independently implemented using Paper's
native EntitySnapshot / SpawnEggMeta APIs and has no Slimefun dependency.
