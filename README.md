> [!CAUTION]
> **BETA TEST BUILD — NOT FOR PRODUCTION.** Back up your world, player data, inventories, custom items and plugin YAML before testing. Use Purpur 26.3 on a separate staging server. Integration is compile-checked but **not verified in live gameplay**. See [beta testing release notes](BETA_RELEASE_NOTES.md) before installing.

EnchantsSquared is a plugin adding over 50 enchantments. These may be obtained in the enchantment table similar to how regular enchantments are obtained. 
This plugin is indended to make these enchantments behave as closely to vanilla enchantments as possible, and everything is very customizable

This version of enchants squared has been forked and is being worked on for Anastasia RPG minecraft server

## Complete custom enchantment catalog (beta)

The [**Custom Enchantments catalog**](https://github.com/schisophrenic/EnchantsSquaredRewrittenAnastasiaFork/blob/release/beta-2026-11-16/docs/wiki/Custom-Enchantments.md) lists every currently declared original and native enchantment, what each one does, eligible equipment, rarity/weight, normal level limits, and whether it is enabled. It also explains the Lesser/Advanced/Supreme NG tiers and random acquisition rates. This is beta documentation; some registered enchantments are placeholders or share older handlers.


## Easy configurable enchantments + Soul Capture (BETA)

Admins can create **real Paper-native enchantments without modifying Java** by
adding an entry in `plugins/EnchantsSquared/config_custom_enchants.yml`.
The format supports custom names, rarity, eligible equipment, maximum levels,
per-source acquisition probabilities, seven event triggers and eight configurable
actions (including a native mob spawn egg capture action).

See the [easy-enchant YAML and Soul Capture guide](docs/EASY_CUSTOM_ENCHANTS_AND_SOUL_CAPTURE_BETA.md)
and the [player catalog](docs/wiki/Custom-Enchantments.md).

**BETA:** Capturing a mob creates a real spawn egg preserving its entity
snapshot; special plugin-owned/boss mobs are excluded. Captures do not
produce normal mob death drops, preventing equipment duplication.
Compilation passed but live Purpur 26.3 testing remains necessary.
