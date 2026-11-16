# EnchantsSquared base enchantment audit (Purpur 26.3)

Source: `CustomEnchantManager.registerEnchants()`, 67 declared IDs, `config.yml`, and individual Java effect classes, on branch `feature/native-enchants-valhalla-26-3`.

**63 base enchantments are enabled by the checked-in config**. `flight`, `overclock` and `mace_aoe` are configured disabled; `void_touch` is also disabled **and its registration is commented out**. Actual server overrides can change enabled flags for the first three.

**Important:** The plugin currently retains the original event handlers and PDC-based storage for these enchantments; some native reads are supported by `CustomEnchantManager`, but not all base entries are registered into Paper's enchantment registry yet. Avoid generating registry-only entries before the effect bindings, conflicts, and migration path are verified.

| ID | Enchantment key | Java event group | Config enabled | Default max | Effect from base config |
|---:|---|---|---|---:|---|
| 1 | `excavation` | on_block_break | Yes | 1 | />/ /M/i/n/e/s/ /a/ /2/x/1/ /-/ /+/ /s/h/a/p/e/d/ /-/ /3/x/3/ /a/r/e/a/ /i/n/s/t/e/a/d/ /o/f/ /1/x/1/x/1/./ /S/n/e/a/k/ /w/h/i/l/e/ /m/i/n/i/n/g/ /t/o/ /s/t/o/p/ /t/h/i/s/ /e/f/f/e/c/t/./ |
| 2 | `sunforged` | on_block_break | Yes | — | />/ /S/m/e/l/t/s/ /b/l/o/c/k/s/ /m/i/n/e/d/ |
| 3 | `kinship` | on_block_break | Yes | 3 | />/ /I/f/ /t/h/e/ /p/i/c/k/a/x/e/ /m/a/t/c/h/e/s/ /t/h/e/ /m/a/t/e/r/i/a/l/ /o/f/ /t/h/e/ /o/r/e/ /y/o/u/'/r/e/ /m/i/n/i/n/g/,/ /s/o/m/e/ /d/u/r/a/b/i/l/i/t/y/ /i/s/ /r/e/s/t/o/r/e/d/./ /s/t/o/n/e/:/0/./0/5/-/0/./1/-/0/./1/5/,/ /i/r/o/n/:/1/-/2/-/3/,/ /d/i/a/m/o/n/d/:/1/0/-/2/0/-/3/0/,/ /n/e/t/h/e/r/i/t/e/:/5/0/-/1/0/0/-/1/5/0/ |
| 4 | `flight` | regular_interval | No | 3 | />/ /A/l/l/o/w/s/ /t/h/e/ /w/e/a/r/e/r/ /t/e/m/p/o/r/a/r/y/ /f/l/i/g/h/t/ /a/t/ /t/h/e/ /c/o/s/t/ /o/f/ /d/u/r/a/b/i/l/i/t/y/ /f/o/r/ /5/-/1/0/-/1/5/ /s/e/c/o/n/d/s/ /a/t/ /a/ /t/i/m/e/./ /R/e/c/h/a/r/g/e/ /y/o/u/r/ /f/l/i/g/h/t/ /b/y/ /s/t/a/n/d/i/n/g/ /o/n/ /t/h/e/ /g/r/o/u/n/d/ /a/t/ /1/-/1/./5/-/2/x/ /s/p/e/e/d/ |
| 5 | `rejuvenation` | regular_interval | Yes | 3 | />/ /S/l/o/w/l/y/ /r/e/g/e/n/e/r/a/t/e/s/ /d/u/r/a/b/i/l/i/t/y/ |
| 6 | `lava_walker` | regular_interval | Yes | 2 | />/ /T/r/a/n/s/f/o/r/m/s/ /l/a/v/a/ /u/n/d/e/r/ /y/o/u/ /w/i/t/h/i/n/ /a/ /r/a/d/i/u/s/ /o/f/ /0/-/1/-/2/ /b/l/o/c/k/s/ /i/n/t/o/ /m/a/g/m/a/,/ /a/t/ /t/h/e/ /c/o/s/t/ /o/f/ /d/u/r/a/b/i/l/i/t/y/./ |
| 7 | `speed_boost` | regular_interval | Yes | 2 | />/ /G/r/a/n/t/s/ /t/h/e/ /w/e/a/r/e/r/ /2/0/-/3/0/%/ /i/n/c/r/e/a/s/e/d/ /m/o/v/e/m/e/n/t/ /s/p/e/e/d/ /w/h/i/l/e/ /w/e/a/r/i/n/g/ |
| 8 | `jump_boost` | regular_interval | Yes | 2 | />/ /G/r/a/n/t/s/ /t/h/e/ /w/e/a/r/e/r/ /j/u/m/p/ /b/o/o/s/t/ /I/-/I/I/ /w/h/i/l/e/ /w/e/a/r/i/n/g/ |
| 9 | `night_vision` | regular_interval | Yes | — | />/ /G/r/a/n/t/s/ /t/h/e/ /p/l/a/y/e/r/ /n/i/g/h/t/ /v/i/s/i/o/n/ /w/h/i/l/e/ /w/e/a/r/i/n/g/ |
| 10 | `water_breathing` | regular_interval | Yes | — | />/ /G/r/a/n/t/s/ /t/h/e/ /p/l/a/y/e/r/ /w/a/t/e/r/ /b/r/e/a/t/h/i/n/g/ /w/h/i/l/e/ /w/e/a/r/i/n/g/ |
| 11 | `haste` | regular_interval | Yes | 2 | />/ /G/r/a/n/t/s/ /t/h/e/ /p/l/a/y/e/r/ /h/a/s/t/e/ /I/-/I/I/ /w/h/i/l/e/ /h/o/l/d/i/n/g/ |
| 12 | `metabolism` | regular_interval | Yes | 2 | />/ /S/l/o/w/l/y/ /r/e/g/e/n/e/r/a/t/e/s/ /h/u/n/g/e/r/ /p/o/i/n/t/s/ /w/h/i/l/e/ /m/o/v/i/n/g/ |
| 13 | `strength` | regular_interval | Yes | 2 | />/ /G/r/a/n/t/s/ /t/h/e/ /p/l/a/y/e/r/ /+/3/-/6/ /a/t/t/a/c/k/ /d/a/m/a/g/e/ /w/h/i/l/e/ /h/o/l/d/i/n/g/ |
| 14 | `vigorous` | regular_interval | Yes | 5 | />/ /A/d/d/s/ /1/ /p/e/r/ /l/e/v/e/l/ /e/x/t/r/a/ /m/a/x/ /h/e/a/l/t/h/ /t/o/ /t/h/e/ /w/e/a/r/e/r/ |
| 15 | `lucky` | regular_interval | Yes | 3 | />/ /G/r/a/n/t/s/ /t/h/e/ /p/l/a/y/e/r/ /1/-/2/-/3/ /l/u/c/k/ /w/h/i/l/e/ /w/e/a/r/i/n/g/ |
| 16 | `curse_durability` | on_item_damage | Yes | 3 | />/ /C/u/r/s/e/ /e/n/c/h/a/n/t/ /t/h/a/t/ /c/a/u/s/e/s/ /i/t/e/m/s/ /t/o/ /b/r/e/a/k/ /2/-/3/-/4/x/ /q/u/i/c/k/e/r/ |
| 17 | `curse_heavy` | regular_interval | Yes | 2 | />/ /C/u/r/s/e/ /e/n/c/h/a/n/t/ /t/h/a/t/,/ /w/h/i/l/e/ /h/o/l/d/i/n/g///w/e/a/r/i/n/g/,/ /s/l/o/w/s/ /t/h/e/ /u/s/e/r/ /d/o/w/n/ /b/y/ /1/0/-/2/0/%/ |
| 18 | `curse_hunger` | regular_interval | Yes | 2 | />/ /C/u/r/s/e/ /e/n/c/h/a/n/t/ /t/h/a/t/ /s/l/o/w/l/y/ /e/a/t/s/ /a/w/a/y/ /a/t/ /y/o/u/r/ /h/u/n/g/e/r/ |
| 19 | `withering` | on_attack | Yes | 3 | />/ /H/a/s/ /a/ /5/-/1/0/-/1/5/%/ /c/h/a/n/c/e/ /t/o/ /i/n/f/l/i/c/t/ /t/h/e/ /e/n/t/i/t/y/ /y/o/u/'/r/e/ /d/a/m/a/g/i/n/g/ /w/i/t/h/ /W/i/t/h/e/r/ |
| 20 | `stunning` | on_attack | Yes | 2 | />/ /H/a/s/ /a/ /2/-/4/%/ /c/h/a/n/c/e/ /t/o/ /i/n/f/l/i/c/t/ /t/h/e/ /e/n/t/i/t/y/ /y/o/u/'/r/e/ /d/a/m/a/g/i/n/g/ /w/i/t/h/ /a/ /s/t/u/n/./ |
| 21 | `slowing` | on_attack | Yes | 2 | />/ /H/a/s/ /a/ /4/-/8/%/ /c/h/a/n/c/e/ /t/o/ /i/n/f/l/i/c/t/ /t/h/e/ /e/n/t/i/t/y/ /y/o/u/'/r/e/ /d/a/m/a/g/i/n/g/ /w/i/t/h/ /S/l/o/w/n/e/s/s/ |
| 22 | `nausea` | on_attack | Yes | 2 | />/ /H/a/s/ /a/ /3/-/5/%/ /c/h/a/n/c/e/ /t/o/ /i/n/f/l/i/c/t/ /t/h/e/ /e/n/t/i/t/y/ /y/o/u/'/r/e/ /d/a/m/a/g/i/n/g/ /w/i/t/h/ /N/a/u/s/e/a/ |
| 23 | `weakening` | on_attack | Yes | 2 | />/ /H/a/s/ /a/ /5/-/1/0/%/ /c/h/a/n/c/e/ /t/o/ /i/n/f/l/i/c/t/ /t/h/e/ /e/n/t/i/t/y/ /y/o/u/'/r/e/ /d/a/m/a/g/i/n/g/ /w/i/t/h/ /W/e/a/k/n/e/s/s/ |
| 24 | `poisoning` | on_attack | Yes | 3 | />/ /H/a/s/ /a/ /5/-/1/0/-/1/5/%/ /c/h/a/n/c/e/ /t/o/ /i/n/f/l/i/c/t/ /t/h/e/ /e/n/t/i/t/y/ /y/o/u/'/r/e/ /d/a/m/a/g/i/n/g/ /w/i/t/h/ /P/o/i/s/o/n/ |
| 25 | `blinding` | on_attack | Yes | 3 | />/ /H/a/s/ /a/ /2/-/4/-/6/%/ /c/h/a/n/c/e/ /t/o/ /i/n/f/l/i/c/t/ /t/h/e/ /e/n/t/i/t/y/ /y/o/u/'/r/e/ /d/a/m/a/g/i/n/g/ /w/i/t/h/ /B/l/i/n/d/n/e/s/s/ |
| 26 | `crushing` | on_attack | Yes | 3 | />/ /F/o/r/ /e/a/c/h/ /p/i/e/c/e/ /o/f/ /h/e/a/v/y/ /a/r/m/o/r/ /t/h/e/ /e/n/e/m/y/ /i/s/ /w/e/a/r/i/n/g/(/g/o/l/d/,/ /i/r/o/n/,/ /n/e/t/h/e/r/i/t/e/)/,/ /y/o/u/ /d/e/a/l/ /2/./5/-/3/./7/5/-/5/%/ /m/o/r/e/ /d/a/m/a/g/e/ |
| 27 | `arrow_aoe` | on_attack | Yes | 3 | />/ /H/i/t/t/i/n/g/ /a/n/ /a/r/r/o/w/ /o/n/ /a/n/ /e/n/t/i/t/y/ /d/e/a/l/s/ /5/0/-/7/0/-/9/0/%/ /o/f/ /t/h/e/ /d/a/m/a/g/e/ /t/o/ /a/l/l/ /e/n/e/m/i/e/s/ /i/n/ /a/ /1/./5/-/2/-/2/./5/ /b/l/o/c/k/ /r/a/d/i/u/s/ /a/r/o/u/n/d/ /t/h/a/t/ /i/n/i/t/i/a/l/ /e/n/t/i/t/y/ |
| 28 | `toxic` | on_attack | Yes | 3 | />/ /H/i/t/t/i/n/g/ /a/n/ /e/n/t/i/t/y/ /r/e/d/u/c/e/s/ /i/t/s/ /h/e/a/l/i/n/g/ /r/e/c/e/i/v/e/d/ /b/y/ /4/0/-/6/0/-/8/0/%/ /f/o/r/ /4/-/5/-/6/ /s/e/c/o/n/d/s/ |
| 29 | `shielding` | on_attacked | Yes | 3 | />/ /H/a/s/ /a/ /2/0/-/4/0/%/ /c/h/a/n/c/e/ /t/o/ /d/e/f/l/e/c/t/ /p/r/o/j/e/c/t/i/l/e/s/ /t/h/a/t/ /w/o/u/l/d/ /n/o/r/m/a/l/l/y/ /h/i/t/ /y/o/u/ |
| 30 | `knockback_protection` | regular_interval | Yes | 2 | />/ /R/e/d/u/c/e/s/ /k/n/o/c/k/b/a/c/k/ /t/a/k/e/n/ /b/y/ /2/0/-/4/0/%/ |
| 31 | `vitality` | on_heal | Yes | 5 | />/ /I/n/c/r/e/a/s/e/s/ /t/h/e/ /w/e/a/r/e/r///h/o/l/d/e/r/'/s/ /h/e/a/l/i/n/g/ /b/y/ /5/%/ /p/e/r/ /l/e/v/e/l/,/ /a/d/d/i/n/g/ /u/p/ /a/l/l/ /v/i/t/a/l/i/t/y/ /y/o/u/ /h/a/v/e/ /o/n/ /a/l/l/ /p/i/e/c/e/s/ /o/f/ /a/r/m/o/r/ /u/p/ /t/o/ /1/0/0/%/./ |
| 32 | `illuminated` | on_interact | Yes | — | />/ /S/h/i/f/t/-/R/i/g/h/t/ /c/l/i/c/k/ /w/i/t/h/ /a/ /p/i/c/k/a/x/e/ /o/n/ /a/n/ /e/m/p/t/y/ /s/u/r/f/a/c/e/ /t/o/ /p/l/a/c/e/ /a/ /t/o/r/c/h/ /a/t/ /t/h/e/ /c/o/s/t/ /o/f/ /3/ /d/u/r/a/b/i/l/i/t/y/ |
| 33 | `auto_replant` | on_interact | Yes | — | />/ /R/i/g/h/t/ /c/l/i/c/k/ /w/i/t/h/ /a/ /h/o/e/ /o/n/ /a/ /f/u/l/l/y/ /g/r/o/w/n/ /c/r/o/p/ /t/o/ /i/n/s/t/a/n/t/l/y/ /h/a/r/v/e/s/t/ /a/n/d/ /r/e/p/l/a/n/t/ /i/t/ |
| 34 | `shockwave` | on_interact | Yes | 2 | />/ /R/i/g/h/t/ /c/l/i/c/k/i/n/g/ /o/n/ /t/h/e/ /u/p/p/e/r/ /s/u/r/f/a/c/e/ /o/f/ /a/ /b/l/o/c/k/ /c/a/u/s/e/s/ /a/ /s/h/o/c/k/w/a/v/e/ /t/h/a/t/ /s/e/n/d/s/ /e/n/t/i/t/i/e/s/ /f/l/y/i/n/g/ /b/a/c/k/w/a/r/d/s/ /i/n/ /a/ /r/a/d/i/u/s/ /o/f/ /3/-/4/ /b/l/o/c/k/s/,/ /w/i/t/h/ /a/ /s/t/r/e/n/g/t/h/ /o/f/ /0/./5/-/0/./7/ |
| 35 | `bonus_exp` | on_death | Yes | 3 | />/ /K/i/l/l/i/n/g/ /a/n/ /e/n/t/i/t/y/ /a/d/d/s/ /1/-/1/./5/-/2/ /m/o/r/e/ /E/X/P/ |
| 36 | `vampiric` | on_death | Yes | 2 | />/ /D/a/m/a/g/i/n/g/ /n/o/n/-/p/a/s/s/i/v/e/ /e/n/t/i/t/i/e/s/ /h/e/a/l/s/ /y/o/u/ /f/o/r/ /5/%/-/1/0/%/ /o/f/ /t/h/e/ /d/a/m/a/g/e/ /d/e/a/l/t/ /(/u/p/ /t/o/ /2/ /p/o/i/n/t/s/ /m/a/x/)/,/ /a/n/d/ /a/n/ /a/d/d/i/t/i/o/n/a/l/ /2/-/4/ /p/o/i/n/t/s/ /w/h/e/n/ /y/o/u/ /k/i/l/l/ /i/t/ |
| 37 | `beheading` | on_death | Yes | 2 | />/ /K/i/l/l/i/n/g/ /a/n/ /e/n/t/i/t/y/ /a/d/d/s/ /a/ /1/-/2/%/ /c/h/a/n/c/e/ /o/f/ /d/r/o/p/p/i/n/g/ /i/t/s/ /h/e/a/d/./ /W/o/r/k/s/ /w/i/t/h/ /c/r/e/e/p/e/r/s/,/ /s/k/e/l/e/t/o/n/s/,/ /p/l/a/y/e/r/s/,/ /e/t/c/./ /A/x/e/s/ /a/r/e/ /t/w/i/c/e/ /a/s/ /p/o/t/e/n/t/ |
| 38 | `soulbound` | on_death | Yes | 1 | />/ /D/y/i/n/g/ /w/i/t/h/ /a/ /s/o/u/l/b/o/u/n/d/ /i/t/e/m/ /p/r/e/v/e/n/t/s/ /i/t/ /f/r/o/m/ /b/e/i/n/g/ /d/r/o/p/p/e/d/,/ /i/n/s/t/e/a/d/,/ /y/o/u/ /r/e/s/p/a/w/n/ /w/i/t/h/ /i/t/ /i/n/ /y/o/u/r/ /i/n/v/e/n/t/o/r/y/ |
| 39 | `chemical_shield` | on_potion_effect | Yes | — | />/ /W/h/i/l/e/ /b/l/o/c/k/i/n/g/,/ /s/p/l/a/s/h/ /p/o/t/i/o/n/s/ /a/n/d/ /p/o/t/i/o/n/ /e/f/f/e/c/t/ /c/l/o/u/d/s/ /d/o/n/'/t/ /a/f/f/e/c/t/ /y/o/u/./ /C/a/r/e/f/u/l/,/ /a/s/ /t/h/i/s/ /a/l/s/o/ /b/l/o/c/k/s/ /b/e/n/e/f/i/c/i/a/l/ /e/f/f/e/c/t/s/./ |
| 40 | `potion_potency_buff` | on_potion_effect | Yes | 2 | />/ /M/o/s/t/ /p/o/t/i/o/n/s/ /y/o/u/ /d/r/i/n/k/ /w/i/l/l/ /h/a/v/e/ /t/h/e/i/r/ /d/u/r/a/t/i/o/n/ /r/e/d/u/c/e/d/ /b/y/ /2/0/-/4/0/%/,/ /b/u/t/ /h/a/v/e/ /t/h/e/i/r/ /a/m/p/l/i/f/i/e/r/s/ /i/n/c/r/e/a/s/e/d/ /b/y/ /5/0/-/1/0/0/%/ |
| 41 | `tradeoff_berserk` | on_attacked | Yes | 3 | />/ /Y/o/u/ /d/e/a/l/ /2/0/-/3/5/-/5/0/%/ /m/o/r/e/ /d/a/m/a/g/e/,/ /b/u/t/ /y/o/u/ /a/l/s/o/ /t/a/k/e/ /3/0/-/5/0/-/7/0/%/ /m/o/r/e/ /d/a/m/a/g/e/ |
| 42 | `plating` | regular_interval | Yes | 3 | />/ /A/d/d/s/ /2/ /p/e/r/ /l/e/v/e/l/ /e/x/t/r/a/ /a/r/m/o/r/ /t/o/ /t/h/e/ /i/t/e/m/ |
| 43 | `trident_sharpness` | on_attack | Yes | 5 | />/ /T/r/i/d/e/n/t/s/ /d/o/ /0/./7/5/-/1/./5/-/2/./2/5/-/3/./0/-/3/./7/5/ /m/o/r/e/ /d/a/m/a/g/e/ |
| 44 | `grappling` | on_fishing | Yes | 2 | />/ /T/u/r/n/s/ /f/i/s/h/i/n/g/ /r/o/d/s/ /i/n/t/o/ /g/r/a/p/p/l/i/n/g/ /h/o/o/k/s/./ /T/h/e/ /h/o/o/k/ /m/u/s/t/ /b/e/ /c/o/n/n/e/c/t/e/d/ /t/o/ /a/ /s/u/r/f/a/c/e/ /t/o/ /b/e/ /a/b/l/e/ /t/o/ /g/r/a/p/p/l/e/ /t/o/ /i/t/ |
| 45 | `lava_resistance` | regular_interval | Yes | — | />/ /G/r/a/n/t/s/ /t/h/e/ /w/e/a/r/e/r/ /f/i/r/e/ /r/e/s/i/s/t/a/n/c/e/ /w/h/i/l/e/ /w/e/a/r/i/n/g/,/ /b/u/t/ /a/p/p/l/i/e/s/ /a/ /s/i/g/n/i/f/i/c/a/n/t/ /s/l/o/w/ /w/h/i/l/e/ /o/n/ /f/i/r/e/ |
| 46 | `perforating` | on_attack | Yes | 3 | />/ /F/o/r/ /e/a/c/h/ /p/i/e/c/e/ /o/f/ /l/i/g/h/t/ /a/r/m/o/r/ /t/h/e/ /e/n/e/m/y/ /i/s/ /w/e/a/r/i/n/g/(/l/e/a/t/h/e/r/,/ /c/h/a/i/n/,/ /d/i/a/m/o/n/d/)/,/ /y/o/u/ /d/e/a/l/ /2/./5/-/3/./7/5/-/5/%/ /m/o/r/e/ /d/a/m/a/g/e/ |
| 47 | `damage_reduction` | on_damaged | Yes | 3 | />/ /G/r/a/n/t/s/ /t/h/e/ /w/e/a/r/e/r/ /1/0/-/1/5/-/2/0/%/ /d/a/m/a/g/e/ /r/e/d/u/c/t/i/o/n/ /w/h/i/l/e/ /w/e/a/r/i/n/g/ |
| 48 | `shield_downer` | on_attack | Yes | 3 | />/ /H/a/s/ /a/ /2/0/-/4/0/-/6/0/%/ /c/h/a/n/c/e/ /t/o/ /d/i/s/a/b/l/e/ /a/ /s/h/i/e/l/d/ /f/o/r/ /2/-/3/-/4/ /s/e/c/o/n/d/s/ |
| 49 | `telekinesis` | on_block_break | Yes | — | />/ /A/n/y/ /d/r/o/p/s/ /f/r/o/m/ /m/i/n/e/d/ /b/l/o/c/k/s/ /a/r/e/ /t/r/a/n/s/f/e/r/r/e/d/ /d/i/r/e/c/t/l/y/ /t/o/ /y/o/u/r/ /i/n/v/e/n/t/o/r/y/ |
| 50 | `lightning` | on_attack | Yes | 3 | />/ /H/a/s/ /a/ /5/-/1/0/-/1/5/%/ /c/h/a/n/c/e/ /t/o/ /s/t/r/i/k/e/ /t/h/e/ /e/n/e/m/y/ /w/i/t/h/ /l/i/g/h/t/n/i/n/g/ |
| 51 | `tree_feller` | on_block_break | Yes | — | />/ /M/i/n/i/n/g/ /a/ /t/r/e/e/,/ /w/h/i/l/e/ /s/n/e/a/k/i/n/g/,/ /d/e/s/t/r/o/y/s/ /t/h/e/ /w/h/o/l/e/ /t/r/e/e/ /i/n/s/t/a/n/t/l/y/ |
| 52 | `pulling` | on_attack | Yes | 3 | />/ /H/i/t/t/i/n/g/ /a/n/ /a/r/r/o/w/ /o/n/ /a/n/ /e/n/t/i/t/y/ /h/a/s/ /a/ /2/0/-/4/0/-/6/0/%/ /c/h/a/n/c/e/ /t/o/ /p/u/l/l/ /i/t/ /c/l/o/s/e/r/./ /T/h/e/ /p/u/l/l/i/n/g/ /e/f/f/e/c/t/ /b/e/c/o/m/e/s/ /s/t/r/o/n/g/e/r/ /p/e/r/ /l/e/v/e/l/ /a/s/ /w/e/l/l/./ |
| 53 | `rapid_shot` | on_shoot | Yes | 3 | />/ /G/r/a/n/t/s/ /a/ /2/0/-/4/0/-/6/0/%/ /c/h/a/n/c/e/ /t/o/ /s/h/o/o/t/ /2/-/3/-/4/ /a/r/r/o/w/s/ /i/n/ /q/u/i/c/k/ /s/u/c/c/e/s/s/i/o/n/./ /T/h/e/s/e/ /a/r/r/o/w/s/,/ /e/x/c/e/p/t/ /t/h/e/ /l/a/s/t/ /o/n/e/,/ /r/e/m/o/v/e/ /i/m/m/u/n/i/t/y/ /f/r/a/m/e/s/ /f/r/o/m/ /t/h/e/ /h/i/t/ /t/a/r/g/e/t/,/ /b/u/t/ /d/e/a/l/ /o/n/l/y/ /5/0/-/4/5/-/4/0/%/ /d/a/m/a/g/e/./ |
| 54 | `streak_damage` | on_attack | Yes | 2 | />/ /R/e/d/u/c/e/s/ /y/o/u/r/ /d/a/m/a/g/e/ /b/y/ /2/0/-/4/0/%/,/ /b/u/t/ /g/r/a/n/t/s/ /y/o/u/ /5/-/1/0/%/ /i/n/c/r/e/a/s/e/d/ /d/a/m/a/g/e/ /p/e/r/ /h/i/t/ /u/p/ /t/o/ /+/5/0/-/1/0/0/%/ /d/a/m/a/g/e/./ /Y/o/u/ /l/o/s/e/ /t/h/i/s/ /d/a/m/a/g/e/ /b/o/n/u/s/ /w/h/e/n/ /y/o/u/ /t/a/k/e/ /d/a/m/a/g/e/ /y/o/u/r/s/e/l/f/./ /D/a/m/a/g/e/ /p/e/n/a/l/t/y/ /a/n/d/ /b/o/n/u/s/ /a/r/e/ /h/a/l/v/e/d/ /w/i/t/h/ /r/a/n/g/e/d/ /h/i/t/s/ |
| 55 | `ground_stomping` | on_damaged | Yes | 2 | />/ /I/m/m/u/n/i/t/y/ /t/o/ /f/a/l/l/ /d/a/m/a/g/e/./ /L/a/n/d/i/n/g/ /a/f/t/e/r/ /g/r/e/a/t/ /f/a/l/l/s/ /d/e/a/l/s/ /d/a/m/a/g/e/ /i/n/ /a/ /r/a/d/i/u/s/./ /D/a/m/a/g/e/ /a/n/d/ /r/a/d/i/u/s/ /i/n/c/r/e/a/s/e/ /w/i/t/h/ /d/i/s/t/a/n/c/e/,/ /s/c/a/l/i/n/g/ /6/0/-/1/0/0/%/ /f/a/s/t/e/r/./ /W/h/i/l/e/ /s/n/e/a/k/i/n/g/,/ /y/o/u/ /f/a/l/l/ /2/./5/x/ /f/a/s/t/e/r/./ /E/f/f/e/c/t/ /h/a/s/ /a/ /2/0/-/1/0/ /s/e/c/o/n/d/ /c/o/o/l/d/o/w/n/ |
| 56 | `exploding` | on_death | Yes | 3 | />/ /K/i/l/l/i/n/g/ /a/n/ /e/n/t/i/t/y/ /c/a/u/s/e/s/ /a/ /f/i/e/r/y/ /e/x/p/l/o/s/i/o/n/ /i/n/ /a/ /2/-/3/-/4/ /b/l/o/c/k/ /r/a/d/i/u/s/,/ /d/e/a/l/i/n/g/ /2/-/4/-/6/ /i/m/m/e/d/i/a/t/e/ /d/a/m/a/g/e/ /a/n/d/ /4/-/6/-/8/ /b/u/r/n/ /d/a/m/a/g/e/ |
| 57 | `scale` | regular_interval | Yes | 2 | />/ /M/a/k/e/s/ /y/o/u/ /2/5/-/5/0/%/ /s/m/a/l/l/e/r/,/ /a/l/s/o/ /a/f/f/e/c/t/i/n/g/ /y/o/u/r/ /b/l/o/c/k/-/ /a/n/d/ /a/t/t/a/c/k/ /r/e/a/c/h/ |
| 58 | `scale2` | regular_interval | Yes | 2 | />/ /I/n/c/r/e/a/s/e/s/ /y/o/u/r/ /b/l/o/c/k/-/ /a/n/d/ /a/t/t/a/c/k/ /r/e/a/c/h/ /b/y/ /2/5/-/5/0/%/,/ /b/u/t/ /a/l/s/o/ /m/a/k/e/s/ /y/o/u/ /2/5/-/5/0/%/ /b/i/g/g/e/r/ |
| 59 | `step_height` | regular_interval | Yes | 1 | />/ /A/l/l/o/w/s/ /y/o/u/ /t/o/ /s/t/e/p/ /u/p/ /f/u/l/l/ /b/l/o/c/k/s/ |
| 60 | `block_reach` | regular_interval | Yes | 3 | />/ /A/l/l/o/w/s/ /y/o/u/ /t/o/ /r/e/a/c/h/ /b/l/o/c/k/s/ /2/-/4/-/6/ /b/l/o/c/k/s/ /f/u/r/t/h/e/r/ |
| 61 | `piercer` | on_attack | Yes | — | />/ /5/0/%/ /o/f/ /b/a/s/e/ /w/e/a/p/o/n/ /d/a/m/a/g/e/ /p/i/e/r/c/e/s/ /a/n/y/ /d/a/m/a/g/e/ /r/e/d/u/c/t/i/o/n/./ /I/N/C/O/M/P/A/T/I/B/L/E/ /W/I/T/H/ /A/N/Y/ /O/T/H/E/R/ /E/N/C/H/A/N/T/ |
| 62 | `speed_master` | regular_interval | Yes | 3 | />/ /G/r/a/n/t/s/ /t/h/e/ /w/i/e/l/d/e/r/ /a/n/d/ /t/h/e/i/r/ /m/o/u/n/t/ /1/0/-/1/5/-/2/0/%/ /i/n/c/r/e/a/s/e/d/ /m/o/v/e/m/e/n/t/ /s/p/e/e/d/ |
| 63 | `cowardice` | on_attack | Yes | 3 | />/Y/o/u/ /d/e/a/l/ /2/0/-/3/0/-/4/0/%/ /m/o/r/e/ /d/a/m/a/g/e/ /w/h/i/l/e/ /a/t/ /f/u/l/l/ /h/e/a/l/t/h/./ |
| 64 | `void_touch` | on_attack | No (commented) | 3 | />/A/p/p/l/i/e/s/ /"/V/o/i/d/ /T/o/u/c/h/e/d/"/ /s/t/a/t/u/s/ /e/f/f/e/c/t/ /t/h/a/t/,/ /o/v/e/r/ /3/-/4/-/5/ /s/e/c/o/n/d/s/,/ /i/n/c/r/e/a/s/e/s/ /d/a/m/a/g/e/ /u/p/ /t/o/ /3/0/-/4/0/-/5/0/%/ /f/o/r/ /t/h/e/ /u/s/e/r/ |
| 65 | `pain_cycle` | on_attack | Yes | 3 | />/C/o/n/s/u/m/e/s/ /5/-/1/0/%/ /o/f/ /y/o/u/r/ /h/p/ /o/n/ /e/a/c/h/ /h/i/t/,/ /a/f/t/e/r/ /5/ /h/i/t/s/ /y/o/u/r/ /n/e/x/t/ /o/n/e/ /w/i/l/l/ /i/n/c/r/e/a/s/e/ /b/a/s/e/ /d/a/m/a/g/e/ /b/a/s/e/d/ /o/n/ /h/e/a/l/t/h/ /c/o/n/s/u/m/e/d/ /w/i/t/h/ /h/a/l/f/ /e/f/f/i/c/i/e/n/c/y/ |
| 66 | `overclock` | on_attack | No | 2 | />/d/e/a/l/s/ /5/0/-/1/0/0/%/ /m/o/r/e/ /d/a/m/a/g/e/ /o/n/ /h/i/t/,/ /b/u/t/ /m/a/c/e/ /g/o/e/s/ /o/n/ /a/ /4/0/-/6/0/ /s/e/c/o/n/d/ /c/o/o/l/d/o/w/n/ /a/f/t/e/r/w/a/r/d/s/ |
| 67 | `mace_aoe` | on_attack | No | 2 | />/d/e/a/l/s/ /2/0/-/4/0/-/6/0/%/ /o/f/ /d/a/m/a/g/e/ /t/o/ /e/n/t/i/t/i/e/s/ /i/n/ /a/ /1/./5/ /b/l/o/c/k/ /r/a/d/i/u/s/ |

