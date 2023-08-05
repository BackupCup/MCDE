package net.backupcup.mcde.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.screen.handler.RunicTableScreenHandler;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
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
        var input = screenGetSlot(0).getStack();
        var other = screenGetSlot(1).getStack();
        var slots = EnchantmentSlots.fromItemStack(input);
        if (slots == null) {
            return;
        }
        if (!input.isItemEqualIgnoreDamage(other) && !other.isOf(Items.ENCHANTED_BOOK)) {
            return;
        }
        levelCost.set(slots.merge(other));
        ItemStack result = ItemStack.EMPTY;
        if (levelCost.get() > 0) {
            result = input.copy();
            slots.removeGilding();
            slots.updateItemStack(result);
        }
        if (MCDEnchantments.getConfig().isEnchantingWithBooksAllowed() && other.isOf(Items.ENCHANTED_BOOK)) {
            var map = EnchantmentHelper.get(other);
            slots.stream().map(slot -> slot.getChosen()).forEach(o -> o.ifPresent(c -> map.remove(c.getEnchantment())));
            var iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                if (!entry.getKey().isAcceptableItem(input)) {
                    iter.remove();
                }
            }
            if (!map.isEmpty()) {
                if (result.isEmpty()) {
                    result = input.copy();
                }
                levelCost.set(map.entrySet().stream()
                        .mapToInt(kvp -> RunicTableScreenHandler.getEnchantCost(EnchantmentUtils.getEnchantmentId(kvp.getKey()), kvp.getValue()))
                        .sum() + levelCost.get());
                EnchantmentHelper.set(map, result);
            }
        }
        if (!newItemName.isBlank() && (levelCost.get() > 0 || other.isEmpty())) {
            if (result.isEmpty()) {
                result = input.copy();
            }
            levelCost.set(levelCost.get() + 1);
            result.setCustomName(Text.literal(newItemName));
        }
        screenGetSlot(2).setStack(result);
        ((AnvilScreenHandler)(Object)this).sendContentUpdates();
        ci.cancel();
    }
}
