package net.backupcup.mcde.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {
    
    public AnvilScreenHandlerMixin(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory,
            ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    @Final @Shadow private Property levelCost;
    @Shadow private String newItemName;

    @ModifyVariable(
        method = "updateResult",
        at = @At(value = "STORE"),
        ordinal = 0,
        slice = @Slice(
            from = @At(value = "INVOKE", target = "net/minecraft/enchantment/Enchantment.getRarity()Lnet/minecraft/enchantment/Enchantment$Rarity;"),
            to = @At(value = "INVOKE", target = "net/minecraft/item/ItemStack.getCount()I", ordinal = 1)
        )
    )
    private int mcde$adjustPrice(int original, @Local(index = 13) Enchantment enchantment, @Local(index = 15) int level, @Local(index = 17) int rarity) {
        int cost = MCDEnchantments.getConfig().getEnchantCost(EnchantmentHelper.getEnchantmentId(enchantment), level);
        return original + cost - rarity * level;
    }

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void mcde$adjustResult(CallbackInfo ci) {
        var slotsOptional1 = EnchantmentSlots.fromItemStack(input.getStack(0));
        var slotsOptional2 = EnchantmentSlots.fromItemStack(input.getStack(1));
        var result = output.getStack(0);
        if (ItemStack.areItemsEqual(input.getStack(0), input.getStack(1)) && !MCDEnchantments.getConfig().isAnvilItemMixingAllowed()) {
            output.setStack(0, ItemStack.EMPTY);
            return;
        }
        if (input.getStack(1).isOf(Items.ENCHANTED_BOOK) && !MCDEnchantments.getConfig().isEnchantingWithBooksAllowed()) {
            output.setStack(0, ItemStack.EMPTY);
            return;
        }
        if (slotsOptional1.isPresent() ^ slotsOptional2.isPresent()) {
            EnchantmentSlots.fromItemStack(result).ifPresent(slots -> {
                slots.updateItemStack(result);
            });
            return;
        }
        var slots1 = slotsOptional1.get();
        var slots2 = slotsOptional2.get();
        var resultMap = EnchantmentHelper.get(result);
        resultMap.entrySet().removeIf(kvp -> 
            switch (MCDEnchantments.getConfig().getGildingMergeStrategy()) {
                case REMOVE -> slots1.hasGilding(kvp.getKey()) || slots2.hasGilding(kvp.getKey());
                case FIRST -> slots2.hasGilding(kvp.getKey());
                case SECOND -> slots1.hasGilding(kvp.getKey());
                case BOTH -> false;
            });
        EnchantmentHelper.set(resultMap, result);
        slots1.merge(slots2).updateItemStack(result);
        output.setStack(0, result);
    }
}
