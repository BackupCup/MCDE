package net.backupcup.mcde.mixin;


import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.backupcup.mcde.MCDE;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.village.TradeOffers;

@Mixin(TradeOffers.EnchantBookFactory.class)
public class EnchantBookFactoryMixin {
    @ModifyVariable(method = "create", at = @At("STORE"))
    private List<Enchantment> mcde$changeTrade(List<Enchantment> list) {
        var pool = MCDE.getConfig().getVillagerBookPool();
        if (pool.isEmpty()) {
            return list;
        }
        return pool;
    }
}
