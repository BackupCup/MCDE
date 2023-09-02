package net.backupcup.mcde.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.backupcup.mcde.util.EnchantmentSlots;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneScreenHandlerMixin extends ScreenHandler {
    protected GrindstoneScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @ModifyReturnValue(method = "grind", at = @At("RETURN"))
    private ItemStack mcde$removeChoiceOnGrind(ItemStack itemStack) {
        EnchantmentSlots.fromItemStack(itemStack).ifPresent(slots -> {
            for (var slot : slots) {
                slot.clearChoice();
            }
            slots.updateItemStack(itemStack);
        });
        return itemStack;
    }
}
