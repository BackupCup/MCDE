package net.backupcup.mcd_enchantments.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class EnchantmentSlots {
    private List<EnchantmentSlot> slots;

    private EnchantmentSlots(List<EnchantmentSlot> slots) {
        this.slots = slots;
    }

    public static class EnchantmentSlot {
        private List<Identifier> enchantments;

        private Optional<Slot> chosen = Optional.empty();

        private EnchantmentSlot(List<Identifier> enchantments) {
            this.enchantments = enchantments;
        }

        private EnchantmentSlot(List<Identifier> enchantments, Slot chosen) {
            this.enchantments = enchantments;
            this.chosen = Optional.of(chosen);
        }

        public Optional<Identifier> getChosen() {
            return chosen.isPresent() ?
                Optional.of(enchantments.get(chosen.get().ordinal())) : Optional.empty();
        }

        public Optional<Identifier> getInnerSlot(Slot slot) {
            return enchantments.size() > slot.ordinal() ?
                Optional.of(enchantments.get(slot.ordinal())) : Optional.empty();
        }

        public static EnchantmentSlot of(Identifier first) {
            return new EnchantmentSlot(List.of(first));
        }

        public static EnchantmentSlot of(Identifier first, Identifier second) {
            return new EnchantmentSlot(List.of(first, second));
        }

        public static EnchantmentSlot of(Identifier first, Identifier second, Identifier third) {
            return new EnchantmentSlot(List.of(first, second, third));
        }

        @Override
        public String toString() {
            return String.format("%s (%s)", enchantments, chosen);
        }
    }

    public static class Builder {
        private List<EnchantmentSlot> slots = new ArrayList<>();

        public Builder withSlot(Identifier first) {
            checkSize();
            slots.add(EnchantmentSlot.of(first));
            return this;
        }

        public Builder withSlot(Identifier first, Identifier second) {
            checkSize();
            slots.add(EnchantmentSlot.of(first, second));
            return this;
        }

        public Builder withSlot(Identifier first, Identifier second, Identifier third) {
            checkSize();
            slots.add(EnchantmentSlot.of(first, second, third));
            return this;
        }

        public EnchantmentSlots build() {
            return new EnchantmentSlots(Collections.unmodifiableList(slots));
        }

        private void checkSize() {
            if (slots.size() > 3) {
                throw new RuntimeException("There is only 3 slots");
            }
        }
    }

    public static EnchantmentSlots EMPTY = new EnchantmentSlots(List.of());

    public static Builder builder() {
        return new Builder();
    }

    public Optional<EnchantmentSlot> getSlot(Slot slot) {
        return slots.size() > slot.ordinal() ?
            Optional.of(slots.get(slot.ordinal())) : Optional.empty();
    }

    @Override
    public String toString() {
        return slots.toString();
    }

    public NbtCompound asNbt() {
        NbtCompound root = new NbtCompound();
        
        for (int i = 0; i < slots.size(); i++) {
            EnchantmentSlot slot = slots.get(i);
            NbtCompound slotNbt = new NbtCompound();
            for (int j = 0; j < slot.enchantments.size(); j++) {
                Identifier id = slot.enchantments.get(j);
                slotNbt.putString(String.format("Choice%s", j), id.toString());
            }
            if (slot.chosen.isPresent()) {
                slotNbt.putInt("Chosen", slot.chosen.get().ordinal());
            }
            root.put(String.format("Slot%s", i), slotNbt);
        }
        return root;
    }

    public static EnchantmentSlots fromNbt(NbtCompound nbt) {
        List<EnchantmentSlot> slots = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String key = String.format("Slot%d", i);
            if (!nbt.contains(key)) {
                continue;
            }
            NbtCompound slotNbt = nbt.getCompound(key);
            List<Identifier> choice = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                String innerKey = String.format("Choice%d", j);
                if (!slotNbt.contains(innerKey)) {
                    continue;
                }
                Identifier id = Identifier.tryParse(slotNbt.getString(String.format("Choice%d", j)));
                choice.add(id);
            }
            if (slotNbt.contains("Chosen")) {
                slots.add(new EnchantmentSlot(Collections.unmodifiableList(choice), Slot.values()[slotNbt.getInt("Chosen")]));
            }
            else {
                slots.add(new EnchantmentSlot(Collections.unmodifiableList(choice)));
            }
        }
        return new EnchantmentSlots(Collections.unmodifiableList(slots));
    }
}
