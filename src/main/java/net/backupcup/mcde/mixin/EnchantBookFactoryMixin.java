package net.backupcup.mcde.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.backupcup.mcde.MCDEnchantments;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;

@Mixin(TradeOffers.EnchantBookFactory.class)
public class EnchantBookFactoryMixin {
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private void changeTrade(Entity entity, Random random, CallbackInfoReturnable<TradeOffer> cir) {
        if (!MCDEnchantments.getConfig().areVillagersSellOnlyUnbreaking()) {
            return;
        }
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        book.addEnchantment(Enchantments.UNBREAKING, random.nextBetween(1, 3));
        cir.setReturnValue(new TradeOffer(
                    new ItemStack(Items.EMERALD, random.nextBetween(9, 36)),
                    new ItemStack(Items.BOOK),
                    book,
                    7, 14, 20, 1.0f
                    ));
    }
}
