package net.backupcup.mcd_enchantments.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.minecraft.util.Identifier;

public class EnchantmentSlots {
    private List<EnchantmentSlot> slots;

    private EnchantmentSlots(List<EnchantmentSlot> slots) {
        this.slots = slots;
    }

    public static class EnchantmentSlot {
        private List<Identifier> enchantments;

        private Optional<Slot> chosen;

        private EnchantmentSlot(List<Identifier> enchantments) {
            this.enchantments = enchantments;
        }

        public Optional<Identifier> getChosen() {
            return chosen.isPresent() ?
                Optional.of(enchantments.get(chosen.get().ordinal())) : Optional.empty();
        }

        public Optional<Identifier> getInnerSlot(Slot slot) {
            return enchantments.size() < slot.ordinal() ?
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
            return "EnchantmentSlot [enchantments=" + enchantments + ", chosen=" + chosen + "]";
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
        return slots.size() < slot.ordinal() ?
            Optional.of(slots.get(slot.ordinal())) : Optional.empty();
    }

    @Override
    public String toString() {
        return slots.toString();
    }
}
