package net.backupcup.mcde.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.backupcup.mcde.MCDEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

public class EnchantmentSlots implements Iterable<EnchantmentSlot> {
    public static final String ENCHANTMENT_SLOTS_KEY = "MCDEnchantments";
    private Map<SlotPosition, EnchantmentSlot> slots;
    private Set<Identifier> gilding;
    private int nextRerollCost;
    private int nextRerollCostPowerful;

    public EnchantmentSlots(Map<SlotPosition, EnchantmentSlot> slots, Set<Identifier> gilding, int nextRerollCost, int nextRerollCostPowerful) {
        this.slots = slots;
        this.gilding = gilding;
        this.nextRerollCost = nextRerollCost;
        this.nextRerollCostPowerful = nextRerollCostPowerful;
    }

    public EnchantmentSlots(Map<SlotPosition, EnchantmentSlot> slots) {
        this(
            slots,
            new HashSet<>(),
            MCDEnchantments.getConfig().getRerollCostParameters().getNormal().getStartCost(),
            MCDEnchantments.getConfig().getRerollCostParameters().getPowerful().getStartCost()
        );
    }

    public boolean hasGilding() {
        return !gilding.isEmpty();
    }

    public boolean hasGilding(Identifier id) {
        return gilding.contains(id);
    }

    public boolean hasGilding(Enchantment id) {
        return gilding.contains(EnchantmentUtils.getEnchantmentId(id));
    }

    public Set<Identifier> getGilding() {
        return gilding;
    }

    public void addGilding(Identifier gilding) {
        this.gilding.add(gilding);
    }

    public void addAllGilding(Collection<Identifier> gilidngs) {
        this.gilding.addAll(gilidngs);
    }

    public void removeGilding(Identifier gilding) {
        this.gilding.remove(gilding);
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

    public static EnchantmentSlots EMPTY = new EnchantmentSlots(Map.of(), Set.of(), 0, 0);

    public static Builder builder() {
        return new Builder();
    }

    public Optional<EnchantmentSlot> getEnchantmentSlot(SlotPosition pos) {
        return Optional.ofNullable(slots.get(pos));
    }

    @Override
    public String toString() {
        return String.format("%s gilded with %s", slots.toString(), gilding.toString());
    }

    public NbtCompound toNbt() {
        NbtCompound root = new NbtCompound();
        NbtCompound slotsCompound = new NbtCompound();
        slots.entrySet().stream()
            .forEach(kvp -> slotsCompound.put(kvp.getKey().name(), kvp.getValue().toNbt()));
        root.put("Slots", slotsCompound);
        root.put("Gilding", gilding.stream()
            .collect(Collectors.mapping(id -> NbtString.of(id.toString()),
                    Collectors.toCollection(() -> new NbtList()))));
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
        for (var id : gilding) {
            enchantments.put(EnchantmentUtils.getEnchantment(id), 1);
        }
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
                nbt.getList("Gilding", NbtList.STRING_TYPE).stream()
                .collect(Collectors.mapping(e -> Identifier.tryParse(e.asString()),
                        Collectors.filtering(id -> enchantments.containsKey(EnchantmentUtils.getEnchantment(id)),
                            Collectors.toSet()))),
                nbt.getCompound("NextRerollCost").getInt("Normal"),
                nbt.getCompound("NextRerollCost").getInt("Powerful"));
    }

    public static Optional<EnchantmentSlots> fromItemStack(ItemStack itemStack) {
        var nbt = itemStack.getNbt();
        if (nbt == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(fromNbt(
            nbt.getCompound(ENCHANTMENT_SLOTS_KEY),
            EnchantmentHelper.fromNbt(nbt.getList(ItemStack.ENCHANTMENTS_KEY, NbtList.COMPOUND_TYPE))
        ));
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

    public EnchantmentSlots merge(EnchantmentSlots other) {
        var mergedSlotMap = new EnumMap<SlotPosition, EnchantmentSlot>(SlotPosition.class);
        mergedSlotMap.putAll(slots);
        for (var kvp : other.slots.entrySet()) {
            mergedSlotMap.putIfAbsent(kvp.getKey(), kvp.getValue());
        }
        Set<Identifier> newGilding = switch (MCDEnchantments.getConfig().getGildingMergeStrategy()) {
            case REMOVE -> new HashSet<>();
            case FIRST -> new HashSet<>(gilding);
            case SECOND -> new HashSet<>(other.gilding);
            case BOTH -> Stream.concat(gilding.stream(), other.gilding.stream()).collect(Collectors.toSet());
        };
        for (var kvp : mergedSlotMap.entrySet()) {
            if (kvp.getValue().getChosen().map(c -> newGilding.contains(c.getEnchantmentId())).orElse(false)) {
                kvp.getValue().clearChoice();
            }
        }
        return new EnchantmentSlots(
            mergedSlotMap,
            newGilding,
            (nextRerollCost + other.nextRerollCost) / 2,
            (nextRerollCostPowerful + other.nextRerollCostPowerful) / 2
        );
    }
}
