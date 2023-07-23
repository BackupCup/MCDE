package net.backupcup.mcde.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.backupcup.mcde.MCDEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.village.TradeOffers;

@Mixin(TradeOffers.EnchantBookFactory.class)
public class EnchantBookFactoryMixin {
    @ModifyVariable(method = "create", at = @At("STORE"))
    private Enchantment changeTrade(Enchantment enchantment) {
        if (!MCDEnchantments.getConfig().areVillagersSellOnlyUnbreaking()) {
            return enchantment;
        }
        return Enchantments.UNBREAKING;
    }
}
