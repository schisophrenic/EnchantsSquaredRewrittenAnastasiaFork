package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ensures all versions of an enchantment are mutually exclusive on an item.
 *
 * The order of each Valhalla tier family is always:
 * vanilla -> Lesser -> Advanced -> Supreme. A custom enchant with
 * upgrade_of extends the same evolution graph, or forms its own chain.
 *
 * When old/unsafe plugin items already contain two forms, the highest form
 * wins and inherits the highest actual enchant level (including VI+).
 * This listener never reduces another, unrelated enchant.
 */
public final class NativeEnchantmentEvolution implements Listener, Runnable {
    private record Upgrade(NamespacedKey source, NamespacedKey target, int required,
                           int result, boolean inherit, String method) {}
    private final EnchantsSquared plugin;
    private final Map<NamespacedKey, NamespacedKey> parent = new HashMap<>();
    private final Map<NamespacedKey, Integer> depthCache = new HashMap<>();
    private final Map<NamespacedKey, Upgrade> upgrades = new HashMap<>();

    public NativeEnchantmentEvolution(EnchantsSquared plugin, YamlConfiguration nativeConfig,
                                      YamlConfiguration simpleConfig) {
        this.plugin = plugin;
        ConfigurationSection families = nativeConfig.getConfigurationSection("tier_families");
        if (families != null) for (String family : families.getKeys(false)) {
            ConfigurationSection item = families.getConfigurationSection(family);
            if (item == null) continue;
            NamespacedKey previous = parse(item.getString("base"));
            if (previous == null) continue;
            NamespacedKey base = previous;
            // The family switch controls NEW tier rolls, not exclusivity:
            // existing tiered items must never stack with the vanilla base.
            for (Map<?, ?> tier : item.getMapList("tiers")) {
                NamespacedKey current = parse(String.valueOf(tier.get("enchantment")));
                if (current == null || current.equals(previous)) continue;
                parent.put(current, previous);
                if (item.getBoolean("enabled", true)) {
                    // A genuine higher-tier book promotes an earlier stage
                    // in the anvil, preserving ValhallaMMO's actual level.
                    upgrades.put(current, new Upgrade(base, current, 1, 1, true, "ANVIL"));
                }
                previous = current;
            }
        }

        ConfigurationSection custom = simpleConfig.getConfigurationSection("enchants");
        if (custom != null) for (String name : custom.getKeys(false)) {
            ConfigurationSection section = custom.getConfigurationSection(name);
            if (section == null || !section.getBoolean("enabled", true)) continue;
            NamespacedKey target = new NamespacedKey("enchantssquared", name);
            NamespacedKey source = parse(section.getString("upgrade_of"));
            if (source == null) continue;
            if (target.equals(source) || parent.containsKey(target)) {
                plugin.getLogger().warning("Invalid or ambiguous upgrade_of for " + target);
                continue;
            }
            parent.put(target, source);
            String method = section.getString("upgrade_method", "ANVIL").toUpperCase(java.util.Locale.ROOT);
            if (!"ANVIL".equals(method)) {
                plugin.getLogger().warning("Unsupported upgrade_method '" + method + "' for " + target
                        + "; use ANVIL");
                parent.remove(target);
                continue;
            }
            upgrades.put(target, new Upgrade(source, target,
                    Math.max(1, section.getInt("required_level", 1)),
                    Math.max(1, section.getInt("result_level", 1)),
                    section.getBoolean("inherit_level", false), method));
        }
        // Reject cycles instead of silently altering which enchant wins.
        for (NamespacedKey id : new HashSet<>(parent.keySet())) {
            Set<NamespacedKey> visited = new HashSet<>();
            NamespacedKey cursor = id;
            while (cursor != null && visited.add(cursor)) cursor = parent.get(cursor);
            if (cursor != null) {
                parent.remove(id);
                upgrades.remove(id);
                plugin.getLogger().warning("Cyclic enchant evolution chain at " + id + " (disabled)");
            }
        }
        plugin.getLogger().info("Loaded " + parent.size()
                + " directed enchant evolution relations (including Valhalla tiers).");
    }

