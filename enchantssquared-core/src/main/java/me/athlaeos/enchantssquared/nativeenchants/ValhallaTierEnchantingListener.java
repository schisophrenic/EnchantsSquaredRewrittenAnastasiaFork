package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.valhallammo.playerstats.AccumulativeStatManager;
import me.athlaeos.valhallammo.playerstats.profiles.ProfileCache;
import me.athlaeos.valhallammo.playerstats.profiles.implementations.PowerProfile;
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

/**
 * Substitutes base enchantments with the highest unlocked tier.
 *
 * Requirements:
 * 1. Enchanting QUALITY must reach the tier's configured threshold.
 * 2. ALL required ValhallaMMO perks must be unlocked.
 *
 * Higher-tier entries are NOT in the enchanting-table datapack tag. Therefore
 * they cannot roll by themselves and do not bypass perk requirements.
 */
public final class ValhallaTierEnchantingListener implements Listener {
    private record Tier(NamespacedKey enchantment, int minimumQuality, List<String> perks) {}

    private final Map<NamespacedKey, List<Tier>> families = new LinkedHashMap<>();

    public ValhallaTierEnchantingListener(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("tier_families");
        if (section == null) return;
        for (String familyId : section.getKeys(false)) {
            String path = "tier_families." + familyId;
            String baseId = config.getString(path + ".base");
            NamespacedKey base = baseId == null ? null : NamespacedKey.fromString(baseId);
            if (base == null) {
                EnchantsSquared.logWarning("Invalid tier family base: " + familyId);
                continue;
            }
            List<Tier> rules = new ArrayList<>();
            for (Map<?, ?> tier : config.getMapList(path + ".tiers")) {
                Object targetValue = tier.get("enchantment");
                NamespacedKey target = targetValue == null ? null : NamespacedKey.fromString(targetValue.toString());
                if (target == null || !target.getNamespace().equals("enchantssquared")) {
                    EnchantsSquared.logWarning("Invalid tier enchantment in " + familyId);
                    continue;
                }
                int quality = 0;
                Object rawQuality = tier.get("minimum_enchanting_quality");
                if (rawQuality instanceof Number number) quality = number.intValue();
                else if (rawQuality != null) try { quality = Integer.parseInt(rawQuality.toString()); }
                catch (NumberFormatException ignored) { continue; }

                List<String> perks = new ArrayList<>();
                Object rawPerks = tier.get("required_perks");
                if (rawPerks instanceof Collection<?> values) for (Object value : values) {
                    if (value != null && !value.toString().isBlank()) perks.add(value.toString());
                }
                // A tier without a required perk cannot accidentally become available to everybody.
                if (perks.isEmpty()) {
                    EnchantsSquared.logWarning("Tier " + target + " is missing required_perks; ignored");
                    continue;
                }
                rules.add(new Tier(target, Math.max(0, quality), List.copyOf(perks)));
            }
            rules.sort(Comparator.comparingInt(Tier::minimumQuality).reversed());
            if (!rules.isEmpty()) families.put(base, List.copyOf(rules));
        }
        EnchantsSquared.logInfo("Loaded " + families.size() + " perk-gated enchanting families");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreview(PrepareItemEnchantEvent event) {
        if (families.isEmpty()) return;
        Player player = event.getEnchanter();
        int quality = enchantingQuality(player);
        Set<String> perks = unlockedPerks(player);
        for (EnchantmentOffer offer : event.getOffers()) {
            if (offer == null) continue;
            NamespacedKey base = offer.getEnchantment().getKey();
            List<Tier> family = families.get(base);
            if (family == null) continue;
            Enchantment replacement = choose(family, quality, perks);
            if (replacement == null) continue;
            offer.setEnchantment(replacement);
            offer.setEnchantmentLevel(Math.max(1,
                    Math.min(offer.getEnchantmentLevel(), replacement.getMaxLevel())));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (families.isEmpty()) return;
        int quality = enchantingQuality(event.getEnchanter());
        Set<String> perks = unlockedPerks(event.getEnchanter());
        Map<Enchantment, Integer> rolled = event.getEnchantsToAdd();

        for (Map.Entry<NamespacedKey, List<Tier>> family : families.entrySet()) {
            Enchantment base = Registry.ENCHANTMENT.get(family.getKey());
            if (base == null) continue;

            // Handle both vanilla-roll maps and servers where changing the
            // preview offer already puts the custom enchantment into the map.
            int level = rolled.getOrDefault(base, 0);
            for (Tier tier : family.getValue()) {
                Enchantment tierEnchant = Registry.ENCHANTMENT.get(tier.enchantment());
                if (tierEnchant != null) level = Math.max(level, rolled.getOrDefault(tierEnchant, 0));
            }
            if (level <= 0) continue;
            rolled.remove(base);
            for (Tier tier : family.getValue()) {
                Enchantment tierEnchant = Registry.ENCHANTMENT.get(tier.enchantment());
                if (tierEnchant != null) rolled.remove(tierEnchant);
            }

            Enchantment replacement = choose(family.getValue(), quality, perks);
            Enchantment winner = replacement == null ? base : replacement;
            int winnerLevel = Math.max(1, Math.min(level, winner.getMaxLevel()));
            rolled.put(winner, winnerLevel);
        }
    }

    private static Enchantment choose(List<Tier> family, int quality, Set<String> perks) {
        for (Tier tier : family) {
            if (quality < tier.minimumQuality() || !perks.containsAll(tier.perks())) continue;
            Enchantment registered = Registry.ENCHANTMENT.get(tier.enchantment());
            if (registered != null) return registered;
        }
        return null;
    }

    private static int enchantingQuality(Player player) {
        double base = AccumulativeStatManager.getCachedStats("ENCHANTING_QUALITY", player, 10000, true);
        double fraction = AccumulativeStatManager.getCachedStats("ENCHANTING_FRACTION_QUALITY", player, 10000, true);
        return (int) (base * (1 + fraction));
    }

    private static Set<String> unlockedPerks(Player player) {
        PowerProfile profile = ProfileCache.getOrCache(player, PowerProfile.class);
        Set<String> result = new HashSet<>(profile.getUnlockedPerks());
        result.addAll(profile.getPermanentlyUnlockedPerks());
        result.addAll(profile.getFakeUnlockedPerks());
        return result;
    }
}
