package net.backupcup.mcde.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import net.backupcup.mcde.MCDE;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.EnchantRandomlyLootFunction;
import net.minecraft.util.math.random.Random;

@Mixin(EnchantRandomlyLootFunction.class)
public abstract class EnchantRandomlyLootFunctionMixin {
    @Shadow private static ItemStack addEnchantmentToStack(ItemStack stack, Enchantment enchantment, Random random) {
        return null;
    }

    @Inject(method = "process", at = @At("HEAD"), cancellable = true)
    private void mcde$processBook(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        var list = MCDE.getConfig().getCustomTreasurePool();
        if (!stack.isOf(Items.BOOK) || list.isEmpty()) {
            return;
        }
        var random = context.getRandom();
        var enchantment = list.get(random.nextInt(list.size()));
        cir.setReturnValue(addEnchantmentToStack(stack, enchantment, random));
    }
}
