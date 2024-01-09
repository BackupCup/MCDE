package net.backupcup.mcde.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneScreenHandlerMixin extends ScreenHandler {
    protected GrindstoneScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Final @Shadow private Inventory input;
    @Final @Shadow private Inventory result;

    @Mixin(targets = "net.minecraft.screen.GrindstoneScreenHandler$4")
    public static abstract class Slot3 extends Slot {

        public Slot3(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Inject(method = "onTakeItem", at = @At("HEAD"))
        private void mcde$onTake(PlayerEntity playerEntity, ItemStack itemStack, CallbackInfo ci) {
            MCDEnchantments.LOGGER.info("On Take");
        }

        @ModifyExpressionValue(method = "getExperience(Lnet/minecraft/item/ItemStack;)I", at = @At(target = "Lnet/minecraft/enchantment/Enchantment;isCursed()Z", value = "INVOKE"))
        private boolean mcde$isCursedOrGilding(boolean original, ItemStack itemStack, @Local Enchantment enchantment) {
            MCDEnchantments.LOGGER.info("{}'s slots: {}", itemStack.getName().getString(), EnchantmentSlots.fromItemStack(itemStack));
            return original || EnchantmentUtils.isGilding(enchantment, itemStack);
        }
    }

    @ModifyReturnValue(method = "grind", at = @At("RETURN"))
    private ItemStack mcde$removeChoiceOnGrind(ItemStack itemStack, ItemStack item) {
        return EnchantmentSlots.fromItemStack(item).map(slots -> {
            for (var slot : slots) {
                slot.clearChoice();
            }
            MCDEnchantments.LOGGER.info("{}'s NBT before update: {}", itemStack.getName().getString(), itemStack.getNbt());
            slots.updateItemStack(itemStack);
            MCDEnchantments.LOGGER.info("{}'s NBT after update: {}", itemStack.getName().getString(), itemStack.getNbt());
            return itemStack;
        }).orElse(itemStack);
    }

    @ModifyVariable(method = "grind", at = @At("STORE"))
    private Map<Enchantment, Integer> mcde$isCursedOrGilding(Map<Enchantment, Integer> map, ItemStack item) {
        map = EnchantmentHelper.get(item);
        map.entrySet().removeIf(kvp -> !kvp.getKey().isCursed() && !EnchantmentUtils.isGilding(kvp.getKey(), item));
        return map;
    }
}
