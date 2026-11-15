package me.athlaeos.enchantssquared.nativeenchants;

import me.athlaeos.enchantssquared.EnchantsSquared;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Config-driven attribute enchantments, independent of the stated vanilla
 * maxLevel. Level N always uses base + (N-1) * scale_per_level.
 *
 * Vanilla Bukkit attributes are applied as transient entity modifiers, NOT
 * item-meta attribute modifiers, so they don't erase vanilla weapon damage or
 * ValhallaEquipment's crafted attribute rolls. All modifiers have stable
 * NamespacedKeys, are refreshed when equipment changes, and are cleaned up.
 *
 * ValhallaMMO stats are exposed through attributeTotal(player,"VALHALLA:KEY")
 * for NativeValhallaAttributeBridge to register into AccumulativeStatManager.
 */
public final class NativeAttributeEnchantEngine implements Runnable {
    public record Definition(Enchantment enchantment, String id, Map<String, Double> bases,
                             Map<String, Double> increments, String operation,
                             List<String> slots) {}

    private final EnchantsSquared plugin;
    private final List<Definition> definitions = new ArrayList<>();
    private final Map<String, Attribute> vanillaAttributes = new LinkedHashMap<>();
    private final Set<String> trackedModifiers = new HashSet<>();
    private static final String MOD_PREFIX = "native_attr_";

