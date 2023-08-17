package net.backupcup.mcde.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EnchantmentSlot {
    private SlotPosition slot;
    private Map<SlotPosition, Identifier> enchantments;
    private int level = 0;

    private Optional<SlotPosition> chosen = Optional.empty();

    public EnchantmentSlot(SlotPosition slot, Map<SlotPosition, Identifier> enchantments) {
        this.slot = slot;
        this.enchantments = enchantments;
    }

    public Optional<Chosen> getChosen() {
        return chosen.map(pos -> new Chosen(pos, enchantments.get(pos), level));
    }

    public boolean setChosen(SlotPosition pos, int level) {
        if (enchantments.containsKey(pos)) {
            this.chosen = Optional.of(pos);
            this.level = level;
            return true;
        }
        return false;
    }

    public void clearChoice() {
        chosen = Optional.empty();
        level = 0;
    }

    public SlotPosition getSlotPosition() {
        return slot;
    }

    public int ordinal() {
        return slot.ordinal();
    }

    public Optional<Identifier> getChoice(SlotPosition pos) {
        return Optional.ofNullable(enchantments.get(pos));
    }

    public List<Choice> choices() {
        return enchantments.entrySet().stream().map(kvp -> new Choice(kvp.getKey(), kvp.getValue())).toList();
    }

    public class Choice {
        private SlotPosition choicePos;
        private Identifier enchantment;

        public Choice(SlotPosition choicePos, Identifier enchantment) {
            this.choicePos = choicePos;
            this.enchantment = enchantment;
        }
        public EnchantmentSlot getEnchantmentSlot() {
            return EnchantmentSlot.this;
        }
        public SlotPosition getChoicePosition() {
            return choicePos;
        }
        public int ordinal() {
            return choicePos.ordinal();
        }
        public Identifier getEnchantmentId() {
            return enchantment;
        }
        public Enchantment getEnchantment() {
            return Registry.ENCHANTMENT.get(enchantment);
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

        private Chosen(SlotPosition slot, Identifier enchantment, int level) {
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
        return level >= Registry.ENCHANTMENT.get(enchantmentId).getMaxLevel();
    }

    public static EnchantmentSlot of(SlotPosition slot, Identifier first) {
        return new EnchantmentSlot(slot, Map.of(SlotPosition.FIRST, first));
    }

    public static EnchantmentSlot of(SlotPosition slot, Identifier first, Identifier second) {
        return new EnchantmentSlot(slot, Map.of(SlotPosition.FIRST, first, SlotPosition.SECOND, second));
    }

    public static EnchantmentSlot of(SlotPosition slot, Identifier first, Identifier second, Identifier third) {
        return new EnchantmentSlot(slot, Map.of(SlotPosition.FIRST, first, SlotPosition.SECOND, second, SlotPosition.THIRD, third));
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

    public static EnchantmentSlot fromNbt(NbtCompound nbt, SlotPosition slot) {
        var choices = nbt.getCompound("Choices");
        var newSlot = new EnchantmentSlot(slot, choices.getKeys().stream()
                .collect(Collectors.toMap(
                    key -> SlotPosition.valueOf(key),
                    key -> Identifier.tryParse(choices.getString(key))
                )));
        if (nbt.contains("Chosen")) {
            newSlot.setChosen(SlotPosition.valueOf(nbt.getString("Chosen")), nbt.getShort("Level"));
        }
        return newSlot;
    }

    public void changeEnchantment(SlotPosition pos, Identifier enchantment) {
        enchantments.put(pos, enchantment);
    }
}
