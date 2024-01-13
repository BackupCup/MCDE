package net.backupcup.mcde.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.backupcup.mcde.MCDEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class EnchantmentSlots implements Iterable<EnchantmentSlot> {
    public static final String ENCHANTMENT_SLOTS_KEY = "MCDEnchantments";
    private Map<SlotPosition, EnchantmentSlot> slots;
    private Optional<Identifier> gilding;
    private int nextRerollCost;
    private int nextRerollCostPowerful;

    public EnchantmentSlots(Map<SlotPosition, EnchantmentSlot> slots, Optional<Identifier> gilding, int nextRerollCost, int nextRerollCostPowerful) {
        this.slots = slots;
        this.gilding = gilding;
        this.nextRerollCost = nextRerollCost;
        this.nextRerollCostPowerful = nextRerollCostPowerful;
    }

    public EnchantmentSlots(Map<SlotPosition, EnchantmentSlot> slots) {
        this(
            slots,
            Optional.empty(),
            MCDEnchantments.getConfig().getRerollCostParameters().getNormal().getStartCost(),
            MCDEnchantments.getConfig().getRerollCostParameters().getPowerful().getStartCost()
        );
    }

    public boolean hasGilding() {
        return gilding.isPresent();
    }

    public Optional<Identifier> getGilding() {
        return gilding;
    }

    public void setGilding(Identifier gilding) {
        this.gilding = Optional.of(gilding);
    }

    public void removeGilding() {
        gilding = Optional.empty();
    }

    public int getNextRerollCost() {
        return nextRerollCost;
    }

    public void setNextRerollCost(int nextRerollCost) {
        this.nextRerollCost = nextRerollCost;
    }

    public int getNextRerollCostPowerful() {
        return nextRerollCostPowerful;
    }

    public void setNextRerollCostPowerful(int nextRerollCostPowerful) {
        this.nextRerollCostPowerful = nextRerollCostPowerful;
    }

    public int getNextRerollCost(Identifier id) {
        return MCDEnchantments.getConfig().isEnchantmentPowerful(id) ? nextRerollCostPowerful : nextRerollCost;
    }

    public static class Builder {
        private Map<SlotPosition, EnchantmentSlot> slots = new EnumMap<>(SlotPosition.class);

        public Builder withSlot(SlotPosition slot, Identifier first) {
            slots.put(slot, EnchantmentSlot.of(slot, first));
            return this;
        }

        public Builder withSlot(SlotPosition slot, Identifier first, Identifier second) {
            slots.put(slot, EnchantmentSlot.of(slot, first, second));
            return this;
        }

        public Builder withSlot(SlotPosition slot, Identifier first, Identifier second, Identifier third) {
            slots.put(slot, EnchantmentSlot.of(slot, first, second, third));
            return this;
        }

        public List<Choice> getAdded() {
            var added = new ArrayList<Choice>();
            for (var slot : slots.values()) {
                added.addAll(slot.choices());
            }
            return added;
        }

        public Optional<EnchantmentSlot> getSlot(SlotPosition pos) {
            return Optional.ofNullable(slots.get(pos));
        }

        public EnchantmentSlots build() {
            return new EnchantmentSlots(Collections.unmodifiableMap(slots));
        }
    }

    public static EnchantmentSlots EMPTY = new EnchantmentSlots(Map.of(), Optional.empty(), 0, 0);

    public static Builder builder() {
        return new Builder();
    }

    public Optional<EnchantmentSlot> getEnchantmentSlot(SlotPosition pos) {
        return Optional.ofNullable(slots.get(pos));
    }

    @Override
    public String toString() {
        return slots.toString();
    }

    public NbtCompound toNbt() {
        NbtCompound root = new NbtCompound();
        NbtCompound slotsCompound = new NbtCompound();
        slots.entrySet().stream()
            .forEach(kvp -> slotsCompound.put(kvp.getKey().name(), kvp.getValue().toNbt()));
        root.put("Slots", slotsCompound);
        gilding.ifPresent(id -> root.putString("Gilding", id.toString()));
        NbtCompound rerollCost = new NbtCompound();
        rerollCost.putInt("Normal", nextRerollCost);
        rerollCost.putInt("Powerful", nextRerollCostPowerful);
        root.put("NextRerollCost", rerollCost);
        return root;
    }

    public void putChosenEnchantments(ItemStack itemStack) {
        var enchantments = EnchantmentHelper.get(itemStack);
        enchantments.putAll(slots.values().stream()
                .flatMap(slot -> slot.getChosen().stream())
                .collect(Collectors.toMap(Choice::getEnchantment, Choice::getLevel)));
        gilding.ifPresent(id -> enchantments.put(EnchantmentUtils.getEnchantment(id), 1));
        EnchantmentHelper.set(enchantments, itemStack);
    }

    public void removeChosenEnchantments(ItemStack itemStack) {
        var enchantments = EnchantmentHelper.get(itemStack);
        var chosen = slots.values().stream()
            .flatMap(slot -> slot.getChosen().map(c -> c.getEnchantment()).stream())
            .toList();
        enchantments.entrySet().removeIf(kvp -> chosen.contains(kvp.getKey()));
        EnchantmentHelper.set(enchantments, itemStack);
    }

    public static EnchantmentSlots fromNbt(NbtCompound nbt, Map<Enchantment, Integer> enchantments) {
        if (nbt == null) {
            return null;
        }
        var slots = nbt.getCompound("Slots");
        return new EnchantmentSlots(slots.getKeys().stream()
                    .map(key -> EnchantmentSlot.fromNbt(slots.getCompound(key), SlotPosition.valueOf(key), enchantments))
                    .filter(slot -> slot.getChosen().filter(c -> c.getLevel() == 0).isEmpty())
                    .collect(Collectors.toMap(EnchantmentSlot::getSlotPosition, Function.identity())),
                Optional.ofNullable(Identifier.tryParse(nbt.getString("Gilding")))
                    .filter(id -> enchantments.containsKey(EnchantmentUtils.getEnchantment(id))),
                nbt.getCompound("NextRerollCost").getInt("Normal"),
                nbt.getCompound("NextRerollCost").getInt("Powerful"));
    }

    public static Optional<EnchantmentSlots> fromItemStack(ItemStack itemStack) {
        return Optional.ofNullable(fromNbt(itemStack.getSubNbt(ENCHANTMENT_SLOTS_KEY), EnchantmentHelper.get(itemStack)));
    }

    public void updateItemStack(ItemStack itemStack) {
        itemStack.setSubNbt(ENCHANTMENT_SLOTS_KEY, toNbt());
        putChosenEnchantments(itemStack);
    }

    @Override
    public Iterator<EnchantmentSlot> iterator() {
        return slots.values().iterator();
    }

    public Stream<EnchantmentSlot> stream() {
        return slots.values().stream();
    }

    public int merge(Map<Enchantment, Integer> enchantmentMap) {
        int cost = 0;
        for (var slot : this) {
            if (slot.getChosen().isEmpty()) {
                continue;
            }
            var chosen = slot.getChosen().get();
            var enchantment = chosen.getEnchantment();
            if (!enchantmentMap.containsKey(enchantment)) {
                continue;
            }
            if (chosen.getLevel() >= chosen.getEnchantment().getMaxLevel()) {
                continue;
            }
            var otherLvl = enchantmentMap.get(enchantment);
            if (otherLvl < chosen.getLevel()) {
                continue;
            }
            var upgrade = chosen.getLevel() == otherLvl ? 1 : otherLvl - chosen.getLevel();
            cost += MCDEnchantments.getConfig().getEnchantCost(chosen.getEnchantmentId(), upgrade);
            slot.setLevel(chosen.getLevel() + upgrade);
        }
        return cost;
    }

    public int merge(ItemStack itemStack) {
        return merge(EnchantmentHelper.get(itemStack));
    }
}
