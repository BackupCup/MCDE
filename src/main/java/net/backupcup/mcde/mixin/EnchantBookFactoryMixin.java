package net.backupcup.mcde.mixin;


import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.backupcup.mcde.MCDEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.village.TradeOffers;

@Mixin(TradeOffers.EnchantBookFactory.class)
public class EnchantBookFactoryMixin {
    @ModifyVariable(method = "create", at = @At("STORE"))
    private List<Enchantment> changeTrade(List<Enchantment> list) {
        var config = MCDEnchantments.getConfig();
        if (config.areVillagersSellFromCustomPool()) {
            var pool = config.getVillagerBookPool();
            if (pool.size() == 0) {
                MCDEnchantments.LOGGER.warn("Custom pool size must be non-zero.");
                return list;
            }
            return pool;
        }
        return list;
    }
}
