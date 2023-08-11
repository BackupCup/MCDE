package net.backupcup.mcde.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class EnchantmentSlot {
    private Slots slot;
    private Map<Slots, Identifier> enchantments;
    private int level = 0;

    private Optional<Slots> chosen = Optional.empty();

    public EnchantmentSlot(Slots slot, Map<Slots, Identifier> enchantments) {
        this.slot = slot;
        this.enchantments = enchantments;
    }

    public Optional<Chosen> getChosen() {
        return chosen.isPresent() ?
            Optional.of(new Chosen(chosen.get(), enchantments.get(chosen.get()), level)) : Optional.empty();
    }

    public boolean setChosen(Slots chosen, int level) {
        if (enchantments.containsKey(chosen)) {
            this.chosen = Optional.of(chosen);
            this.level = level;
            return true;
        }
        return false;
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
        private Slots choiceSlot;
        private Identifier enchantment;

        public Choice(Slots choiceSlot, Identifier enchantment) {
            this.choiceSlot = choiceSlot;
            this.enchantment = enchantment;
        }
        public EnchantmentSlot getSlot() {
            return EnchantmentSlot.this;
        }
        public Slots getChoiceSlot() {
            return choiceSlot;
        }
        public int ordinal() {
            return choiceSlot.ordinal();
        }
        public Identifier getEnchantmentId() {
            return enchantment;
        }
        public Enchantment getEnchantment() {
            return Registries.ENCHANTMENT.get(enchantment);
        }
        public int getLevel() {
            return 1;
        }
        public boolean isMaxedOut() {
            return false;
        }
        public boolean isChosen() {
            return false;
        }
    }

    public class Chosen extends Choice {
        private int level;

        private Chosen(Slots slot, Identifier enchantment, int level) {
            super(slot, enchantment);
            this.level = level;
        }

        @Override
        public int getLevel() {
            return level;
        }

        public void upgrade() {
            if (!isMaxedOut()) {
                level++;
                EnchantmentSlot.this.level++;
            }
        }

        @Override
        public boolean isMaxedOut() {
            return EnchantmentSlot.isMaxedOut(getEnchantmentId(), level);
        }

        @Override
        public boolean isChosen() {
            return true;
        }
    }

    public void setLevel(int level) {
        this.level = level;
    }

    private static boolean isMaxedOut(Identifier enchantmentId, int level) {
        return level >= Registries.ENCHANTMENT.get(enchantmentId).getMaxLevel();
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
            root.putInt("Level", level);
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

    public void changeEnchantment(Slots slot, Identifier enchantment) {
        enchantments.put(slot, enchantment);
    }
}
