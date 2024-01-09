package net.backupcup.mcde.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class EnchantmentSlot {
    private SlotPosition slot;
    private Map<SlotPosition, Identifier> enchantments;
    private int level = 0;

    private Optional<SlotPosition> chosen = Optional.empty();

    public EnchantmentSlot(SlotPosition slot, Map<SlotPosition, Identifier> enchantments) {
        this.slot = slot;
        this.enchantments = enchantments;
    }

    public Optional<SlotPosition> getChosenPosition() {
        return chosen;
    }

    public Optional<Choice> getChosen() {
        return chosen.map(pos -> new Choice(this, pos));
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
        return enchantments.keySet().stream().map(pos -> new Choice(this, pos)).toList();
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void upgrade() {
        if (!isMaxedOut()) {
            level++;
        }
    }

    public boolean isMaxedOut() {
        return chosen.map(pos -> level >= Registries.ENCHANTMENT.get(enchantments.get(pos)).getMaxLevel()).orElse(false);
    }

    public void removeChosenEnchantment(ItemStack itemStack) {
        getChosen().ifPresent(c -> {
            var enchantments = EnchantmentHelper.get(itemStack);
            enchantments.remove(c.getEnchantment());
            EnchantmentHelper.set(enchantments, itemStack);
        });
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
        }
        return root;
    }

    public static EnchantmentSlot fromNbt(NbtCompound nbt, SlotPosition slot, Map<Enchantment, Integer> enchantments) {
        var choices = nbt.getCompound("Choices");
        var choiceMap = choices.getKeys().stream()
                .collect(Collectors.toMap(
                    key -> SlotPosition.valueOf(key),
                    key -> Identifier.tryParse(choices.getString(key))
                ));
        choiceMap.entrySet().removeIf(entry -> EnchantmentUtils.getEnchantment(entry.getValue()) == null);
        var newSlot = new EnchantmentSlot(slot, choiceMap);
        if (nbt.contains("Chosen")) {
            var chosenSlot = SlotPosition.valueOf(nbt.getString("Chosen"));
            if (choiceMap.containsKey(chosenSlot)) {
                newSlot.setChosen(chosenSlot, enchantments.getOrDefault(EnchantmentUtils.getEnchantment(choiceMap.get(chosenSlot)), 0));
            }
        }
        return newSlot;
    }

    public void changeEnchantment(SlotPosition pos, Identifier enchantment) {
        enchantments.put(pos, enchantment);
    }
}
