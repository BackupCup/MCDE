package net.backupcup.mcd_enchantments.util;

import java.util.Map;
import java.util.Optional;

import net.minecraft.util.Identifier;

public class EnchantmentSlot {
    private Slot slot;
    private Map<Slot, Identifier> enchantments;

    private Optional<Slot> chosen = Optional.empty();

    public EnchantmentSlot(Slot slot, Map<Slot, Identifier> enchantments) {
        this.slot = slot;
        this.enchantments = enchantments;
    }

    public Optional<Choice> getChosen() {
        return chosen.isPresent() ?
            Optional.of(new Choice(chosen.get(), enchantments.get(chosen.get()))) : Optional.empty();
    }

    public void setChosen(Slot chosen) {
        if (enchantments.containsKey(chosen)) {
            this.chosen = Optional.of(chosen);
        }
    }

    public Slot getSlot() {
        return slot;
    }

    public int ordinal() {
        return slot.ordinal();
    }

    public Optional<Identifier> getChoice(Slot slot) {
        return enchantments.containsKey(slot) ?
            Optional.of(enchantments.get(slot)) : Optional.empty();
    }

    public Iterable<Choice> choices() {
        return () -> enchantments.entrySet().stream().map(kvp -> new Choice(kvp.getKey(), kvp.getValue())).iterator();
    }

    public class Choice {
        private Slot slot;
        private Identifier enchantment;

        public Choice(Slot slot, Identifier enchantment) {
            this.slot = slot;
            this.enchantment = enchantment;
        }
        public Slot getSlot() {
            return slot;
        }
        public Identifier getEnchantment() {
            return enchantment;
        }
    }

    public static EnchantmentSlot of(Slot slot, Identifier first) {
        return new EnchantmentSlot(slot, Map.of(Slot.FIRST, first));
    }

    public static EnchantmentSlot of(Slot slot, Identifier first, Identifier second) {
        return new EnchantmentSlot(slot, Map.of(Slot.FIRST, first, Slot.SECOND, second));
    }

    public static EnchantmentSlot of(Slot slot, Identifier first, Identifier second, Identifier third) {
        return new EnchantmentSlot(slot, Map.of(Slot.FIRST, first, Slot.SECOND, second, Slot.THIRD, third));
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", enchantments, chosen);
    }
}
