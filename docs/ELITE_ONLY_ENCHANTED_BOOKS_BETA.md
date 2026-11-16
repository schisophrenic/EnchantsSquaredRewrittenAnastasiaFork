# Elite-only enchanted-book drops (Purpur 26.3 beta)

The native EnchantsSquared loot listener checks ValhallaEquipment's PDC marker
`valhallaequipment:elite_mob_v1` (BYTE != 0) on each defeated non-player mob.
The plugin `ValhallaEquipment` must be enabled. By default the elite must be
killed by a player (`LivingEntity.getKiller()`).

## Balancing

In `plugins/EnchantsSquared/config_native_enchants.yml`, add or edit:

```yaml
elite_enchanted_books:
  enabled: true
  chance: 0.02
  require_player_kill: true
```

This is a **single 2% roll for each eligible elite kill**, not one chance
per enchantment. Zero or one book can drop from a mob through this listener.
The value is per elite mob: expect about one book per 50 eligible elite kills
on average, but random streaks are possible. The built-in ValhallaEquipment
elite-spawn chance is separate.

## Sources and contents

- Non-elite mob deaths: enchanted books in the Bukkit death drop list removed.
- Elite mob deaths: existing book drops in the event list removed; at most
  one new book generated if the single chance roll succeeds.
- Natural generated loot tables: enchanted books removed.
- Fishing: caught enchanted books replaced by ordinary cod.
- Non-book equipment, gemstones, mob gear, XP and ordinary drops are unchanged.
- Existing player-owned books, anvils, book enchanting, and villager trades
  remain available. The restriction is specifically for **loot drops**, not
  every way to obtain an enchanted book.
- One book contains one random vanilla or enabled native enchantment.
  Lesser/Advanced/Supreme perk-gated variants are excluded from the elite
  random pool; normal progression remains intact.
- This only controls the loot events handled by this plugin. Other plugins
  that spawn items outside those events can bypass it.

## Test plan

1. Back up a staging world and use only the new EnchantsSquared test JAR.
2. Keep ValhallaEquipment enabled and check its elite generation is active.
3. Use `/valeq mobspawn ZOMBIE 30 false` and `/valeq mobspawn ZOMBIE 30 true`.
4. Temporarily set `chance: 1.0` and kill several non-elites and elites.
   Non-elites should drop no enchanted books; each player-killed elite should
   drop exactly **one**.
5. Test natural chest loot, fishing, and ordinary mobs for no book drops.
6. Set `require_player_kill: true` and confirm elites dying without a
   player killer have no book.
7. Restore `chance: 0.02`. With 100 elite player kills the average
   is two books, but it is not guaranteed.
8. Check ValhallaEquipment quality, models, mobs, and normal equipment
   drops still work.

If ValhallaMMO's built-in mob scaling is enabled and ValhallaEquipment's
fallback elite generation is disabled, mobs may lack the elite marker.
This system **fails closed** and does not distribute enchanted books
to unmarked mobs.

This beta has not yet passed a verified Maven build or live server test.