    private static NamespacedKey parse(String id) {
        if (id == null || id.isBlank() || !id.contains(":")) return null;
        return NamespacedKey.fromString(id);
    }

    private NamespacedKey root(NamespacedKey id) {
        NamespacedKey cursor = id;
        Set<NamespacedKey> visited = new HashSet<>();
        while (parent.containsKey(cursor) && visited.add(cursor)) cursor = parent.get(cursor);
        return cursor;
    }

    private int rank(NamespacedKey id) {
        Integer cached = depthCache.get(id);
        if (cached != null) return cached;
        int depth = 0;
        NamespacedKey cursor = id;
        Set<NamespacedKey> visited = new HashSet<>();
        while (parent.containsKey(cursor) && visited.add(cursor)) {
            depth++;
            cursor = parent.get(cursor);
        }
        depthCache.put(id, depth);
        return depth;
    }

    private boolean sameTrack(Enchantment first, Enchantment second) {
        if (first.equals(second)) return true;
        NamespacedKey a = first.getKey();
        NamespacedKey b = second.getKey();
        return (parent.containsKey(a) || parent.containsKey(b) ||
                parent.containsValue(a) || parent.containsValue(b))
                && root(a).equals(root(b));
    }

    private void normalize(Map<Enchantment, Integer> enchants) {
        if (enchants.size() < 2) return;
        Map<NamespacedKey, List<Enchantment>> byTrack = new LinkedHashMap<>();
        for (Enchantment enchant : enchants.keySet()) {
            NamespacedKey key = enchant.getKey();
            if (!parent.containsKey(key) && !parent.containsValue(key)) continue;
            byTrack.computeIfAbsent(root(key), unused -> new ArrayList<>()).add(enchant);
        }
        for (List<Enchantment> track : byTrack.values()) {
            if (track.size() < 2) continue;
            track.sort(Comparator.comparingInt((Enchantment e) -> rank(e.getKey())).reversed()
                    .thenComparing(e -> e.getKey().toString()));
            Enchantment winner = track.get(0);
            int level = 1;
            for (Enchantment enchant : track) level = Math.max(level, enchants.getOrDefault(enchant, 0));
            for (Enchantment enchant : track) enchants.remove(enchant);
            enchants.put(winner, Math.min(255, level));
        }
    }

