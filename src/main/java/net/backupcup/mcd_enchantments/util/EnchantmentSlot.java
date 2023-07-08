package net.backupcup.mcd_enchantments.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class EnchantmentSlot {
    private Slots slot;
    private Map<Slots, Identifier> enchantments;
    private short level = 0;

    private Optional<Slots> chosen = Optional.empty();

    public EnchantmentSlot(Slots slot, Map<Slots, Identifier> enchantments) {
        this.slot = slot;
        this.enchantments = enchantments;
    }

    public Optional<ChoiceWithLevel> getChosen() {
        return chosen.isPresent() ?
            Optional.of(new ChoiceWithLevel(chosen.get(), enchantments.get(chosen.get()), level)) : Optional.empty();
    }

    public boolean setChosen(Slots chosen, short level) {
        if (enchantments.containsKey(chosen)) {
            this.chosen = Optional.of(chosen);
            this.level = level;
            return true;
        }
        return false;
    }

    public Optional<ChoiceWithLevel> tryUpgrade() {
        if (chosen.isPresent()) {
            if (!isMaxedOut())
                level++;
            return Optional.of(new ChoiceWithLevel(chosen.get(), enchantments.get(chosen.get()), level));
        }
        return Optional.empty();
    }

    public void clearChoice() {
        chosen = Optional.empty();
        level = 0;
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

    public List<Choice> choices() {
        return enchantments.entrySet().stream().map(kvp -> new Choice(kvp.getKey(), kvp.getValue())).toList();
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

    public class ChoiceWithLevel extends Choice {
        private short level;

        private ChoiceWithLevel(Slots slot, Identifier enchantment, short level) {
            super(slot, enchantment);
            this.level = level;
        }

        public short getLevel() {
            return level;
        }

        public boolean isMaxedOut() {
            return EnchantmentSlot.isMaxedOut(getEnchantment(), level);
        }
    }

    private static boolean isMaxedOut(Identifier enchantmentId, short level) {
        return level >= Registry.ENCHANTMENT.get(enchantmentId).getMaxLevel();
    }
    private boolean isMaxedOut() {
        return chosen.isPresent() && isMaxedOut(enchantments.get(chosen.get()), level);
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

    public NbtCompound toNbt() {
        NbtCompound root = new NbtCompound();
        NbtCompound choices = new NbtCompound();
        enchantments.entrySet().stream()
            .forEach(kvp -> choices.putString(kvp.getKey().name(), kvp.getValue().toString()));
        root.put("Choices", choices);
        if (chosen.isPresent()) {
            root.putString("Chosen", chosen.get().name());
            root.putShort("Level", level);
        }
        return root;
    }

    public static EnchantmentSlot fromNbt(NbtCompound nbt, Slots slot) {
        var choices = nbt.getCompound("Choices");
        var newSlot = new EnchantmentSlot(slot, choices.getKeys().stream()
                .collect(Collectors.toMap(
                    key -> Slots.valueOf(key),
                    key -> Identifier.tryParse(choices.getString(key))
                )));
        if (nbt.contains("Chosen")) {
            newSlot.setChosen(Slots.valueOf(nbt.getString("Chosen")), nbt.getShort("Level"));
        }
        return newSlot;
    }
}
