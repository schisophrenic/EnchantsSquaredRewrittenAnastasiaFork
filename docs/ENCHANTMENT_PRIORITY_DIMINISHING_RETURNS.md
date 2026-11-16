# Enchantment priority / diminishing returns (design, not yet wired)

This feature is intended for Slimefun-generated or otherwise multi-enchanted armor and weapons. Existing levels and tier upgrades remain unchanged. This is a runtime calculation, not an item rewrite.

## Families and priority order
Defense, in priority order: protection, magic_protection, projectile_protection, blast_protection, fire_protection.
Offense, in priority order: sharpness, smite, bane_of_arthropods.

## Allocation tables
Defense: 1=[10], 2=[5,5], 3=[5,3,2], 4=[4,2,2,2], 5=[3,2,2,2,1].
Offense: 1=[10], 2=[5,5], 3=[5,3,2].
Only present families count. The first entry goes to the highest-priority present family. Each family's effective bonus is its unmodified bonus multiplied by allocation/10. Levels remain intact.

For example, a helmet with protection and fire_protection receives 50% of each enchant's original bonus; a helmet with just fire_protection receives 100% of its bonus.

## Runtime requirements
- Defense allocation is **per armor piece**, before bonuses are combined across pieces.
- Offense allocation is **per held attacking item**.
- Support vanilla + Lesser/Advanced/Supreme families; only the strongest enchant tier of the *same* family contributes.
- Vanilla protection EPF is calculated by Minecraft itself. A separate damage-event multiplier alone cannot undo that mitigation safely; correct integration must adjust the vanilla calculation path or compensate with correct before/after damage components.
- Avoid duplicate effects from vanilla, ValhallaMMO and this plugin. Preserve damage caps and applicable custom damage type classification.
- Preserve existing enchantment levels, PDC, model data, lore and Slimefun item metadata.
- Apply scaling to the enchantment contribution only, not to unrelated ValhallaMMO resistance, armor, toughness, or generic damage.
- Priority order and allocations should be configurable in YAML with safe defaults above, and invalid allocation tables rejected.
- Tier upgrades of the same family stay exclusive; different families may coexist.
- Check vanilla versus native enchantments in the current implementation. If Paper's enchantment registry conflicts block normal enchanting, Slimefun-forced combinations still require runtime scaling.

## Verification matrix
- Each single protection family at 100%, including magic only.
- Every 2/3/4/5 protection combination, with noncontiguous priority membership.
- Mixed armor slots, vanilla/native tier variants, overleveled ValhallaMMO levels.
- Sharpness + Smite on undead; Sharpness + Bane on arthropods; all three together.
- Slimefun-forced combinations, smithing, anvil, enchant table, and stored books.
- Ensure no double damage-reduction or duplicate bonus against incoming Valhalla custom types.

Implementation status: SPEC ONLY. No listener/engine source has been modified by this commit.