    public NativeAttributeEnchantEngine(EnchantsSquared plugin, YamlConfiguration config) {
        this.plugin = plugin;
        ConfigurationSection entries = config.getConfigurationSection("enchantments");
        if (entries == null) return;
        for (String id : entries.getKeys(false)) {
            ConfigurationSection section = entries.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) continue;
            ConfigurationSection attributes = section.getConfigurationSection("attribute");
            if (attributes == null || attributes.getKeys(false).isEmpty()) continue;
            Enchantment enchantment = Registry.ENCHANTMENT.get(new NamespacedKey("enchantssquared", id));
            if (enchantment == null) continue;
            Map<String, Double> bases = new LinkedHashMap<>();
            Map<String, Double> steps = new LinkedHashMap<>();
            ConfigurationSection perAttributeSteps = section.getConfigurationSection("scale_per_level");
            for (String raw : attributes.getKeys(false)) {
                if (!attributes.isDouble(raw) && !attributes.isInt(raw) && !attributes.isLong(raw)) {
                    EnchantsSquared.logWarning("Non-numeric native attribute " + id + "." + raw);
                    continue;
                }
                String key = raw.toUpperCase(Locale.ROOT);
                bases.put(key, attributes.getDouble(raw));
                steps.put(key, perAttributeSteps == null
                        ? section.getDouble("scale_per_level", 0.0)
                        : perAttributeSteps.getDouble(raw, 0.0));
            }
            if (bases.isEmpty()) continue;
            definitions.add(new Definition(enchantment, id, Map.copyOf(bases), Map.copyOf(steps),
                    section.getString("attribute_operation", "ADD_NUMBER").toUpperCase(Locale.ROOT),
                    List.copyOf(section.getStringList("slots"))));
        }
        EnchantsSquared.logInfo("Loaded " + definitions.size() + " native attribute-based enchantments.");
    }

    public boolean isEnabled() { return !definitions.isEmpty(); }

    public Set<String> usedValhallaStats() {
        Set<String> result = new HashSet<>();
        for (Definition def : definitions) for (String key : def.bases().keySet()) {
            if (key.startsWith("VALHALLA:")) result.add(key.substring("VALHALLA:".length()));
        }
        return result;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
    }

    public void refresh(Player player) {
        Map<String, Double> values = new HashMap<>();
        for (Definition def : definitions) {
            for (String attributeName : def.bases().keySet()) {
                if (attributeName.equals("AIR_DRAG") || attributeName.startsWith("VALHALLA:")) continue;
                Attribute attr = vanillaAttribute(attributeName);
                if (attr == null) continue;
                double value = equippedValue(player, def, attributeName);
                if (value != 0 && Double.isFinite(value)) {
                    values.merge(attributeName + "|" + def.operation(), value, Double::sum);
                }
            }
        }
        // Keep the same transient modifier when the value hasn't changed.
        // Re-applying every second would reset health/speed calculations for
        // no reason. Refresh only when players change enchanted equipment.
        trackedModifiers.addAll(values.keySet());
        for (String id : new HashSet<>(trackedModifiers)) {
            int pipe = id.lastIndexOf('|');
            if (pipe < 0) continue;
            String attributeId = id.substring(0, pipe);
            String operationName = id.substring(pipe + 1);
            Attribute attribute = vanillaAttribute(attributeId);
            if (attribute == null) continue;
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            NamespacedKey key = modifierKey(attributeId, operationName);
            AttributeModifier existing = null;
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getKey().equals(key)) {
                    existing = modifier;
                    break;
                }
            }
            double amount = Math.max(-1024, Math.min(1024, values.getOrDefault(id, 0.0)));
            if (!Double.isFinite(amount)) amount = 0;
            if (existing != null && existing.getAmount() == amount) continue;
            if (existing != null) instance.removeModifier(existing);
            if (amount == 0) continue;
            AttributeModifier.Operation op;
            try { op = AttributeModifier.Operation.valueOf(operationName); }
            catch (IllegalArgumentException ex) {
                EnchantsSquared.logWarning("Invalid native attribute operation: " + operationName);
                continue;
            }
            instance.addTransientModifier(new AttributeModifier(key, amount, op, EquipmentSlotGroup.ANY));
        }
    }

    /** Optional non-vanilla physics hook, disabled unless AIR_DRAG is used. */
    public void tickAirDrag() {
        if (definitions.isEmpty()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOnGround() || player.isFlying() || player.isInsideVehicle()) continue;
            double drag = attributeTotal(player, "AIR_DRAG");
            if (drag <= 0) continue;
            // AIR_DRAG is an explicit plugin pseudo-attribute, not a vanilla
            // Attribute enum; only affects airborne horizontal velocity.
            double fraction = Math.max(0, Math.min(0.25, drag));
            var velocity = player.getVelocity();
            velocity.setX(velocity.getX() * (1.0 - fraction));
            velocity.setZ(velocity.getZ() * (1.0 - fraction));
            player.setVelocity(velocity);
        }
    }

    public double attributeTotal(Player player, String attrName) {
        double value = 0.0;
        for (Definition def : definitions) {
            if (!def.bases().containsKey(attrName)) continue;
            value += equippedValue(player, def, attrName);
        }
        return Double.isFinite(value) ? value : 0.0;
    }

    private double equippedValue(Player player, Definition def, String key) {
        double sum = 0;
        var equipment = player.getEquipment();
        if (equipment == null) return 0;
        if (def.slots().contains("armor") || def.slots().contains("any")) {
            for (ItemStack item : equipment.getArmorContents()) {
                sum += itemValue(item, def, key);
            }
        }
        if (def.slots().contains("mainhand") || def.slots().contains("hand")
                || def.slots().contains("any")) {
            sum += itemValue(equipment.getItemInMainHand(), def, key);
        }
        if (def.slots().contains("offhand") || def.slots().contains("hand")
                || def.slots().contains("any")) {
            sum += itemValue(equipment.getItemInOffHand(), def, key);
        }
        return sum;
    }

    private double itemValue(ItemStack item, Definition def, String key) {
        if (item == null || item.getType().isAir()) return 0;
        int level = item.getEnchantmentLevel(def.enchantment());
        if (level < 1) return 0;
        // Never clamp to def.enchantment().getMaxLevel(). ValhallaMMO can
        // amplify e.g. Lifesteal V despite the base max III.
        return def.bases().getOrDefault(key, 0.0)
                + ((double) level - 1.0) * def.increments().getOrDefault(key, 0.0);
    }

    public void clean() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (String id : trackedModifiers) {
                int pipe = id.lastIndexOf('|');
                if (pipe < 0) continue;
                Attribute attr = vanillaAttribute(id.substring(0, pipe));
                AttributeInstance inst = attr == null ? null : player.getAttribute(attr);
                if (inst == null) continue;
                NamespacedKey key = modifierKey(id.substring(0, pipe), id.substring(pipe + 1));
                for (AttributeModifier modifier : new ArrayList<>(inst.getModifiers())) {
                    if (modifier.getKey().equals(key)) inst.removeModifier(modifier);
                }
            }
        }
    }

    private NamespacedKey modifierKey(String attribute, String operation) {
        String raw = attribute.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_")
                + "_" + operation.toLowerCase(Locale.ROOT);
        return new NamespacedKey(plugin, MOD_PREFIX + raw);
    }

    private Attribute vanillaAttribute(String raw) {
        if (vanillaAttributes.containsKey(raw)) return vanillaAttributes.get(raw);
        String name = raw;
        if (!raw.startsWith("GENERIC_") && !raw.startsWith("PLAYER_")
                && !raw.startsWith("ZOMBIE_") && !raw.startsWith("HORSE_")
                && !raw.startsWith("ARMOR_") && !raw.startsWith("ATTACK_")) {
            name = "GENERIC_" + raw;
        }
        Attribute attr = null;
        try { attr = Attribute.valueOf(name); }
        catch (IllegalArgumentException ignored) {
            try { attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(raw.toLowerCase(Locale.ROOT))); }
            catch (RuntimeException alsoIgnored) { }
        }
        if (attr != null) vanillaAttributes.put(raw, attr);
        return attr;
    }
}
