package net.backupcup.mcde.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.backupcup.mcde.util.SlotsGenerator;
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
import net.minecraft.text.Text;

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
    private int mcde$adjustPrice(int original, @Local Enchantment enchantment, @Local(index = 15) int level, @Local(index = 17) int rarity) {
        int cost = MCDEnchantments.getConfig().getEnchantCost(EnchantmentHelper.getEnchantmentId(enchantment), level);
        MCDEnchantments.LOGGER.debug("Adding cost of {} ({}) instead of original {}", enchantment.getName(level).getString(), cost, rarity * level);
        return original + cost - rarity * level;
    }

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void mcde$adjustResult() {
        var slotsOptional1 = EnchantmentSlots.fromItemStack(input.getStack(0));
        var slotsOptional2 = EnchantmentSlots.fromItemStack(input.getStack(1));
        if (!slotsOptional1.isPresent() || !slotsOptional2.isPresent()) {
            return;
        }
        var slots1 = slotsOptional1.get();
        var slots2 = slotsOptional2.get();
        var result = output.getStack(0);
        slots1.merge(slots2).updateItemStack(result);
        // TODO:
        // 1. merge slots
        // 2. handle gilidng (in different ways, specified via config)
        output.setStack(0, result);
    }
}
