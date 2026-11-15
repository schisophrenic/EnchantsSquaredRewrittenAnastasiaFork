# EnchantsSquared registry datapack (Minecraft / Purpur 26.3)

**PREVIEW — not yet runtime tested.** The datapack registers 68 enabled enchantments as real `enchantssquared:*` registry entries. Java plugin effects remain in EnchantsSquared; the registry JSON intentionally leaves `effects` empty so no effect is applied twice.

1. Back up your world and player inventories.
2. Deploy the matching EnchantsSquared build from this branch.
3. Install this folder under `<world>/datapacks/EnchantsSquaredNative/` with `pack.mcmeta` at the root.
4. Restart; check `/datapack list` and test `/enchant @s enchantssquared:crushing 1` on a compatible item.
5. Test enchanting table, anvils, books, grindstones, and ValhallaMMO progression on a staging server.

This pack **replaces the enchanting-table enchantment tag**, removing vanilla enchants from TABLE ROLLS only. Existing items and other vanilla loot sources are unchanged. It adds 44 custom table-eligible entries.

21 source enchantments lack configured compatibility and are excluded from the table until supported items are configured: kinship, curse_heavy, curse_hunger, nausea, blinding, arrow_aoe, toxic, illuminated, auto_replant, shockwave, beheading, chemical_shield, tradeoff_berserk, trident_sharpness, grappling, tree_feller, pulling, rapid_shot, streak_damage, ground_stomping, exploding.

Native registry max levels, supported items and weights are derived from the existing source configuration. The data-driven cost curves are temporary defaults (5–50, +10 per additional level) and need balancing. Perk conversion starts with example replacements in `config_native_enchanting.yml`; review your server's perk tree. Native enchanted items are visible to the existing Java effects through the updated manager. Legacy PDC items are read and converted when the manager rewrites them; perform a staged migration before production.
