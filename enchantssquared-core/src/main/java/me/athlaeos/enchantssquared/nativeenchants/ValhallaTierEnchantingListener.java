package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
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
    private record Band(NamespacedKey enchantment, int ng, int minimumQuality, List<String> perks) {}
    private record Family(NamespacedKey base, List<String> perks, List<Band> bands) {}
    private record Choice(Enchantment enchantment, int level) {}
    private record Bonus(NamespacedKey enchantment, List<String> perks, int quality,
                         int requiredNg, double chance, int baseLevel, int qualityStep) {}

    private final Map<NamespacedKey, Family> families = new LinkedHashMap<>();
    private final List<Bonus> bonusEnchants = new ArrayList<>();
    private final int maximumBonusEnchants;

    public ValhallaTierEnchantingListener(YamlConfiguration config) {
        maximumBonusEnchants = Math.max(0, Math.min(10, config.getInt("settings.maximum_bonus_enchantments", 1)));
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
                    // Stage threshold alone selects the tier; the actual
                    // enchant level comes from ValhallaMMO (including VI+).
                    bands.add(new Band(id, ng, minimum, tierPerks));
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

        for (EnchantmentOffer offer : event.getOffers()) {
            if (offer == null) continue;
            for (Family family : families.values()) {
                if (!matchesFamily(offer.getEnchantment(), family)) continue;
                // ValhallaMMO already amplified the vanilla offer according to
                // its own chance, scaling formula and perk bonuses. Preserve
                // that FULL level when substituting a tiered enchant.
                Choice replacement = choose(family, quality, ng, perks, offer.getEnchantmentLevel());
                if (replacement != null) {
                    offer.setEnchantment(replacement.enchantment());
                    offer.setEnchantmentLevel(replacement.level());
                } else {
                    Enchantment base = Registry.ENCHANTMENT.get(family.base());
                    if (base != null) {
                        offer.setEnchantment(base);
                        // Keep ValhallaMMO's amplified vanilla level (including VI+).
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

            // ValhallaMMO has already scaled the original vanilla result.
            // Do not scale again and do not cap the final result at V.
            int incomingLevel = rolls.getOrDefault(base, 1);
            for (Band band : family.bands()) {
                Enchantment nativeEnchantment = Registry.ENCHANTMENT.get(band.enchantment());
                if (nativeEnchantment != null) {
                    incomingLevel = Math.max(incomingLevel, rolls.getOrDefault(nativeEnchantment, 0));
                }
            }
            Choice chosen = choose(family, quality, ng, perks, incomingLevel);
            // If no tier is unlocked, retain the vanilla enchantment exactly
            // as ValhallaMMO calculated it, including its custom level bonus.
            if (chosen == null && rolls.containsKey(base)) {
                removeTierAlternatives(rolls, family);
                // ValhallaMMO adds its perk bonuses after applying the
                // scaling's configured upper bound. Enforce vanilla V here.
                // Retain the vanilla scaled level, even if it exceeds V.
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
                rolls.put(base, previousLevel);
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

    /**
     * Native registry maxLevel is V, like vanilla. It limits ordinary
     * enchanting and /enchant usage; it is NOT the maximum effective level
     * after ValhallaMMO's scaling or level-boosting perks.
     *
     * Tier eligibility depends on NG and effective enchanting quality.
     * Actual enchantment LEVEL comes from the original vanilla enchant
     * roll, after ValhallaMMO has scaled it. Do not amplify it twice.
     */
    private static Choice choose(Family family, int quality, int ng,
                                 Set<String> perks, int scaledVanillaLevel) {
        if (!perks.containsAll(family.perks())) return null;
        for (Band band : family.bands()) {
            if (ng < band.ng() || quality < band.minimumQuality() ||
                    !perks.containsAll(band.perks())) continue;
            Enchantment registered = Registry.ENCHANTMENT.get(band.enchantment());
            if (registered == null) continue;
            // Minecraft enchantment levels are supported through 255 on items.
            return new Choice(registered, Math.max(1, Math.min(255, scaledVanillaLevel)));
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
