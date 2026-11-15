package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.enchantssquared.config.ConfigManager;
import me.athlaeos.valhallammo.item.EnchantingItemPropertyManager;
import me.athlaeos.valhallammo.item.EnchantmentClassification;
import me.athlaeos.valhallammo.playerstats.profiles.implementations.EnchantingProfile;
import me.athlaeos.valhallammo.playerstats.AccumulativeStatManager;
import me.athlaeos.valhallammo.playerstats.profiles.ProfileCache;
import me.athlaeos.valhallammo.playerstats.profiles.implementations.PowerProfile;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Player-specific replacement of ordinary enchants with NG-bound levels and
 * optional perk-bound special enchantment bonus rolls.
 *
 * Source tiers are never added to the regular global enchanting-table tag.
 * This listener checks eligibility on BOTH preview and final item enchant.
 */
public final class ValhallaTierEnchantingListener implements Listener {
    private record Band(NamespacedKey enchantment, int ng, int minimumQuality,
                        int qualityStep, int firstLevel, int maxLevel, List<String> perks) {}
    private record Family(NamespacedKey base, List<String> perks, List<Band> bands) {}
    private record Choice(Enchantment enchantment, int level) {}
    private record Bonus(NamespacedKey enchantment, List<String> perks, int quality,
                         int requiredNg, double chance, int baseLevel, int qualityStep) {}

    private final Map<NamespacedKey, Family> families = new LinkedHashMap<>();
    private final List<Bonus> bonusEnchants = new ArrayList<>();
    private final int maximumBonusEnchants;
    private final double perkQualityPerBonus;
    private final boolean nativeScalingsEnabled;

