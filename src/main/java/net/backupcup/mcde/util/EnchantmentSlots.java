package net.backupcup.mcde.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.backupcup.mcde.util.EnchantmentSlot.Choice;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class EnchantmentSlots implements Iterable<EnchantmentSlot> {
    private Map<Slots, EnchantmentSlot> slots;
    private Optional<Identifier> gilding = Optional.empty();

    public EnchantmentSlots(Map<Slots, EnchantmentSlot> slots, Optional<Identifier> gilding) {
        this.gilding = gilding;
        this.slots = slots;
    }

    public EnchantmentSlots(Map<Slots, EnchantmentSlot> slots) {
        this.slots = slots;
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

    public static class Builder {
        private Map<Slots, EnchantmentSlot> slots = new TreeMap<>();

        public Builder withSlot(Slots slot, Identifier first) {
            slots.put(slot, EnchantmentSlot.of(slot, first));
            return this;
        }

        public Builder withSlot(Slots slot, Identifier first, Identifier second) {
            slots.put(slot, EnchantmentSlot.of(slot, first, second));
            return this;
        }

        public Builder withSlot(Slots slot, Identifier first, Identifier second, Identifier third) {
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

        public EnchantmentSlots build() {
            return new EnchantmentSlots(Collections.unmodifiableMap(slots));
        }
    }

    public static EnchantmentSlots EMPTY = new EnchantmentSlots(Map.of());

    public static Builder builder() {
        return new Builder();
    }

    public Optional<EnchantmentSlot> getSlot(Slots slot) {
        return slots.size() > slot.ordinal() ?
            Optional.of(slots.get(slot)) : Optional.empty();
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
        return root;
    }

    public static EnchantmentSlots fromNbt(NbtCompound nbt) {
        if (nbt == null) {
            return null;
        }
        var slots = nbt.getCompound("Slots");
        return new EnchantmentSlots(slots.getKeys().stream()
                .collect(Collectors.toMap(
                    key -> Slots.valueOf(key),
                    key -> EnchantmentSlot.fromNbt(slots.getCompound(key), Slots.valueOf(key))
                )), Optional.ofNullable(nbt.getString("Gilding")).filter(id -> !id.isEmpty()).map(Identifier::tryParse));
    }

    public static EnchantmentSlots fromItemStack(ItemStack itemStack) {
        return EnchantmentSlots.fromNbt(itemStack.getSubNbt("MCDEnchantments"));
    }

    public void updateItemStack(ItemStack itemStack) {
        itemStack.setSubNbt("MCDEnchantments", toNbt());
    }

    @Override
    public Iterator<EnchantmentSlot> iterator() {
        return slots.values().iterator();
    }

    public Stream<EnchantmentSlot> stream() {
        return slots.values().stream();
    }
}
