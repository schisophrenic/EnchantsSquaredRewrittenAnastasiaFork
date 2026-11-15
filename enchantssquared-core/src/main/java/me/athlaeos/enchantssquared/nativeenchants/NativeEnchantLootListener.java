package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.enchantssquared.hooks.valhallammo.NativeLowQualityEquipmentLoot;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.loot.LootTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Configured rare drops from real vanilla loot generation/trading/fishing.
 * Uses native Paper enchants, never duplicates the existing legacy ES
 * enchantment handlers or the ValhallaMMO tier substitution listener.
 *
 * Rarity is a selection weight; acquisition keys are per-event probabilities.
 * Source chances may be 0, and this does not require Smithing perks.
 */
public final class NativeEnchantLootListener implements Listener {
    private record Entry(Enchantment enchant, String rarity, Map<String, Double> chance) {}
    private final EnchantsSquared plugin;
    private final List<Entry> pool = new ArrayList<>();
    private final YamlConfiguration config;

    public NativeEnchantLootListener(EnchantsSquared plugin, YamlConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        var all = config.getConfigurationSection("enchantments");
        if (all == null) return;
        for (String id : all.getKeys(false)) {
            ConfigurationSection section = all.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) continue;
            ConfigurationSection acq = section.getConfigurationSection("acquisition");
            if (acq == null) continue;
            Enchantment enchantment = Registry.ENCHANTMENT.get(new NamespacedKey("enchantssquared", id));
            if (enchantment == null) continue;
            java.util.Map<String, Double> chances = new java.util.HashMap<>();
            for (String source : List.of("enchanting", "villager_trade", "fishing",
                    "structure_chest", "mob_drop")) {
                chances.put(source, Math.max(0, Math.min(1, acq.getDouble(source, 0))));
            }
            pool.add(new Entry(enchantment,
                    section.getString("rarity", "common").toLowerCase(Locale.ROOT), Map.copyOf(chances)));
        }
        EnchantsSquared.logInfo("Loaded " + pool.size() + " random native enchantment loot candidates.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        // Additional independent roll. Never grant a tier without Valhalla's
        // NG requirements; tier IDs are intentionally not acquisition entries.
        if (pool.isEmpty()) return;
        Entry roll = choose("enchanting", event.getItem());
        if (roll == null) return;
        if (event.getEnchantsToAdd().keySet().stream().anyMatch(roll.enchant()::conflictsWith)) return;
        if (event.getItem().getEnchantments().keySet().stream().anyMatch(roll.enchant()::conflictsWith)) return;
        event.getEnchantsToAdd().putIfAbsent(roll.enchant(), level(roll.enchant()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTrade(VillagerAcquireTradeEvent event) {
        MerchantRecipe old = event.getRecipe();
        if (old.getResult().getType() != Material.ENCHANTED_BOOK) return;
        Entry roll = choose("villager_trade", old.getResult());
        if (roll == null) return;
        ItemStack output = book(roll);
        MerchantRecipe modified = new MerchantRecipe(output, old.getMaxUses());
        modified.setIngredients(old.getIngredients());
        modified.setExperienceReward(old.hasExperienceReward());
        modified.setVillagerExperience(old.getVillagerExperience());
        modified.setPriceMultiplier(old.getPriceMultiplier());
        modified.setDemand(old.getDemand());
        modified.setSpecialPrice(old.getSpecialPrice());
        event.setRecipe(modified);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                || !(event.getCaught() instanceof Item caught)) return;
        ItemStack generatedGear = chanceForEquipment("fishing")
                ? makeEquipment("fishing") : null;
        if (generatedGear != null) {
            caught.setItemStack(generatedGear);
            return;
        }
        Entry roll = choose("fishing", caught.getItemStack());
        if (roll != null) caught.setItemStack(book(roll));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNaturalChest(LootGenerateEvent event) {
        LootTable table = event.getLootTable();
        if (table == null || !table.getKey().toString().startsWith("minecraft:chests/")) return;
        List<ItemStack> items = new ArrayList<>(event.getLoot());
        ItemStack gear = chanceForEquipment("structure_chest") ? makeEquipment("structure_chest") : null;
        if (gear != null) items.add(gear);
        Entry roll = choose("structure_chest", new ItemStack(Material.ENCHANTED_BOOK));
        if (roll != null) items.add(book(roll));
        if (gear != null || roll != null) event.setLoot(items);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDrops(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        ItemStack gear = chanceForEquipment("mob_drop") ? makeEquipment("mob_drop") : null;
        if (gear != null) event.getDrops().add(gear);
        Entry roll = choose("mob_drop", new ItemStack(Material.ENCHANTED_BOOK));
        if (roll != null) event.getDrops().add(book(roll));
    }

    private ItemStack makeEquipment(String source) {
        if (!EnchantsSquared.isValhallaHooked()) return null;
        ItemStack item = NativeLowQualityEquipmentLoot.create(config);
        if (item == null || item.getType().isAir()) return null;

        // Optional enchanted natural gear, after ValhallaEquipment has rolled
        // its native quality/class/model/stats. Keep PDC and item model intact.
        double chance = Math.max(0.0, Math.min(1.0,
                config.getDouble("loot_equipment.native_enchant_chance", 0.20)));
        if (ThreadLocalRandom.current().nextDouble() >= chance) return item;

        // Only draw from this source's native enchant loot pool, not NG tiered
        // enchantments (those have no acquisition entries by default).
        Entry roll = chooseCompatible(source, item);
        if (roll == null) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !roll.enchant().canEnchantItem(item)) return item;
        for (Enchantment existing : meta.getEnchants().keySet()) {
            if (roll.enchant().conflictsWith(existing) || existing.conflictsWith(roll.enchant())) return item;
        }
        meta.addEnchant(roll.enchant(), level(roll.enchant()), true);
        item.setItemMeta(meta);
        return item;
    }

    private Entry chooseCompatible(String source, ItemStack item) {
        List<Entry> eligible = new ArrayList<>();
        for (Entry entry : pool) {
            if (!entry.enchant().canEnchantItem(item)) continue;
            double chance = entry.chance().getOrDefault(source, 0.0);
            if (chance <= 0) continue;
            if (ThreadLocalRandom.current().nextDouble() < chance) eligible.add(entry);
        }
        if (eligible.isEmpty()) return null;
        double totalWeight = 0;
        for (Entry entry : eligible) totalWeight += rarityWeight(entry.rarity());
        double roll = ThreadLocalRandom.current().nextDouble() * totalWeight;
        for (Entry entry : eligible) {
            roll -= rarityWeight(entry.rarity());
            if (roll <= 0) return entry;
        }
        return eligible.get(eligible.size() - 1);
    }

    private boolean chanceForEquipment(String source) {
        if (!config.getBoolean("loot_equipment.enabled", false)) return false;
        double value = config.getDouble("loot_equipment.acquisition." + source, 0);
        return ThreadLocalRandom.current().nextDouble() < Math.max(0, Math.min(1, value));
    }

    private Entry choose(String source, ItemStack target) {
        if (pool.isEmpty()) return null;
        List<Entry> hits = new ArrayList<>();
        for (Entry entry : pool) {
            double chance = entry.chance().getOrDefault(source, 0.0);
            if (chance <= 0) continue;
            if (target.getType() != Material.BOOK && target.getType() != Material.ENCHANTED_BOOK
                    && !entry.enchant().canEnchantItem(target) && source.equals("enchanting")) continue;
            if (ThreadLocalRandom.current().nextDouble() < chance) hits.add(entry);
        }
        if (hits.isEmpty()) return null;
        double weight = 0;
        for (Entry entry : hits) weight += rarityWeight(entry.rarity());
        double selected = ThreadLocalRandom.current().nextDouble() * weight;
        for (Entry entry : hits) {
            selected -= rarityWeight(entry.rarity());
            if (selected <= 0) return entry;
        }
        return hits.get(hits.size() - 1);
    }

    private static double rarityWeight(String rarity) {
        return switch (rarity) {
            case "legendary" -> 0.05;
            case "epic" -> 0.20;
            case "rare" -> 0.50;
            case "uncommon" -> 0.75;
            default -> 1.0;
        };
    }

    private static int level(Enchantment enchantment) {
        return ThreadLocalRandom.current().nextInt(1,
                Math.max(1, enchantment.getMaxLevel()) + 1);
    }

    private static ItemStack book(Entry roll) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        if (item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            meta.addStoredEnchant(roll.enchant(), level(roll.enchant()), true);
            item.setItemMeta(meta);
        }
        return item;
    }
}
