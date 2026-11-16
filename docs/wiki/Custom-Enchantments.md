# Custom Enchantments — EnchantsSquared Wiki

> [!WARNING]
> **Beta documentation (November 16, 2026).** This catalog describes the configuration and Java handlers in the [v2.7.0 beta branch](https://github.com/schisophrenic/EnchantsSquaredRewrittenAnastasiaFork/tree/release/beta-2026-11-16). In-game behavior on Purpur 26.3 requires testing. Disabled entries and placeholders are clearly marked.

The base beta source declares **67 original EnchantsSquared enchantments** and **54 built-in Paper-native definitions** (53 enabled). The optional easy-config file `config_custom_enchants.yml` adds **4 sample definitions** (1 enabled by default: Soul Capture) for a total of **54 enabled native enchantments** on this feature branch. Some native entries are compatibility versions of original effects; the two counts are **not** a count of distinct effects.

## How to read this page

- **Where to put it:** the item categories accepted by the source configuration or, where absent, its verified Java compatibility method. Actual items can be restricted by server settings.
- **Legacy rarity:** original EnchantsSquared uses a numeric selection **weight**, not Common/Rare/Epic labels. A smaller weight is generally less likely relative to other eligible enchants. `Treasure` means a special loot/trade/fishing route rather than an ordinary table roll.
- **Native rarity:** some new enchantments explicitly specify a named rarity; others provide only a numeric weight. We show the actual source value instead of inventing a label.
- **Levels:** the listed max is the normal/default level. ValhallaMMO can amplify item levels beyond that. Many handlers scale above the normal limit; particular abilities may have their own safety cap.
- **Status:** an original enchant may be disabled or blocked. A native registry entry with `handler: none` may depend on an older EnchantsSquared Java handler instead of having a separate native effect.

## Quick navigation

- [Original EnchantsSquared enchantments (67)](#original-enchantssquared-enchantments)
- [Native Lesser, Advanced and Supreme families (12)](#native-lesser-advanced-and-supreme-families)
- [Other native enchantments (18)](#other-native-enchantments)
- [Where to find native enchantments](#where-to-find-native-enchantments)
- [Notes on disabled and mirrored enchantments](#notes-on-disabled-and-mirrored-enchantments)

## Original EnchantsSquared enchantments

Descriptions below are derived from the source `config.yml` descriptions, not invented gameplay effects. These entries use legacy item enchantment IDs/PDC and existing Java effect handlers. Names reflect active display aliases where provided.

### Mining & gathering

| Enchantment (internal ID) | What it does | Equipment | Rarity | Max level | Status |
|---|---|---|---|---|---|
| **Excavation** (`excavation`) | Mines a 2x1 - + shaped - 3x3 area instead of 1x1x1. Sneak while mining to stop this effect. | Axes, Pickaxes, Shovels, Hoes | Weight 1 | 1 | Enabled (legacy) |
| **Smelter** (`sunforged`) | Smelts blocks mined | Axes, Pickaxes, Shovels, Hoes | Weight 20 | I (single-level) | Enabled (legacy) |
| **Kinship** (`kinship`) | If the pickaxe matches the material of the ore you're mining, some durability is restored. stone:0.05-0.1-0.15, iron:1-2-3, diamond:10-20-30, netherite:50-100-150 | Pickaxes | Weight 80 | 3 | Enabled (legacy) |
| **Telekinesis** (`telekinesis`) | Any drops from mined blocks are transferred directly to your inventory | Axes, Pickaxes, Shovels, Hoes | Weight 20 | I (single-level) | Enabled (legacy) |
| **Deforestation** (`tree_feller`) | Mining a tree, while sneaking, destroys the whole tree instantly | Axes | Weight 20 | I (single-level) | Enabled (legacy) |

### Durability

| Enchantment (internal ID) | What it does | Equipment | Rarity | Max level | Status |
|---|---|---|---|---|---|
| **Brittle** (`curse_durability`) | Curse enchant that causes items to break 2-3-4x quicker | All | Weight 5 | 3 | Enabled (legacy) |

### Passive gear, movement & curses

| Enchantment (internal ID) | What it does | Equipment | Rarity | Max level | Status |
|---|---|---|---|---|---|
| **Wings** (`flight`) | Allows the wearer temporary flight at the cost of durability for 5-10-15 seconds at a time. Recharge your flight by standing on the ground at 1-1.5-2x speed | Boots | Weight 10 | 3 | Disabled by default |
| **Rejuvenation** (`rejuvenation`) | Slowly regenerates durability | All | Weight 80 | 3 | Enabled (legacy) |
| **Lava Walker** (`lava_walker`) | Transforms lava under you within a radius of 0-1-2 blocks into magma, at the cost of durability. | Boots | Weight 20 | 2 | Enabled (legacy) |
| **Fleeting** (`speed_boost`) | Grants the wearer 20-30% increased movement speed while wearing | Boots | Weight 40 | 2 | Enabled (legacy) |
| **Leaping** (`jump_boost`) | Grants the wearer jump boost I-II while wearing | Boots | Weight 40 | 2 | Enabled (legacy) |
| **Nightvision** (`night_vision`) | Grants the player night vision while wearing | Helmets | Weight 20 | I (single-level) | Enabled (legacy) |
| **Gills** (`water_breathing`) | Grants the player water breathing while wearing | Helmets | Weight 80 | I (single-level) | Enabled (legacy) |
| **Quickened** (`haste`) | Grants the player haste I-II while holding | Swords, Pickaxes, Shovels, Axes, Hoes | Weight 10 | 2 | Enabled (legacy) |
| **Metabolism** (`metabolism`) | Slowly regenerates hunger points while moving | Helmets, Chestplates, Leggings, Boots | Weight 80 | 2 | Enabled (legacy) |
| **Barbarian** (`strength`) | Grants the player +3-6 attack damage while holding | Axes | Weight 40 | 2 | Enabled (legacy) |
| **Vigorous** (`vigorous`) | Adds 1 per level extra max health to the wearer | Helmets, Chestplates, Leggings, Boots | Weight 40 | 5 | Enabled (legacy) |
| **Blessing** (`lucky`) | Grants the player 1-2-3 luck while wearing | Chestplates | Weight 40 | 3 | Enabled (legacy) |
| **Burden** (`curse_heavy`) | Curse enchant that, while holding/wearing, slows the user down by 10-20% | All | Weight 5 | 2 | Enabled (legacy) |
| **Parasitic** (`curse_hunger`) | Curse enchant that slowly eats away at your hunger | Helmets, Chestplates, Leggings, Boots | Weight 5 | 2 | Enabled (legacy) |
| **Steady** (`knockback_protection`) | Reduces knockback taken by 20-40% | Leggings | Weight 40 | 2 | Enabled (legacy) |
| **Plating** (`plating`) | Adds 2 per level extra armor to the item | Elytra | Weight 40 | 3 | Enabled (legacy) |
| **Basalt Skin** (`lava_resistance`) | Grants the wearer fire resistance while wearing, but applies a significant slow while on fire | Chestplates | Weight 10 | I (single-level) | Enabled (legacy) |
| **Pact of the Faerie** (`scale`) | Makes you 25-50% smaller, also affecting your block- and attack reach | Chestplates, Elytras | Weight 20 | 2 | Enabled (legacy) |
| **Pact of the Jotunn** (`scale2`) | Increases your block- and attack reach by 25-50%, but also makes you 25-50% bigger | Chestplates | Weight 20 | 2 | Enabled (legacy) |
| **Strider** (`step_height`) | Allows you to step up full blocks | Leggings | Weight 40 | 1 | Enabled (legacy) |
| **Aspect of the Enderman** (`block_reach`) | Allows you to reach blocks 2-4-6 blocks further | Chestplates | Weight 20 | 3 | Enabled (legacy) |
| **Speed Master** (`speed_master`) | Grants the wielder and their mount 10-15-20% increased movement speed | Spears | Weight 40 | 3 | Enabled (legacy) |

### Combat: on hit

| Enchantment (internal ID) | What it does | Equipment | Rarity | Max level | Status |
|---|---|---|---|---|---|
| **Withering** (`withering`) | Has a 5-10-15% chance to inflict the entity you're damaging with Wither | Swords, Bows, Crossbows, Spears | Weight 120 | 3 | Enabled (legacy) |
| **Blunt** (`stunning`) | Has a 2-4% chance to inflict the entity you're damaging with a stun. | Swords, Bows, Crossbows, Axes, Pickaxes, Maces, Spears | Weight 20 | 2 | Enabled (legacy) |
| **Crippling** (`slowing`) | Has a 4-8% chance to inflict the entity you're damaging with Slowness | Swords, Bows, Crossbows, Spears | Weight 40 | 2 | Enabled (legacy) |
| **Concussion** (`nausea`) | Has a 3-5% chance to inflict the entity you're damaging with Nausea | Swords, Bows, Crossbows, Maces, Spears | Weight 80 | 2 | Enabled (legacy) |
| **Weakening** (`weakening`) | Has a 5-10% chance to inflict the entity you're damaging with Weakness | Swords, Bows, Crossbows, Maces, Spears | Weight 40 | 2 | Enabled (legacy) |
| **Poisoning** (`poisoning`) | Has a 5-10-15% chance to inflict the entity you're damaging with Poison | Swords, Bows, Crossbows, Spears | Weight 120 | 3 | Enabled (legacy) |
| **Blinding** (`blinding`) | Has a 2-4-6% chance to inflict the entity you're damaging with Blindness | Swords, Bows, Crossbows, Axes, Maces, Spears | Weight 40 | 3 | Enabled (legacy) |
| **Crushing** (`crushing`) | For each piece of heavy armor the enemy is wearing(gold, iron, netherite), you deal 2.5-3.75-5% more damage | Crossbows, Axes | Weight 20 | 3 | Enabled (legacy) |
| **Scattershot** (`arrow_aoe`) | Hitting an arrow on an entity deals 50-70-90% of the damage to all enemies in a 1.5-2-2.5 block radius around that initial entity | Crossbows, Bows | Weight 20 | 3 | Enabled (legacy) |
| **Toxic** (`toxic`) | Hitting an entity reduces its healing received by 40-60-80% for 4-5-6 seconds | Crossbows, Bows, Swords, Spears, Tridents | Weight 80 | 3 | Enabled (legacy) |
| **Harpooning** (`trident_sharpness`) | Tridents do 0.75-1.5-2.25-3.0-3.75 more damage | Trident | Weight 40 | 5 | Enabled (legacy) |
| **Perforating** (`perforating`) | For each piece of light armor the enemy is wearing(leather, chain, diamond), you deal 2.5-3.75-5% more damage | Crossbows, Axes | Weight 20 | 3 | Enabled (legacy) |
| **Splintering** (`shield_downer`) | Has a 20-40-60% chance to disable a shield for 2-3-4 seconds | Axes | Weight 40 | 3 | Enabled (legacy) |
| **Ionizing** (`lightning`) | Has a 5-10-15% chance to strike the enemy with lightning | Bows, Crossbows | Weight 40 | 3 | Enabled (legacy) |
| **Dragging** (`pulling`) | Hitting an arrow on an entity has a 20-40-60% chance to pull it closer. The pulling effect becomes stronger per level as well. | Crossbows, Bows | Weight 10 | 3 | Enabled (legacy) |
| **Pact of the Perfectionist** (`streak_damage`) | Reduces your damage by 20-40%, but grants you 5-10% increased damage per hit up to +50-100% damage. You lose this damage bonus when you take damage yourself. Damage penalty and bonus are halved with ranged hits | Swords, Axes, Bows, Crossbows, Tridents, Maces, Spears | Weight 20 | 2 | Enabled (legacy) |
| **Penetrating Edge** (`piercer`) | 50% of base weapon damage pierces any damage reduction. INCOMPATIBLE WITH ANY OTHER ENCHANT | Swords | Weight 1 | I (single-level) | Enabled (legacy) |
| **Cowardice** (`cowardice`) | You deal 20-30-40% more damage while at full health. | Boots | Weight 15 · Treasure | 3 | Enabled (legacy) |
| **Void Strike** (`void_touch`) | Applies "Void Touched" status effect that, over 3-4-5 seconds, increases damage up to 30-40-50% for the user | Swords, Axes, Bows, Crossbows, Tridents, Maces, Spears | Weight 15 · Treasure | 3 | Disabled; unregistered |
| **Pain Cycle** (`pain_cycle`) | Consumes 5-10% of your hp on each hit, after 5 hits your next one will increase base damage based on health consumed with half efficiency | Swords, Axes, Tridents | Weight 15 · Treasure | 3 | Enabled (legacy) |
| **Overclock** (`overclock`) | deals 50-100% more damage on hit, but mace goes on a 40-60 second cooldown afterwards | Maces | Weight 10 · Treasure | 2 | Disabled by default |
| **Impact Zone** (`mace_aoe`) | deals 20-40-60% of damage to entities in a 1.5 block radius | Maces | Weight 10 · Treasure | 2 | Disabled by default |

### Defense: when attacked

| Enchantment (internal ID) | What it does | Equipment | Rarity | Max level | Status |
|---|---|---|---|---|---|
| **Natural Blocking** (`shielding`) | Has a 20-40% chance to deflect projectiles that would normally hit you | Chestplates | Weight 40 | 3 | Enabled (legacy) |
| **Pact of the Berserker** (`tradeoff_berserk`) | You deal 20-35-50% more damage, but you also take 30-50-70% more damage | All | Weight 5 | 3 | Enabled (legacy) |

### Healing

| Enchantment (internal ID) | What it does | Equipment | Rarity | Max level | Status |
|---|---|---|---|---|---|
| **Strengthened Vitality** (`vitality`) | Increases the wearer/holder's healing by 5% per level, adding up all vitality you have on all pieces of armor up to 100%. | Helmets, Chestplates, Leggings, Boots | Weight 80 | 5 | Enabled (legacy) |

### Interaction & tools

| Enchantment (internal ID) | What it does | Equipment | Rarity | Max level | Status |
|---|---|---|---|---|---|
| **Illuminated** (`illuminated`) | Shift-Right click with a pickaxe on an empty surface to place a torch at the cost of 3 durability | Pickaxes | Weight 40 | I (single-level) | Enabled (legacy) |
| **Harvesting** (`auto_replant`) | Right click with a hoe on a fully grown crop to instantly harvest and replant it | Hoes | Weight 80 | I (single-level) | Enabled (legacy) |
| **Shockwave** (`shockwave`) | Right clicking on the upper surface of a block causes a shockwave that sends entities flying backwards in a radius of 3-4 blocks, with a strength of 0.5-0.7 | Swords, Axes, Maces | Weight 10 | 2 | Enabled (legacy) |

### On kill / death

| Enchantment (internal ID) | What it does | Equipment | Rarity | Max level | Status |
|---|---|---|---|---|---|
| **Sapping** (`bonus_exp`) | Killing an entity adds 1-1.5-2 more EXP | Swords, Axes | Weight 20 | 3 | Enabled (legacy) |
| **Life Steal** (`vampiric`) | Damaging non-passive entities heals you for 5%-10% of the damage dealt (up to 2 points max), and an additional 2-4 points when you kill it | Swords, Axes | Weight 80 | 2 | Enabled (legacy) |
| **Executioner** (`beheading`) | Killing an entity adds a 1-2% chance of dropping its head. Works with creepers, skeletons, players, etc. Axes are twice as potent | Swords, Axes | Weight 10 | 2 | Enabled (legacy) |
| **Soulbound** (`soulbound`) | Dying with a soulbound item prevents it from being dropped, instead, you respawn with it in your inventory | All | Weight 40 | 1 | Enabled (legacy) |
| **Will 'o' Wisp** (`exploding`) | Killing an entity causes a fiery explosion in a 2-3-4 block radius, dealing 2-4-6 immediate damage and 4-6-8 burn damage | Chestplates | Weight 20 | 3 | Enabled (legacy) |

### Potions & protection

| Enchantment (internal ID) | What it does | Equipment | Rarity | Max level | Status |
|---|---|---|---|---|---|
| **Neutralizer** (`chemical_shield`) | While blocking, splash potions and potion effect clouds don't affect you. Careful, as this also blocks beneficial effects. | Shields | Weight 40 | I (single-level) | Enabled (legacy) |
| **Potency** (`potion_potency_buff`) | Most potions you drink will have their duration reduced by 20-40%, but have their amplifiers increased by 50-100% | Chestplates, Elytra | Weight 10 | 2 | Enabled (legacy) |

### Fishing

| Enchantment (internal ID) | What it does | Equipment | Rarity | Max level | Status |
|---|---|---|---|---|---|
| **Grappling** (`grappling`) | Turns fishing rods into grappling hooks. The hook must be connected to a surface to be able to grapple to it | Fishing Rods | Weight 40 | 2 | Enabled (legacy) |

### Damage-triggered abilities

| Enchantment (internal ID) | What it does | Equipment | Rarity | Max level | Status |
|---|---|---|---|---|---|
| **Dragonhide** (`damage_reduction`) | Grants the wearer 10-15-20% damage reduction while wearing | Chestplates | Weight 1 · Treasure | 3 | Blocked by native settings |
| **Mass** (`ground_stomping`) | Immunity to fall damage. Landing after great falls deals damage in a radius. Damage and radius increase with distance, scaling 60-100% faster. While sneaking, you fall 2.5x faster. Effect has a 20-10 second cooldown | Boots, Elytras | Weight 5 | 2 | Enabled (legacy) |

### Ranged combat

| Enchantment (internal ID) | What it does | Equipment | Rarity | Max level | Status |
|---|---|---|---|---|---|
| **Rapid Shot** (`rapid_shot`) | Grants a 20-40-60% chance to shoot 2-3-4 arrows in quick succession. These arrows, except the last one, remove immunity frames from the hit target, but deal only 50-45-40% damage. | Crossbows, Bows | Weight 10 | 3 | Enabled (legacy) |

## Native Lesser, Advanced and Supreme families

These are **real Paper registry enchantments**. Their tier is selected by ValhallaMMO New Game+ (NG), unlocked perks and effective Enchanting quality; the original ValhallaMMO enchant level is preserved rather than capped at the normal maximum. **Turning off one `tier_families.<family>.enabled` switch disables tier substitution for that family while normal vanilla/ValhallaMMO enchanting remains usable.**

| Family / all three native names | What it does | Equipment | Normal max level | Rarity weights (L / A / S) | Default tier switch |
|---|---|---|---:|---|---|
| **Sharpness**: Lesser Sharpness (`lesser_sharpness`); Advanced Sharpness (`advanced_sharpness`); Supreme Sharpness (`supreme_sharpness`) | Adds melee damage against most targets (+0.5 × level + 0.5). | Weapons (Minecraft enchantable/weapon tag) | 5 | 10 / 5 / 1 | Enabled |
| **Smite**: Lesser Smite (`lesser_smite`); Advanced Smite (`advanced_smite`); Supreme Smite (`supreme_smite`) | Adds melee damage against undead targets (+2.5 × level). | Weapons (Minecraft enchantable/weapon tag) | 5 | 8 / 4 / 1 | Enabled |
| **Bane Of Arthropods**: Lesser Bane of Arthropods (`lesser_bane_of_arthropods`); Advanced Bane of Arthropods (`advanced_bane_of_arthropods`); Supreme Bane of Arthropods (`supreme_bane_of_arthropods`) | Adds damage against arthropods (+2.5 × level) and briefly slows them. | Weapons (Minecraft enchantable/weapon tag) | 5 | 8 / 4 / 1 | Enabled |
| **Power**: Lesser Power (`lesser_power`); Advanced Power (`advanced_power`); Supreme Power (`supreme_power`) | Adds bow projectile damage (+0.5 × level + 0.5). | Bows | 5 | 10 / 10 / 10 | Enabled |
| **Punch**: Lesser Punch (`lesser_punch`); Advanced Punch (`advanced_punch`); Supreme Punch (`supreme_punch`) | Increases arrow knockback. | Bows | 2 | 5 / 5 / 5 | Enabled |
| **Flame**: Lesser Flame (`lesser_flame`); Advanced Flame (`advanced_flame`); Supreme Flame (`supreme_flame`) | Fired arrows ignite targets. | Bows | 1 | 2 / 2 / 2 | Enabled |
| **Fire Aspect**: Lesser Fire Aspect (`lesser_fire_aspect`); Advanced Fire Aspect (`advanced_fire_aspect`); Supreme Fire Aspect (`supreme_fire_aspect`) | Direct melee hits set targets on fire. | Fire Aspect-compatible weapons | 2 | 5 / 5 / 5 | Enabled |
| **Protection**: Lesser Protection (`lesser_protection`); Advanced Protection (`advanced_protection`); Supreme Protection (`supreme_protection`) | Reduces general incoming damage, sharing vanilla's protection cap. | Armor | 4 | 10 / 10 / 10 | Enabled |
| **Fire Protection**: Lesser Fire Protection (`lesser_fire_protection`); Advanced Fire Protection (`advanced_fire_protection`); Supreme Fire Protection (`supreme_fire_protection`) | Reduces fire damage and burn duration. | Armor | 4 | 5 / 5 / 5 | Enabled |
| **Blast Protection**: Lesser Blast Protection (`lesser_blast_protection`); Advanced Blast Protection (`advanced_blast_protection`); Supreme Blast Protection (`supreme_blast_protection`) | Reduces explosion damage. | Armor | 4 | 5 / 5 / 5 | Enabled |
| **Projectile Protection**: Lesser Projectile Protection (`lesser_projectile_protection`); Advanced Projectile Protection (`advanced_projectile_protection`); Supreme Projectile Protection (`supreme_projectile_protection`) | Reduces projectile damage. | Armor | 4 | 5 / 5 / 5 | Enabled |
| **Magic Protection**: Lesser Magic Protection (`lesser_magic_protection`); Advanced Magic Protection (`advanced_magic_protection`); Supreme Magic Protection (`supreme_magic_protection`) | Reduces elemental damage (3% per enchanted level on equipped armor), capped at 85% combined enchantment-based mitigation across armor. | Armor | 4 | 5 / 5 / 5 | Enabled |

### Default NG progression

| NG level | Eligible tier | Effective enchanting quality threshold |
|---|---|---:|
| NG1 | Lesser | 650 |
| NG2 | Lesser | 855 |
| NG3 | Advanced | 1,090 |
| NG4 | Advanced | 1,295 |
| NG5 | Supreme | 1,520 |

*These are configured tier thresholds, not extra enchantment-level caps. Individual families also require their configured ValhallaMMO enchanting perks.*

## Other native enchantments

The following are configured as separate native registry entries (not all introduce a new effect). **Mirror** entries share the identity/purpose of the corresponding original enchantment, preserving a separate existing legacy effect in the codebase.

| Enchantment (native ID) | Effect | Equipment | Rarity | Normal max level | Status / implementation |
|---|---|---|---|---:|---|
| **Envenomed** (`enchantssquared:envenomed`) | Chance to apply Poison (60 ticks) and Wither (40 ticks) together on hit (15% configured chance). | Weapons (Minecraft enchantable/weapon tag) | Weight 5 | 3 | Enabled; native handler / attribute system |
| **Magic Protection** (`enchantssquared:magic_protection`) | Reduces elemental damage (3% per enchanted level on equipped armor), capped at 85% combined enchantment-based mitigation across armor. | Armor | Weight 5 | 4 | Enabled; native handler / attribute system |
| **Life Steal** (`enchantssquared:vampiric`) | Damaging non-passive entities heals you for 5%-10% of the damage dealt (up to 2 points max), and an additional 2-4 points when you kill it | Weapons (Minecraft enchantable/weapon tag) | rare | 2 | Enabled; legacy-effect bridge (test native-only use) |
| **Adept** (`enchantssquared:sapping`) | Killing an entity adds 1-1.5-2 more EXP | Weapons (Minecraft enchantable/weapon tag) | uncommon | 3 | Enabled; legacy-effect bridge (test native-only use) |
| **Strengthened Vitality** (`enchantssquared:vitality`) | Increases the wearer/holder's healing by 5% per level, adding up all vitality you have on all pieces of armor up to 100%. | Armor | rare | 5 | Enabled; legacy-effect bridge (test native-only use) |
| **Smelter** (`enchantssquared:sunforged`) | Smelts blocks mined | Mining tools | rare | 1 | Enabled; legacy-effect bridge (test native-only use) |
| **Lava Walker** (`enchantssquared:lava_walker`) | Transforms lava under you within a radius of 0-1-2 blocks into magma, at the cost of durability. | Boots | rare | 2 | Enabled; legacy-effect bridge (test native-only use) |
| **Dragging** (`enchantssquared:pulling`) | Hitting an arrow on an entity has a 20-40-60% chance to pull it closer. The pulling effect becomes stronger per level as well. | Bows | rare | 3 | Enabled; legacy-effect bridge (test native-only use) |
| **Penetrating Edge** (`enchantssquared:piercer`) | 50% of base weapon damage pierces any damage reduction. INCOMPATIBLE WITH ANY OTHER ENCHANT | Weapons (Minecraft enchantable/weapon tag) | legendary | 1 | Enabled; legacy-effect bridge (test native-only use) |
| **Natural Blocking** (`enchantssquared:shielding`) | Has a 20-40% chance to deflect projectiles that would normally hit you | Armor | rare | 3 | Enabled; legacy-effect bridge (test native-only use) |
| **Disorientating Blade** (`enchantssquared:disorientating_blade`) | Melee hits may apply both Blindness and Nausea (chance scales with level). | Weapons (Minecraft enchantable/weapon tag) | epic | 3 | Enabled; native handler / attribute system |
| **Evasion** (`enchantssquared:evasion`) | Chance to dodge an incoming hit; adds 4% per level across armor, capped at 50% in the native handler. | Armor | rare | 4 | Enabled; native handler / attribute system |
| **Splitshot** (`enchantssquared:splitshot`) | Adds spreading secondary arrows on a bow shot (up to 12), each at 35% of original arrow damage. | Bows | epic | 3 | Enabled; native handler / attribute system |
| **Strafe** (`enchantssquared:strafe`) | Boosts partially charged arrows to approximate faster bow draws (not true Forge draw-speed behavior). | Bows | rare | 4 | Enabled; native handler / attribute system |
| **Rapid Shot** (`enchantssquared:rapid_shot`) | Grants a 20-40-60% chance to shoot 2-3-4 arrows in quick succession. These arrows, except the last one, remove immunity frames from the hit target, but deal only 50-45-40% damage. | Bows | epic | 3 | Enabled; legacy-effect bridge (test native-only use) |
| **Graviton** (`enchantssquared:graviton`) | Adds +0.01 gravity per enchantment level while equipped; levels above the usual max still scale. | Armor | rare | 10 | Enabled; native handler / attribute system |
| **Summoner** (`enchantssquared:summoner`) | When the wearer takes a direct hit, has a configurable chance to summon loyal combat allies (default Wolves / Iron Golems). Summons inherit matching Bukkit attributes and a snapshot of ValhallaMMO combat stats; the owner and their allies cannot hurt each other. Cooldown, maximum active allies and lifespan are configurable. | Armor (helmet, chestplate, leggings, boots) | epic | 3 | Enabled; native summon AI and stat mirroring (beta) |
| **Soul Capture** (`enchantssquared:soul_capture`) | On a successful melee attack proc, removes an eligible mob without normal loot and grants its real mob-specific spawn egg containing a Paper EntitySnapshot. Retains supported mob NBT, attributes, equipment, status effects and names. Excludes protected plugin-owned mobs and configured bosses. | Weapons | legendary | 3 | Enabled via `config_custom_enchants.yml` (BETA) |
| **Physical Protection** (`enchantssquared:physical_protection`) | Placeholder for physical damage mitigation; disabled and no Java effect handler. | Armor | epic | 4 | **Disabled; placeholder (no effect handler)** |

## Where to find native enchantments

Native custom entries can specify independent acquisition probabilities for the enchantment table, villager trades, fishing, structure chests, and mob drops. **These are chance settings per eligible event, not guaranteed loot chances.** Selection also considers rarity and compatibility. Tier variants are not independently selected through these normal acquisition rolls; they use the ValhallaMMO NG tier-conversion system.

| Enchantment | Enchanting | Villager | Fishing | Structure chest | Mob drop |
|---|---:|---:|---:|---:|---:|
| Life Steal | 1.5% | 0.6% | 0.4% | 2.5% | 0.8% |
| Adept | 3% | 2% | 0.7% | 3.5% | 1.5% |
| Strengthened Vitality | 2.5% | 2% | 1% | 3.5% | 1% |
| Smelter | 3% | 1.8% | 0.7% | 4% | 1.2% |
| Lava Walker | 3% | 1.4% | 0.9% | 3.5% | 1% |
| Dragging | 2.4% | 1% | 0.6% | 3% | 1.2% |
| Penetrating Edge | 0.5% | 0.2% | 0.1% | 1% | 0.2% |
| Natural Blocking | 2.5% | 1% | 0.8% | 3% | 0.8% |
| Disorientating Blade | 1.5% | 1% | 0.5% | 2.5% | 0.8% |
| Evasion | 2% | 1.5% | 1% | 3.5% | 1.2% |
| Splitshot | 1.8% | 1% | 0.7% | 3.5% | 1% |
| Strafe | 2.5% | 2.5% | 0.8% | 2.5% | 1.2% |
| Rapid Shot | 0% | 0.8% | 0.5% | 2% | 1% |
| Graviton | 3% | 1.5% | 0.6% | 3% | 1% |
| Summoner | 1.2% | 0.8% | 0.5% | 2.5% | 0.7% |
| Soul Capture | 0.5% | 0.3% | 0.1% | 1.5% | 0.3% |
| Physical Protection | 0% | 0% | 0% | 0% | 0% |

*An absent acquisition entry means this extra random-acquisition system does not select that enchantment directly; it may have another registered source such as a perk roll or an original plugin's system.*

## Enchantment evolution (anti-stacking)

The beta evolution feature prevents two stages of the same enchantment from stacking on a single item. For example, **vanilla Sharpness → Lesser Sharpness → Advanced Sharpness → Supreme Sharpness** is one chain, not four independent damage bonuses. If a legacy test sword contains both Sharpness V and Supreme Sharpness III, the evolution normalizer retains **Supreme Sharpness V** and removes ordinary Sharpness. The same principle applies to Smite, Power, Protection, Magic Protection, Fire Aspect and the other configured tier families.

Administrators can create their own evolutions by setting `upgrade_of`, `upgrade_method: ANVIL`, `required_level`, `result_level`, and `inherit_level` under an enchantment in `config_custom_enchants.yml`. A disabled example named **Greater Soul Capture** demonstrates evolving Soul Capture III into Greater Soul Capture I with an enchanted book in an anvil. The enchantment registry automatically marks all stages of an evolution family as incompatible; already-illegal items are normalized on login, held-item change and periodically.

This feature is **beta**; test anvil outcomes and item metadata on Purpur 26.3 before deploying to a production server. See [Enchantment Evolution guide](../ENCHANT_EVOLUTION_NO_STACKING_BETA.md).

## Notes on disabled and mirrored enchantments

- **Damage Reduction** (`damage_reduction`) is blocked by the native `blocked_base_enchants` configuration even though its original legacy `config.yml` entry is enabled.
- **Physical Protection** (`enchantssquared:physical_protection`) is disabled and has no functional native handler. It is not a working enchantment in this test build.
- **Flight**, **Void Touch**, **Overclock**, and **Mace AOE** are disabled by the default legacy YAML. Void Touch's registration is also commented out in the source.
- **Envenomed** is separate from Poisoning and Withering; **Disorientating Blade** is separate from Blinding and Nausea; **Crushing** and **Perforating** remain separate heavy/light armor counters; **Evasion** is separate from Natural Blocking; **Splitshot**, **Strafe**, and **Rapid Shot** are distinct bow mechanics.
- Some native entries intentionally mirror original enchants (Life Steal/Vampiric, Adept/Sapping, Strengthened Vitality/Vitality, Smelter/Sunforged, Lava Walker, Dragging/Pulling, Penetrating Edge/Piercer, Natural Blocking/Shielding, Rapid Shot). The source still has legacy handlers; test native-only books before advertising both representations as independently operational.
- **Magic Protection** currently caps enchantment-derived elemental damage reduction at 85% across armor. Independent armor, potion and ValhallaMMO resistance systems may separately modify final damage.
- **Summoner** (`enchantssquared:summoner`) is an Epic armor enchant that reacts to damage by a living attacker. Level scaling uses the sum of levels on worn armor, including overleveled items. Summons cannot damage their owner or other summons belonging to the same owner; owner attacks and projectiles cannot hurt them. Vanilla attributes are copied only where the mob entity supports them; ValhallaMMO combat stats are resolved once at spawn, not dynamically mirrored. Its full live-server behavior is still in beta testing.
- **Graviton** uses the real Bukkit gravity attribute. `AIR_DRAG` is a custom plugin movement-effect key, not a vanilla attribute.
- The older So Many Enchantments mod's other enchantments are **not all implemented** in this beta. This catalog lists only the entries supported by the current source.

## Create your own custom enchantments (no Java edits)

The new [`config_custom_enchants.yml`](../../enchantssquared-core/src/main/resources/config_custom_enchants.yml) lets server owners register their own native enchants by configuring item tags, rarity, levels, loot chances, and triggers/actions. Supported actions include potion effects, healing, extra damage, ignition, knockback, experience, messages and **capturing mobs to real spawn eggs**. See [Easy Custom Enchantments & Soul Capture BETA](../EASY_CUSTOM_ENCHANTS_AND_SOUL_CAPTURE_BETA.md) for a complete example and testing limitations.

## Technical sources

- [Original enchantment definitions and descriptions](https://github.com/schisophrenic/EnchantsSquaredRewrittenAnastasiaFork/blob/release/beta-2026-11-16/enchantssquared-core/src/main/resources/config.yml)
- [Native registry definitions, rarity and acquisition settings](https://github.com/schisophrenic/EnchantsSquaredRewrittenAnastasiaFork/blob/release/beta-2026-11-16/enchantssquared-core/src/main/resources/config_native_enchants.yml)
- [Original enchantment registration and internal IDs](https://github.com/schisophrenic/EnchantsSquaredRewrittenAnastasiaFork/blob/release/beta-2026-11-16/enchantssquared-core/src/main/java/me/athlaeos/enchantssquared/managers/CustomEnchantManager.java)
- [Native tier selection by NG/quality](https://github.com/schisophrenic/EnchantsSquaredRewrittenAnastasiaFork/blob/release/beta-2026-11-16/enchantssquared-core/src/main/java/me/athlaeos/enchantssquared/nativeenchants/ValhallaTierEnchantingListener.java)
- [Native special-effect handlers](https://github.com/schisophrenic/EnchantsSquaredRewrittenAnastasiaFork/blob/release/beta-2026-11-16/enchantssquared-core/src/main/java/me/athlaeos/enchantssquared/nativeenchants/NativeSpecialEnchantListener.java)
- [Native armor elemental protection](https://github.com/schisophrenic/EnchantsSquaredRewrittenAnastasiaFork/blob/release/beta-2026-11-16/enchantssquared-core/src/main/java/me/athlaeos/enchantssquared/nativeenchants/NativeProtectionEnchantListener.java)

*Last source audit: November 16, 2026 · Beta-only documentation.*
