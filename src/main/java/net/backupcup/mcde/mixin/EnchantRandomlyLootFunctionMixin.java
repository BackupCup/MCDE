package net.backupcup.mcde.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.backupcup.mcde.MCDEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.EnchantRandomlyLootFunction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;

@Mixin(EnchantRandomlyLootFunction.class)
public abstract class EnchantRandomlyLootFunctionMixin {
    @Shadow private static ItemStack addEnchantmentToStack(ItemStack stack, Enchantment enchantment, Random random) {
        return null;
    }

    @Inject(method = "process", at = @At("HEAD"), cancellable = true)
    private void processBook(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        if (!stack.isOf(Items.BOOK) || !MCDEnchantments.getConfig().isTreasureCustom()) {
            return;
        }
        var random = context.getRandom();
        var list = Registry.ENCHANTMENT.stream()
            .filter(MCDEnchantments.getConfig()::isInCustomTreasurePool).toList();
        var enchantment = list.get(random.nextInt(list.size()));
        cir.setReturnValue(addEnchantmentToStack(stack, enchantment, random));
    }
}
