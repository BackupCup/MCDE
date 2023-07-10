package net.backupcup.mcd_enchantments.mixin;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {
    public Slot screenGetSlot(int index) {
        return ((AnvilScreenHandler)(Object)this).getSlot(index);
    }

    @Inject(method = "canTakeOutput", at = @At("HEAD"), cancellable = true)
    protected void canTakeOutputProxy(PlayerEntity player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        ItemStack itemSlot1 = screenGetSlot(0).getStack();
        ItemStack itemSlot2 = screenGetSlot(1).getStack();

        MCDEnchantments.LOGGER.info("canTakeOutput called with slots {} and {}", itemSlot1, itemSlot2);
        if ((itemSlot2.getItem() != Items.ENCHANTED_BOOK)) cir.setReturnValue(false);
    }
}
