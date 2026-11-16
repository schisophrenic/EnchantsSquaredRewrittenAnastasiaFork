# Apotheosis / Apothic Enchanting Integration — Working Branch

> **Status: Research/scaffolding only — NOT an implemented integration or a test JAR.**
>
> This branch was created from `feature/enchantment-evolution-no-stacking-26-3`.
> It preserves the existing native enchantments, Soul Capture, Summoner, YAML
> actions, tier evolution and ValhallaMMO compatibility. No production release
> or default-branch merge is part of this work.

## Reference supplied for analysis

User-provided binary: `ApothicEnchanting-1.21.1-1.6.4.jar` (not committed
to this repository).

Verified from the uploaded JAR's `META-INF/neoforge.mods.toml`:

| Property | Reference JAR |
|---|---|
| Mod ID | `apothic_enchanting` |
| Version | `1.6.4` |
| Platform | NeoForge (Minecraft 1.21.1+) |
| Required NeoForge | 21.1.187+ |
| Other required mods | `placebo` 9.9.0+, `apothic_attributes` 2.4.0+ |
| Data | 20 Apothic Enchanting-specific enchantment JSON definitions, 26 enchanting-stat block JSON definitions |

**IMPORTANT:** This NeoForge JAR **cannot be installed as a Paper/Purpur plugin**.
The plan is an independent Paper-compatible implementation of compatible
gameplay behavior and data-driven configuration, NOT runtime loading of
NeoForge mixins, registries, or GUI classes. Do not redistribute the uploaded
third-party JAR. Review licensing before reusing external code/assets.

This reference is the *enchanting module* of Apotheosis, not every feature
of the wider Apotheosis/Apothic Attributes mod ecosystem. Adventure loot,
gems and affixes are separate topics.

## Systems verified inside the uploaded reference

- Three central enchanting stats: **Eterna**, **Quanta**, **Arcana**.
  `en_us.json` describes Eterna as setting enchanting level maximum,
  Quanta as adding randomness, and Arcana as favoring rarer and additional
  enchantments. Separate **Max Eterna**, **Clues** and
  **Quantic Stability/Rectification** concepts also exist.
- `data/apothic_enchanting/enchanting_stats/*.json` describes block-driven
  stat contributions. For example, `basic_bookshelf.json` grants
  `eterna: 2`; `glowing_hellshelf.json` describes `maxEterna: 60`,
  `eterna: 5`, `quanta: 5`, and `arcana: 3.3333`.
  These are source data points, **not** values already supported in ES.
- Enchanting tables / extended table UI, enchanting info and Raven table,
  shelf blocks, filtering/treasure shelves, enchanted tomes, library,
  and infusion recipes are present in the reference, but use NeoForge
  gameplay/client APIs. Porting each would be a separate feature.
- There are **20** `data/apothic_enchanting/enchantment/*.json` definitions,
  including `berserkers_fury`, `boon_of_the_earth`, `chainsaw`,
  `chromatic`, `crescendo_of_bolts`, `endless_quiver`,
  `growth_serum`, `icy_thorns`, `infusion`,
  `knowledge_of_the_ages`, `life_mending`,
  `miners_fervor`, `natures_blessing`, `rebounding`,
  `reflective_defenses`, `scavenger`, `shield_bash`,
  `stable_footing`, `tempting`, and `worker_exploitation`.
  No claims are made that these mechanics currently run on Purpur.

## Planned independent implementation (no code port yet)

### Phase 1 — Configurable enchant-table statistics

Create `config_apothic_enchanting.yml` with:
- Feature flags: `enabled`, `stat_calculation`,
  `table_rolls`, `library`, `custom_enchantments`;
- Eterna, Quanta, Arcana, Max Eterna, Clues and Rectification;
- Block/material contributions, their placement/distance rules,
  diminishing/capping rules, and balancing;
- A transparent conversion from enchanting stats to roll quality;
- Per-player debug output and safe disable/fallback behavior.

Do not automatically double-apply **ValhallaMMO Enchanting Quality** or
overwrite NG tier/perk gates. Decide how Apothic statistics modify the
roll only after verifying how ValhallaMMO currently intercepts Paper's
`PrepareItemEnchantEvent` and `EnchantItemEvent`.

### Phase 2 — Compatible enchanting + NG/evolution rules

Integrate calculated table stats with the existing
`ValhallaTierEnchantingListener`, `NativeEnchantmentBootstrap`,
`NativeEnchantLootListener` and `NativeEnchantmentEvolution`.

In particular:
- **Vanilla Sharpness and Lesser/Advanced/Supreme Sharpness are one
  exclusive evolution family**, never simultaneous damage effects.
- Respect the configured `tier_families.<family>.enabled` switches.
- Preserve overlevelled VI+ enchant effects where already supported.
- Keep the user's `config_custom_enchants.yml` registry and probabilities.
- Provide a simple on/off switch with **no changes to normal vanilla
  or ValhallaMMO enchanting when disabled**.

### Phase 3 — Enchantment effects and loot options

Audit the 20 supplied enchantment JSON definitions individually against
existing ES enchants; **avoid duplicate mechanics**. Register only those
which can be implemented with modern Paper hooks, via additional
data-driven action handlers or small Java adapters.

Preserve original gameplay intent where practical, but do not represent
a Forge-only effect as implemented until it passes a live server test.
Add YAML rarity, allowed item tags, levels, incompatible/evolving
enchantments and acquisition chances for any genuinely new entries.

### Phase 4 — Optional library / UI and blocks

A simple Paper inventory-based **Enchantment Library** and shelf-stat
logic could be designed separately. NeoForge's bespoke screens, client
mixins, custom blocks and block entities **do not simply work on a
vanilla Minecraft client**. Any resource-pack models or server-side
interaction substitutes require their own design and testing.

## Compatibility / beta acceptance checklist

1. Purpur 26.3 and supported Paper versions boot with integration off/on.
2. Plugin functions without any Forge/NeoForge JARs or mod dependencies.
3. Vanilla and ValhallaMMO enchanting continue to work with feature off.
4. Eterna, Quanta and Arcana contributions and caps display predictably.
5. NG tier chances and level amplification do not double-stack or regress.
6. No item has ordinary and Supreme variants of the same enchant simultaneously.
7. Enchanted books, anvil evolution and Netherite upgrades retain custom data.
8. Configured enchant source chances and item restrictions are respected.
9. ValhallaEquipment item quality, PDC, weight class and model survive.
10. Live mob/potion/attribute effects verified on a backup-world staging server.

## References inside this repository

- `enchantssquared-core/src/main/resources/config_native_enchants.yml`
- `enchantssquared-core/src/main/resources/config_custom_enchants.yml`
- `enchantssquared-core/src/main/java/me/athlaeos/enchantssquared/nativeenchants/ValhallaTierEnchantingListener.java`
- `enchantssquared-core/src/main/java/me/athlaeos/enchantssquared/nativeenchants/NativeEnchantmentEvolution.java`
- `docs/ENCHANT_EVOLUTION_NO_STACKING_BETA.md`

**No functional Apotheosis support is claimed by creating this branch.**
