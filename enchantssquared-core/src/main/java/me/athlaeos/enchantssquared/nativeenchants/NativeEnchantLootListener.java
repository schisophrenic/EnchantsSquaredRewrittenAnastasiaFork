package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import me.athlaeos.enchantssquared.hooks.valhallammo.NativeLowQualityEquipmentLoot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.CreatureSpawnEvent;
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
    private static final NamespacedKey ELITE_MOB = new NamespacedKey("valhallaequipment", "elite_mob_v1");
    private final List<Enchantment> vanillaBookPool = new ArrayList<>();

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
        for (Enchantment enchant : Registry.ENCHANTMENT) {
            if (enchant.getKey().getNamespace().equals("minecraft")
                    && !enchant.getKey().getKey().startsWith("binding_curse")
                    && !enchant.getKey().getKey().startsWith("vanishing_curse")) {
                vanillaBookPool.add(enchant);
            }
        }
        EnchantsSquared.logInfo("Loaded " + pool.size()
                + " native and " + vanillaBookPool.size() + " vanilla enchanted-book candidates.");
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

    // A single roll per catch. Vanilla enchanted books are not an extra
    // independent source: replace the caught book unless this roll succeeds.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                || !(event.getCaught() instanceof Item caught)) return;
        if (chance("elite_enchanted_books.fishing_chance", 0.00002)) {
            ItemStack book = randomEliteBook();
            if (book != null) {
                caught.setItemStack(book);
                return;
            }
        }
        ItemStack gear = chanceForEquipment("fishing") ? makeEquipment("fishing") : null;
        if (gear != null) caught.setItemStack(gear);
        else if (caught.getItemStack().getType() == Material.ENCHANTED_BOOK)
            caught.setItemStack(new ItemStack(Material.COD));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNaturalChest(LootGenerateEvent event) {
        LootTable table = event.getLootTable();
        if (table == null || !table.getKey().toString().startsWith("minecraft:chests/")) return;
        List<ItemStack> items = new ArrayList<>(event.getLoot());
        // Remove pre-existing vanilla books so the rate is exactly one
        // chest-wide roll rather than multiple independent item rolls.
        items.removeIf(item -> item != null && item.getType() == Material.ENCHANTED_BOOK);
        for (ItemStack item : items) {
            enchantEquipment(item, "elite_enchanted_books.equipment_enchant_chance", 0.20);
        }
        ItemStack gear = chanceForEquipment("structure_chest") ? makeEquipment("structure_chest") : null;
        if (gear != null) items.add(gear);
        if (chance("elite_enchanted_books.chest_chance", 0.00002)) {
            ItemStack book = randomEliteBook();
            if (book != null) items.add(book);
        }
        event.setLoot(items);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEliteSpawn(CreatureSpawnEvent event) {
        LivingEntity mob = event.getEntity();
        // ValhallaEquipment assigns its elite PDC tag during its own spawn
        // handler. Wait until the tag and equipment are finalized.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!mob.isValid() || !elite(mob)) return;
            EntityEquipment equipment = mob.getEquipment();
            if (equipment == null) return;
            ItemStack[] armor = equipment.getArmorContents();
            for (ItemStack item : armor)
                enchantEquipment(item, "elite_enchanted_books.elite_equipment_enchant_chance", 0.20);
            equipment.setArmorContents(armor);
            ItemStack weapon = equipment.getItemInMainHand();
            enchantEquipment(weapon, "elite_enchanted_books.elite_equipment_enchant_chance", 0.20);
            equipment.setItemInMainHand(weapon);
        }, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobDrops(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        // Prevent multiple stacked enchanted-book drops from lower priority
        // loot handlers; normal equipment and non-book drops stay intact.
        event.getDrops().removeIf(item -> item != null && item.getType() == Material.ENCHANTED_BOOK);

        ItemStack gear = chanceForEquipment("mob_drop") ? makeEquipment("mob_drop") : null;
        if (gear != null) event.getDrops().add(gear);

        if (!elite(event.getEntity())) return;
        for (ItemStack item : event.getDrops())
            enchantEquipment(item, "elite_enchanted_books.elite_equipment_enchant_chance", 0.20);

        if (!config.getBoolean("elite_enchanted_books.enabled", true)
                || (config.getBoolean("elite_enchanted_books.require_player_kill", true)
                && event.getEntity().getKiller() == null)) return;
        if (!chance("elite_enchanted_books.chance", 0.02)) return;
        ItemStack book = randomEliteBook();
        if (book != null) event.getDrops().add(book);
    }

    private boolean elite(LivingEntity mob) {
        return Bukkit.getPluginManager().isPluginEnabled("ValhallaEquipment")
                && mob.getPersistentDataContainer()
                .getOrDefault(ELITE_MOB, PersistentDataType.BYTE, (byte) 0) != 0;
    }

    private boolean chance(String path, double fallback) {
        if (!config.getBoolean("elite_enchanted_books.enabled", true)) return false;
        double value = config.getDouble(path, fallback);
        return Double.isFinite(value) && ThreadLocalRandom.current().nextDouble()
                < Math.max(0.0, Math.min(1.0, value));
    }

    // Mutates an eligible generated piece only, never its quality, lore,
    // ValhallaEquipment model or custom persistent data.
    private void enchantEquipment(ItemStack item, String path, double defaultChance) {
        if (item == null || item.getType().isAir()
                || item.getType() == Material.ENCHANTED_BOOK || !chance(path, defaultChance)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<Enchantment> candidates = new ArrayList<>();
        for (Enchantment enchant : vanillaBookPool) {
            if (enchant.canEnchantItem(item) && compatible(enchant, meta)) candidates.add(enchant);
        }
        ConfigurationSection definitions = config.getConfigurationSection("enchantments");
        if (definitions != null) for (String id : definitions.getKeys(false)) {
            if (!definitions.getBoolean(id + ".enabled", true)
                    || id.startsWith("lesser_") || id.startsWith("advanced_")
                    || id.startsWith("supreme_")) continue;
            Enchantment enchant = Registry.ENCHANTMENT.get(new NamespacedKey("enchantssquared", id));
            if (enchant != null && enchant.canEnchantItem(item) && compatible(enchant, meta))
                candidates.add(enchant);
        }
        if (candidates.isEmpty()) return;
        Enchantment enchantment = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        meta.addEnchant(enchantment, level(enchantment), true);
        item.setItemMeta(meta);
    }

    private static boolean compatible(Enchantment enchantment, ItemMeta meta) {
        return meta.getEnchants().keySet().stream()
                .noneMatch(existing -> existing.conflictsWith(enchantment)
                        || enchantment.conflictsWith(existing)
                        || existing.equals(enchantment));
    }

    private ItemStack makeEquipment(String source) {
        if (!EnchantsSquared.isValhallaHooked()) return null;
        ItemStack item = NativeLowQualityEquipmentLoot.create(config);
        if (item == null || item.getType().isAir()) return null;
        enchantEquipment(item, "elite_enchanted_books.equipment_enchant_chance", 0.20);
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
        return book(roll.enchant());
    }

    private static ItemStack book(Enchantment enchantment) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        if (item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            meta.addStoredEnchant(enchantment, level(enchantment), true);
            item.setItemMeta(meta);
        }
        return item;
    }
}