    /** Normalize direct and stored enchantments without replacing ItemMeta's PDC/models. */
    public boolean normalize(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        boolean changed = false;
        Map<Enchantment, Integer> direct = new LinkedHashMap<>(meta.getEnchants());
        Map<Enchantment, Integer> reduced = new LinkedHashMap<>(direct);
        normalize(reduced);
        if (!reduced.equals(direct)) {
            for (Enchantment enchantment : direct.keySet()) meta.removeEnchant(enchantment);
            for (Map.Entry<Enchantment, Integer> entry : reduced.entrySet())
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            changed = true;
        }
        if (meta instanceof EnchantmentStorageMeta stored) {
            Map<Enchantment, Integer> existing = new LinkedHashMap<>(stored.getStoredEnchants());
            Map<Enchantment, Integer> chosen = new LinkedHashMap<>(existing);
            normalize(chosen);
            if (!chosen.equals(existing)) {
                for (Enchantment enchantment : existing.keySet()) stored.removeStoredEnchant(enchantment);
                for (Map.Entry<Enchantment, Integer> entry : chosen.entrySet())
                    stored.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                changed = true;
            }
        }
        if (changed) item.setItemMeta(meta);
        return changed;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        normalize(event.getEnchantsToAdd());
        // Remove illegal old combinations after the enchantment table applies
        // its result, without changing ValhallaMMO's computed enchant levels.
        Bukkit.getScheduler().runTask(plugin, () -> normalize(event.getItem()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack left = event.getInventory().getItem(0);
        ItemStack right = event.getInventory().getItem(1);
        if (left == null || left.getType().isAir() || right == null || right.getType().isAir()) {
            if (event.getResult() != null) {
                ItemStack result = event.getResult().clone();
                if (normalize(result)) event.setResult(result);
            }
            return;
        }

        ItemStack result = event.getResult() == null ? null : event.getResult().clone();
        Map<Enchantment, Integer> rightEnchants = allEnchantments(right);
        Map<Enchantment, Integer> leftEnchants = allEnchantments(left);
        Upgrade wanted = null;
        int sourceLevel = 0;
        Enchantment target = null;

        for (Map.Entry<Enchantment, Integer> rightEntry : rightEnchants.entrySet()) {
            Upgrade candidate = upgrades.get(rightEntry.getKey().getKey());
            if (candidate == null || !"ANVIL".equals(candidate.method())) continue;
            Enchantment output = Registry.ENCHANTMENT.get(candidate.target());
            if (output == null || !output.canEnchantItem(left)) continue;
            // The original input must already have the named ancestor.
            Enchantment source = Registry.ENCHANTMENT.get(candidate.source());
            if (source == null) continue;
            int existing = leftEnchants.getOrDefault(source, 0);
            // For NG tier books, any earlier stage may be evolved without
            // requiring a separate book for every intermediate tier.
            if (candidate.inherit() && existing < candidate.required()) {
                for (var input : leftEnchants.entrySet()) {
                    if (sameTrack(input.getKey(), output)
                            && rank(input.getKey().getKey()) < rank(candidate.target()))
                        existing = Math.max(existing, input.getValue());
                }
            }
            if (existing < candidate.required()) continue;
            // Cannot replace one enchant with another that conflicts with
            // an unrelated property; do not silently delete those properties.
            boolean incompatible = false;
            for (Enchantment existingEnchant : leftEnchants.keySet()) {
                if (sameTrack(existingEnchant, output)) continue;
                if (output.conflictsWith(existingEnchant)
                        || existingEnchant.conflictsWith(output)) {
                    incompatible = true;
                    break;
                }
            }
            if (incompatible) continue;
            wanted = candidate;
            sourceLevel = existing;
            target = output;
            break;
        }

        if (wanted != null && target != null) {
            // Build directly from the *left* input so old lore/PDC, Equipment
            // texture/model and custom attributes are never discarded.
            result = left.clone();
            ItemMeta meta = result.getItemMeta();
            if (meta == null) return;
            for (Enchantment existing : new ArrayList<>(meta.getEnchants().keySet())) {
                if (sameTrack(existing, target)) meta.removeEnchant(existing);
            }
            int newLevel = wanted.inherit() ? Math.max(wanted.result(), sourceLevel)
                    : wanted.result();
            meta.addEnchant(target, Math.min(255, newLevel), true);
            result.setItemMeta(meta);
            event.getInventory().setRepairCost(Math.max(event.getInventory().getRepairCost(), 5));
        }

        if (result != null && normalize(result)) event.setResult(result);
        else if (wanted != null && result != null) event.setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSmithing(PrepareSmithingEvent event) {
        if (event.getResult() == null) return;
        ItemStack item = event.getResult().clone();
        if (normalize(item)) event.setResult(item);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> cleanPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> cleanPlayer(event.getPlayer()));
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) cleanPlayer(player);
    }

    private void cleanPlayer(Player player) {
        // This also repairs old items transferred by custom mob loot,
        // commands, creative or inventories without touching unrelated items.
        if (!player.isOnline()) return;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (normalize(item)) player.getInventory().setItem(slot, item);
        }
    }

    private static Map<Enchantment, Integer> allEnchantments(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Map.of();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Map.of();
        Map<Enchantment, Integer> result = new LinkedHashMap<>(meta.getEnchants());
        if (meta instanceof EnchantmentStorageMeta book) result.putAll(book.getStoredEnchants());
        return result;
    }
}
