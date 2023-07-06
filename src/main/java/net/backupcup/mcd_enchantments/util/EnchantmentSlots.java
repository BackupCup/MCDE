package net.backupcup.mcd_enchantments.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class EnchantmentSlots implements Iterable<EnchantmentSlot> {
    private Map<Slot, EnchantmentSlot> slots;

    public EnchantmentSlots(Map<Slot, EnchantmentSlot> slots) {
        this.slots = slots;
    }

    public static class Builder {
        private Map<Slot, EnchantmentSlot> slots = new TreeMap<>();

        public Builder withSlot(Slot slot, Identifier first) {
            slots.put(slot, EnchantmentSlot.of(slot, first));
            return this;
        }

        public Builder withSlot(Slot slot, Identifier first, Identifier second) {
            slots.put(slot, EnchantmentSlot.of(slot, first, second));
            return this;
        }

        public Builder withSlot(Slot slot, Identifier first, Identifier second, Identifier third) {
            slots.put(slot, EnchantmentSlot.of(slot, first, second, third));
            return this;
        }

        public EnchantmentSlots build() {
            return new EnchantmentSlots(Collections.unmodifiableMap(slots));
        }
    }

    public static EnchantmentSlots EMPTY = new EnchantmentSlots(Map.of());

    public static Builder builder() {
        return new Builder();
    }

    public Optional<EnchantmentSlot> getSlot(Slot slot) {
        return slots.size() > slot.ordinal() ?
            Optional.of(slots.get(slot)) : Optional.empty();
    }

    @Override
    public String toString() {
        return slots.toString();
    }

    public NbtCompound asNbt() {
        NbtCompound root = new NbtCompound();
        
        for (var kvp : slots.entrySet()) {
            EnchantmentSlot slot = kvp.getValue();
            NbtCompound slotNbt = new NbtCompound();
            for (var choice : slot.choices()) {
                slotNbt.putString(
                    String.format("Choice%s", choice.getSlot().ordinal()),
                    choice.getEnchantment().toString()
                );
            }
            if (slot.getChosen().isPresent()) {
                slotNbt.putInt("Chosen", slot.getChosen().get().getSlot().ordinal());
            }
            root.put(String.format("Slot%s", kvp.getKey().ordinal()), slotNbt);
        }
        return root;
    }

    public static EnchantmentSlots fromNbt(NbtCompound nbt) {
        Map<Slot, EnchantmentSlot> slots = new TreeMap<>();
        for (var slot : Slot.values()) {
            String key = String.format("Slot%d", slot.ordinal());
            if (!nbt.contains(key)) {
                continue;
            }
            NbtCompound slotNbt = nbt.getCompound(key);
            Map<Slot, Identifier> choice = new TreeMap<>();
            for (var choiceSlot : Slot.values()) {
                String choiceKey = String.format("Choice%d", choiceSlot.ordinal());
                if (!slotNbt.contains(choiceKey)) {
                    continue;
                }
                Identifier id = Identifier.tryParse(slotNbt.getString(choiceKey));
                choice.put(choiceSlot, id);
            }
            var newSlot = new EnchantmentSlot(slot, Collections.unmodifiableMap(choice));
            slots.put(slot, newSlot);
            if (slotNbt.contains("Chosen")) {
                newSlot.setChosen(Slot.values()[slotNbt.getInt("Chosen")]);
            }
        }
        return new EnchantmentSlots(Collections.unmodifiableMap(slots));
    }

    @Override
    public Iterator<EnchantmentSlot> iterator() {
        return slots.values().iterator();
    }
}
