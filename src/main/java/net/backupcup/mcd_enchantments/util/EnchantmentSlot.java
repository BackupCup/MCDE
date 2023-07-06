package net.backupcup.mcd_enchantments.util;

import java.util.Map;
import java.util.Optional;

import net.minecraft.util.Identifier;

public class EnchantmentSlot {
    private Slots slot;
    private Map<Slots, Identifier> enchantments;

    private Optional<Slots> chosen = Optional.empty();

    public EnchantmentSlot(Slots slot, Map<Slots, Identifier> enchantments) {
        this.slot = slot;
        this.enchantments = enchantments;
    }

    public Optional<Choice> getChosen() {
        return chosen.isPresent() ?
            Optional.of(new Choice(chosen.get(), enchantments.get(chosen.get()))) : Optional.empty();
    }

    public void setChosen(Slots chosen) {
        if (enchantments.containsKey(chosen)) {
            this.chosen = Optional.of(chosen);
        }
    }

    public Slots getSlot() {
        return slot;
    }

    public int ordinal() {
        return slot.ordinal();
    }

    public Optional<Identifier> getChoice(Slots slot) {
        return enchantments.containsKey(slot) ?
            Optional.of(enchantments.get(slot)) : Optional.empty();
    }

    public Iterable<Choice> choices() {
        return () -> enchantments.entrySet().stream().map(kvp -> new Choice(kvp.getKey(), kvp.getValue())).iterator();
    }

    public class Choice {
        private Slots slot;
        private Identifier enchantment;

        public Choice(Slots slot, Identifier enchantment) {
            this.slot = slot;
            this.enchantment = enchantment;
        }
        public Slots getSlot() {
            return slot;
        }
        public int ordinal() {
            return slot.ordinal();
        }
        public Identifier getEnchantment() {
            return enchantment;
        }
    }

    public static EnchantmentSlot of(Slots slot, Identifier first) {
        return new EnchantmentSlot(slot, Map.of(Slots.FIRST, first));
    }

    public static EnchantmentSlot of(Slots slot, Identifier first, Identifier second) {
        return new EnchantmentSlot(slot, Map.of(Slots.FIRST, first, Slots.SECOND, second));
    }

    public static EnchantmentSlot of(Slots slot, Identifier first, Identifier second, Identifier third) {
        return new EnchantmentSlot(slot, Map.of(Slots.FIRST, first, Slots.SECOND, second, Slots.THIRD, third));
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", enchantments, chosen);
    }
}
