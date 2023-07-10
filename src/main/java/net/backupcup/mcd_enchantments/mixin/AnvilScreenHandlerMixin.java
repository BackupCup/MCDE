package net.backupcup.mcd_enchantments.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.Slot;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {
    public Slot screenGetSlot(int index) {
        return ((AnvilScreenHandler)(Object)this).getSlot(index);
    }

    @Inject(method = "canTakeOutput", at = @At("HEAD"), cancellable = true)
    protected void canTakeOutputProxy(PlayerEntity player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        MCDEnchantments.LOGGER.info("canTakeOutput called with slots {} and {}", screenGetSlot(0).getStack(), screenGetSlot(1).getStack());
        cir.setReturnValue(false);
    }
}
