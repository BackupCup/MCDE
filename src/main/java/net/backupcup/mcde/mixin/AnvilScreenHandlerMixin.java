package net.backupcup.mcde.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {
    @Shadow private Property levelCost;
    @Shadow private String newItemName;

    public Slot screenGetSlot(int index) {
        return ((AnvilScreenHandler)(Object)this).getSlot(index);
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void mixSlots(CallbackInfo ci) {
        if (!MCDEnchantments.getConfig().isAnvilItemMixingAllowed()) {
            ItemStack input1 = screenGetSlot(0).getStack();
            ItemStack input2 = screenGetSlot(1).getStack();
            if (input2.isEmpty()) {
                return;
            }
            if (EnchantmentSlots.fromItemStack(input1) != null || EnchantmentSlots.fromItemStack(input2) != null) {
                screenGetSlot(2).setStack(ItemStack.EMPTY);
                ci.cancel();
            }
            return;
        }
        var input1 = screenGetSlot(0).getStack();
        var input2 = screenGetSlot(1).getStack();
        var slots = EnchantmentSlots.fromItemStack(input1);
        var slots2 = EnchantmentSlots.fromItemStack(input2);
        ItemStack other = input2;
        if (slots == null && slots2 == null) {
            return;
        }
        if (!input1.isItemEqualIgnoreDamage(input2) && !input2.isOf(Items.ENCHANTED_BOOK)) {
            return;
        }
        if (slots == null) {
            slots = slots2;
            other = input1;
        }
        levelCost.set(slots.merge(other));
        ItemStack result = ItemStack.EMPTY;
        if (levelCost.get() > 0) {
            result = input1.copy();
            slots.removeGilding();
            slots.updateItemStack(result);
        }
        if (!newItemName.isBlank() && (levelCost.get() > 0 || input2.isEmpty())) {
            if (result.isEmpty()) {
                result = input1.copy();
            }
            levelCost.set(levelCost.get() + 1);
            result.setCustomName(Text.literal(newItemName));
        }
        screenGetSlot(2).setStack(result);
        ((AnvilScreenHandler)(Object)this).sendContentUpdates();
        ci.cancel();
    }
}
