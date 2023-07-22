package net.backupcup.mcd_enchantments.mixin;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.backupcup.mcd_enchantments.util.EnchantmentSlots;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
        if (MCDEnchantments.getConfig().isAnvilItemMixingAllowed()) {
            return;
        }
        ItemStack input1 = screenGetSlot(0).getStack();
        ItemStack input2 = screenGetSlot(1).getStack();
        if (input2.isEmpty()) {
            return;
        }
        if (EnchantmentSlots.fromItemStack(input1) != null || EnchantmentSlots.fromItemStack(input2) != null) {
            cir.setReturnValue(false);
        }
    }
}
