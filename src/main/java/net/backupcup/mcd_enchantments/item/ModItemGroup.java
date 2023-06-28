package net.backupcup.mcd_enchantments.item;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ModItemGroup {
    public static final ItemGroup MCDEncahntments = FabricItemGroupBuilder.build(
            new Identifier(MCDEnchantments.MOD_ID, "mcd_enchantments"), () -> new ItemStack(ModItems.TEST_RUNE));
}