    public ValhallaTierEnchantingListener(YamlConfiguration config) {
        maximumBonusEnchants = Math.max(0, Math.min(10, config.getInt("settings.maximum_bonus_enchantments", 1)));
        YamlConfiguration valhallaBridge = ConfigManager.getInstance()
                .getConfig("config_native_enchanting.yml").get();
        perkQualityPerBonus = Math.max(0.0, Math.min(50.0,
                valhallaBridge.getDouble("tier_perk_quality_per_bonus", 5.0)));
        nativeScalingsEnabled = valhallaBridge.getBoolean("native_scalings_enabled", true);
        ConfigurationSection sections = config.getConfigurationSection("tier_families");
        if (sections != null) for (String name : sections.getKeys(false)) {
            String prefix = "tier_families." + name;
            NamespacedKey base = parse(config.getString(prefix + ".base"));
            if (base == null) {
                EnchantsSquared.logWarning("Invalid base for native enchanting family " + name);
                continue;
            }
            List<Band> bands = new ArrayList<>();
            for (Map<?, ?> tier : config.getMapList(prefix + ".tiers")) {
                NamespacedKey id = parse(Objects.toString(tier.get("enchantment"), ""));
                if (id == null) continue;
                List<String> tierPerks = asStrings(tier.get("required_perks"));
                Object stages = tier.get("progression");
                if (!(stages instanceof List<?> list)) continue;

                for (Object stage : list) {
                    if (!(stage instanceof Map<?, ?> row)) continue;
                    int ng = Math.max(1, number(row.get("required_ng"), 1));
                    int minimum = Math.max(0, number(row.get("quality_at_first_level"), 0));
                    int step = Math.max(1, number(row.get("quality_per_level"), 1));
                    int first = Math.max(1, number(row.get("first_level"), 1));
                    int cap = Math.max(first, number(row.get("max_level"), first));
                    if (id.getKey().matches("(lesser|advanced|supreme)_(sharpness|smite|bane_of_arthropods)")) {
                        // Migrate old tier progression I–X / XI–XXX on the fly.
                        // Existing configs remain valid and are never rewritten.
                        // Unlock stage, quality required, and the five-level
                        // ceiling are authoritative in the new compact system.
                        if (cap > 5 || first > 5) {
                            first = (ng == 2 || ng == 4) ? 4 : 1;
                            cap = (ng == 1 || ng == 3) ? 3 : 5;
                            step = switch (ng) {
                                case 1 -> 90;
                                case 2 -> 228;
                                case 3 -> 99;
                                case 4 -> 209;
                                default -> 88;
                            };
                        } else {
                            first = Math.min(5, first);
                            cap = Math.min(5, cap);
                        }
                    }
                    bands.add(new Band(id, ng, minimum, step, first, cap, tierPerks));
                }
            }
            // Supreme/Advanced first; for each tier the highest available NG band wins.
            bands.sort(Comparator.comparingInt(Band::ng).reversed());
            if (!bands.isEmpty()) families.put(base, new Family(base, config.getStringList(prefix + ".required_perks"), bands));
        }

        ConfigurationSection perkSection = config.getConfigurationSection("perk_enchantments");
        if (perkSection != null) for (String name : perkSection.getKeys(false)) {
            ConfigurationSection item = perkSection.getConfigurationSection(name);
            if (item == null || !item.getBoolean("enabled", true)) continue;
            NamespacedKey id = parse(item.getString("enchantment"));
            List<String> perks = new ArrayList<>(item.getStringList("required_perks"));
            String group = item.getString("perk_group");
            if (group != null && !group.isBlank()) {
                String groupPerk = config.getString("perk_groups." + group);
                if (groupPerk == null || groupPerk.isBlank()) {
                    EnchantsSquared.logWarning("Unknown perk_group '" + group + "' in " + name + "; enchantment disabled");
                    continue;
                }
                perks.add(groupPerk);
            }
            if (id == null || perks.isEmpty()) {
                EnchantsSquared.logWarning("Perk-enchantment '" + name + "' needs an enchantment ID and required_perks");
                continue;
            }
            bonusEnchants.add(new Bonus(id, List.copyOf(perks),
                    item.getInt("minimum_enchanting_quality", 0), item.getInt("minimum_ng", 0),
                    Math.max(0, Math.min(1, item.getDouble("chance", 0))),
                    Math.max(1, item.getInt("fixed_level", 1)),
                    Math.max(0, item.getInt("quality_per_level", 0))));
        }

        EnchantsSquared.logInfo("Loaded " + families.size() + " tier families and "
                + bonusEnchants.size() + " perk-only enchantment rolls.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreview(PrepareItemEnchantEvent event) {
        if (families.isEmpty()) return;
        Player player = event.getEnchanter();
        int quality = enchantingQuality(player);
        Set<String> perks = unlockedPerks(player);
        int ng = ngLevel(perks);
        EnchantingProfile enchantingProfile = ProfileCache.getOrCache(player, EnchantingProfile.class);

        for (EnchantmentOffer offer : event.getOffers()) {
            if (offer == null) continue;
            for (Family family : families.values()) {
                if (!matchesFamily(offer.getEnchantment(), family)) continue;
                Choice replacement = choose(family, quality, ng, perks, enchantingProfile);
                if (replacement != null) {
                    offer.setEnchantment(replacement.enchantment());
                    offer.setEnchantmentLevel(replacement.level());
                } else {
                    Enchantment base = Registry.ENCHANTMENT.get(family.base());
                    if (base != null) {
                        offer.setEnchantment(base);
                        offer.setEnchantmentLevel(Math.min(5, offer.getEnchantmentLevel()));
                    }
                }
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        int quality = enchantingQuality(player);
        Set<String> perks = unlockedPerks(player);
        int ng = ngLevel(perks);
        EnchantingProfile enchantingProfile = ProfileCache.getOrCache(player, EnchantingProfile.class);
        Map<Enchantment, Integer> rolls = event.getEnchantsToAdd();

        for (Family family : families.values()) {
            Enchantment base = Registry.ENCHANTMENT.get(family.base());
            if (base == null) continue;
            boolean exists = rolls.containsKey(base);
            for (Band band : family.bands()) {
                Enchantment e = Registry.ENCHANTMENT.get(band.enchantment());
                if (e != null && rolls.containsKey(e)) exists = true;
            }
            if (!exists) continue;

            Choice chosen = choose(family, quality, ng, perks, enchantingProfile);
            // If no tier is unlocked, retain the vanilla enchantment exactly
            // as ValhallaMMO calculated it, including its custom level bonus.
            if (chosen == null && rolls.containsKey(base)) {
                removeTierAlternatives(rolls, family);
                // ValhallaMMO adds its perk bonuses after applying the
                // scaling's configured upper bound. Enforce vanilla V here.
                rolls.put(base, Math.min(5, rolls.get(base)));
                continue;
            }

            int previousLevel = rolls.getOrDefault(base, 1);
            for (Band band : family.bands()) {
                Enchantment tier = Registry.ENCHANTMENT.get(band.enchantment());
                if (tier != null) previousLevel = Math.max(previousLevel, rolls.getOrDefault(tier, 0));
            }
            rolls.remove(base);
            removeTierAlternatives(rolls, family);
            if (chosen == null) {
                rolls.put(base, Math.min(5, previousLevel));
            } else {
                rolls.put(chosen.enchantment(), chosen.level());
            }
        }

        int added = 0;
        for (Bonus bonus : bonusEnchants) {
            if (added >= maximumBonusEnchants) break;
            if (ng < bonus.requiredNg() || quality < bonus.quality() || !perks.containsAll(bonus.perks())) continue;
            Enchantment enchantment = Registry.ENCHANTMENT.get(bonus.enchantment());
            if (enchantment == null || rolls.containsKey(enchantment)) continue;
            Material material = event.getItem().getType();
            if (material != Material.BOOK && material != Material.ENCHANTED_BOOK
                    && !enchantment.canEnchantItem(event.getItem())) continue;
            if (rolls.keySet().stream().anyMatch(enchantment::conflictsWith)) continue;
            if (event.getItem().getEnchantments().keySet().stream().anyMatch(enchantment::conflictsWith)) continue;
            if (ThreadLocalRandom.current().nextDouble() >= bonus.chance()) continue;

            int level = bonus.baseLevel();
            if (bonus.qualityStep() > 0) level += (quality - bonus.quality()) / bonus.qualityStep();
            rolls.put(enchantment, Math.max(1, Math.min(enchantment.getMaxLevel(), level)));
            added++;
        }
    }

    private Choice choose(Family family, int quality, int ng, Set<String> perks,
                          EnchantingProfile enchantingProfile) {
        if (!perks.containsAll(family.perks())) return null;
        for (Band band : family.bands()) {
            if (ng < band.ng() || quality < band.minimumQuality() || !perks.containsAll(band.perks())) continue;
            Enchantment registered = Registry.ENCHANTMENT.get(band.enchantment());
            if (registered == null) continue;

            Enchantment vanilla = Registry.ENCHANTMENT.get(family.base());
            int sourceBonus = vanilla == null ? 0
                    : enchantingProfile.getEnchantmentBonus(vanilla)
                        + enchantingProfile.getEnchantmentBonus(EnchantmentClassification.OFFENSIVE);
            // Express generic +specific Valhalla perks as a modest quality
            // improvement instead of instantly adding multiple full levels.
            // A fresh NG unlock still cannot jump to the maximum level.
            int bonusQuality = (int) Math.floor(Math.max(0, sourceBonus) * perkQualityPerBonus);
            long adjustedQuality = (long) quality + bonusQuality;
            int bonusLevels = (int) Math.min(Integer.MAX_VALUE,
                    (adjustedQuality - band.minimumQuality()) / band.qualityStep());
            int level = band.firstLevel() + bonusLevels;
            // Native Enchantment instances are registered through Paper bootstrap.
            // Use ValhallaMMO's public scaling registry for their NG-band levels,
            // preserving the previous approved quality progression by normalizing
            // rating to 100 units per level before invoking Scaling.evaluate().
            if (nativeScalingsEnabled && EnchantingItemPropertyManager.getScaling(registered) != null) {
                int normalized = (int) Math.min(Integer.MAX_VALUE,
                        ((adjustedQuality - band.minimumQuality()) * 100L) / band.qualityStep());
                level = EnchantingItemPropertyManager.getScaledLevel(registered, normalized, band.firstLevel());
            }

            // An NG milestone and its required quality remain a strict ceiling.
            level = Math.max(band.firstLevel(), Math.min(level, band.maxLevel()));
            return new Choice(registered, Math.min(level, registered.getMaxLevel()));
        }
        return null;
    }

    private static void removeTierAlternatives(Map<Enchantment, Integer> rolls, Family family) {
        for (Band band : family.bands()) {
            Enchantment tier = Registry.ENCHANTMENT.get(band.enchantment());
            if (tier != null) rolls.remove(tier);
        }
    }

    private static boolean matchesFamily(Enchantment enchantment, Family family) {
        if (enchantment.getKey().equals(family.base())) return true;
        for (Band band : family.bands()) {
            if (enchantment.getKey().equals(band.enchantment())) return true;
        }
        return false;
    }

    private static NamespacedKey parse(String id) {
        if (id == null || id.isBlank()) return null;
        return NamespacedKey.fromString(id);
    }

    private static int number(Object raw, int fallback) {
        if (raw instanceof Number n) return n.intValue();
        try { return Integer.parseInt(Objects.toString(raw)); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static List<String> asStrings(Object raw) {
        if (!(raw instanceof Collection<?> values)) return List.of();
        List<String> strings = new ArrayList<>();
        for (Object value : values) if (value != null && !value.toString().isBlank()) strings.add(value.toString());
        return List.copyOf(strings);
    }

    private static int enchantingQuality(Player player) {
        double base = AccumulativeStatManager.getCachedStats("ENCHANTING_QUALITY", player, 10000, true);
        double fraction = AccumulativeStatManager.getCachedStats("ENCHANTING_FRACTION_QUALITY", player, 10000, true);
        return Math.max(0, (int) (base * (1 + fraction)));
    }

    private static Set<String> unlockedPerks(Player player) {
        PowerProfile profile = ProfileCache.getOrCache(player, PowerProfile.class);
        Set<String> result = new HashSet<>(profile.getUnlockedPerks());
        result.addAll(profile.getPermanentlyUnlockedPerks());
        result.addAll(profile.getFakeUnlockedPerks());
        return result;
    }

    private static int ngLevel(Set<String> perks) {
        for (int ng = 6; ng >= 1; ng--) {
            if (perks.contains("enchanting_perk_ng" + ng)) return ng;
        }
        return 0;
    }
}