## High-confidence cross-source overlap candidates

This is a **behavioral** comparison, not a claim that the source code is byte-for-byte identical:

| Existing EnchantsSquared effect | So Many Enchantments equivalent/overlap | Action before adding |
|---|---|---|
| `vampiric` (heal on hit and on kill) | Lifesteal; Blessed Edge (partial) | Share healing logic; preserve Vampiric's kill bonus; choose conflict/stacking policy |
| `bonus_exp` (XP on kills) | Adept | Merge reward family or explicitly exclude stacking |
| `vigorous` (+max health) | Strengthened Vitality | Share health modifier implementation, one cap |
| `vitality` (healing received bonus) | Combat Medic | Share heal modifier; don't add twice |
| `sunforged` (auto-smelt block drops) | Smelter | Exact purpose match, preserve fortune/recipe behavior if different |
| `lava_walker` (temporary magma over lava) | Magma Walker | Same traversal concept; compare block conversion and durability costs |
| `poisoning` + `withering` | Envenomed | Envenomed combines both effects; create incompatibility or unified proc engine |
| `blinding` + `nausea` | Disorientating Blade | Combined dual-debuff effect; don't double-apply |
| `slowing` | Cryogenic | Slowing is a component, Cryogenic also applies fatigue/freezing |
| `crushing`, `perforating`, `piercer` | Penetrating Edge, Brutality, Rune: Piercing Capabilities | Related anti-armor mechanics, **not exact duplicates**; require a common armor-penetration limit |
| `shielding`, `damage_reduction`, `plating` | Evasion, Natural Blocking, Physical Protection, Magic Protection | Defensive overlap, mechanically distinct; ensure reduction caps across armor |
| `rapid_shot` (sequential extra arrows) | Splitshot (simultaneous split), Strafe (draw speed) | Distinct action; separate skills and rate/arrow limits |
| `arrow_aoe` (arrow splash damage) | Splitshot | Related multi-target result, **not the same effect** |
| `pulling` (pull target closer on arrow hit) | Dragging | Very similar displacement; one shared handling/cooldown |
| `beheading` (head drop chance on kill) | Culling (also produces a head) | Culling is execute; beheading is drop chance; cap head drop to one |
| `curse_heavy` (slow) | Heavy Weight (fall/jump/attack-speed penalties) | Related but not equivalent |
| `curse_durability` (durability penalty) | Rusted; Instability (partial) | Prevent multiplicative durability loss spikes |
| `lightning` (lightning on hit) | Rune: Magical Blessing (partial) | Different compound effects; distinguish damage types |
| `strength`, `speed_boost`, `jump_boost` | Inner Berserk, Light Weight, Swifter Slashes | Stat-family overlap only, not direct duplicate |

## Recommended migration rules

1. Keep the existing base enchant ID and gameplay Java class as canonical whenever a mod effect is genuinely equivalent. Avoid introducing a second working enchantment that triggers the same effect from an overlapping item.
2. Register the canonical enchantment as a real Paper registry entry **at bootstrap**, with material eligibility, max levels, mutually exclusive sets and correct anvil rules.
3. Continue to run the existing base Java effect handler exactly once and maintain existing PDC item compatibility. Never run both the legacy effect and a new duplicate listener for the same native enchant.
4. Keep distinct mod effects with distinct mechanics (e.g. Strafe vs Rapid Shot) as separate new enchantments, with proper conditions and caps.
5. Keep the already native NG tier families (Sharpness/Smite/Bane/Power/Punch/Flame/Fire Aspect/protections/Magic Protection) canonical. Add missing tier families rather than creating same-meaning duplicate names.
6. Add any new special effect only when its Java event handler is implemented and validated; creating a registry entry alone does not port the gameplay behavior.
