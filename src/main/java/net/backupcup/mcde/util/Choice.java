package net.backupcup.mcde.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry.Reference;

public class Choice {
    private final SlotPosition choicePos;
    private final EnchantmentSlot enchantmentSlot;

    public Choice(EnchantmentSlot enchantmentSlot, SlotPosition choicePos) {
        this.choicePos = choicePos;
        this.enchantmentSlot = enchantmentSlot;
    }

    public int getLevel() {
        return enchantmentSlot.getLevel();
    }

    public boolean isMaxedOut() {
        return enchantmentSlot.isMaxedOut();
    }

    public boolean isChosen() {
        return enchantmentSlot.getChosenPosition().map(choicePos::equals).orElse(false) &&
            enchantmentSlot.getLevel() > 0;
    }

    public EnchantmentSlot getEnchantmentSlot() {
        return enchantmentSlot;
    }

    public SlotPosition getChoicePosition() {
        return choicePos;
    }

    public int ordinal() {
        return choicePos.ordinal();
    }

    public Identifier getEnchantmentId() {
        return enchantmentSlot.getChoice(choicePos).get();
    }

    public Reference<Enchantment> getEnchantment() {
        return Registries.ENCHANTMENT.get(enchantmentSlot.getChoice(choicePos).get());
    }
}
