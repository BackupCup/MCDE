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
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.text.Text;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends AnvilScreenHandler {
    public AnvilScreenHandlerMixin(int syncId, PlayerInventory inventory) {
        super(syncId, inventory);
    }
    
    @Shadow private Property levelCost;
    @Shadow private String newItemName;

    public AnvilScreenHandlerMixin(int syncId, PlayerInventory inventory, ScreenHandlerContext context) {
        super(syncId, inventory, context);
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void mixSlots(CallbackInfo ci) {
        var input = getSlot(0).getStack();
        var other = getSlot(1).getStack();
        var slots = EnchantmentSlots.fromItemStack(input);
        ItemStack result = ItemStack.EMPTY;
        if (slots == null) {
            if (EnchantmentSlots.fromItemStack(other) != null) {
                getSlot(2).setStack(ItemStack.EMPTY);
                ci.cancel();
            }
            return;
        }
        if (MCDEnchantments.getConfig().isEnchantingWithBooksAllowed() && other.isOf(Items.ENCHANTED_BOOK)) {
            
            levelCost.set(slots.merge(other));
            var map = EnchantmentHelper.get(other);
            var present = EnchantmentHelper.get(input);
            slots.stream().forEach(slot -> slot.getChosen().ifPresent(c -> map.remove(c.getEnchantment())));
            if (MCDEnchantments.getConfig().isCompatibilityRequired() && !player.isCreative()) {
                map.entrySet().removeIf(kvp -> present.keySet().stream()
                        .anyMatch(e -> !kvp.getKey().canCombine(e)));
            }
            var iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                if (!entry.getKey().isAcceptableItem(input) || (present.containsKey(entry.getKey()) && present.get(entry.getKey()) > entry.getValue())) {
                    iter.remove();
                    continue;
                }
                if (present.containsKey(entry.getKey())) {
                    entry.setValue(entry.getValue() + 1);
                }
            }
            if (!map.isEmpty()) {
                result = input.copy();
                slots.updateItemStack(result);
                getSlot(2).setStack(result);

                levelCost.set(map.entrySet().stream()
                        .mapToInt(kvp -> RunicTableScreenHandler.getEnchantCost(
                            EnchantmentUtils.getEnchantmentId(kvp.getKey()),
                            present.containsKey(kvp.getKey()) ?
                            kvp.getValue() == present.get(kvp.getKey()) ?
                                1 : kvp.getValue() - present.get(kvp.getKey()) :
                            kvp.getValue()
                        ))
                        .sum() + levelCost.get());
                present.entrySet().stream()
                    .filter(kvp -> !map.containsKey(kvp.getKey())).forEach(kvp -> map.put(kvp.getKey(), kvp.getValue()));
                EnchantmentHelper.set(map, result);
                setCustomNameToResult();
            }
            ci.cancel();
            return;
        }
        if (!other.getItem().isEnchantable(other) && !other.isOf(Items.ENCHANTED_BOOK)) {
            return;
        }
        if (!MCDEnchantments.getConfig().isAnvilItemMixingAllowed()) {
            if (other.isEmpty()) {
                return;
            }
            if (EnchantmentSlots.fromItemStack(input) != null || EnchantmentSlots.fromItemStack(other) != null) {
                getSlot(2).setStack(ItemStack.EMPTY);
                ci.cancel();
                return;
            }
        }
        if (!input.isItemEqualIgnoreDamage(other) && !other.isOf(Items.ENCHANTED_BOOK)) {
            return;
        }
        levelCost.set(slots.merge(other));
        if (levelCost.get() > 0) {
            result = input.copy();
            slots.removeGilding();
            slots.updateItemStack(result);
        }
        getSlot(2).setStack(result);
        setCustomNameToResult();
        ((AnvilScreenHandler)(Object)this).sendContentUpdates();
        ci.cancel();
    }

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void adjustPrice(CallbackInfo ci) {
        var input = getSlot(0).getStack();
        var other = getSlot(1).getStack();
        var left = EnchantmentHelper.get(input);
        var right = EnchantmentHelper.get(other);
        if (right.isEmpty() || EnchantmentSlots.fromItemStack(input) != null) {
            return;
        }
        levelCost.set(left.entrySet().stream()
                .filter(kvp -> right.containsKey(kvp.getKey()) && right.get(kvp.getKey()) >= kvp.getValue())
                .mapToInt(kvp -> RunicTableScreenHandler.getEnchantCost(
                    EnchantmentHelper.getEnchantmentId(kvp.getKey()),
                    kvp.getValue() == right.get(kvp.getKey()) ?
                        1 : right.get(kvp.getKey()) - kvp.getValue()
                )).sum() +
            right.entrySet().stream()
                .filter(kvp -> !left.containsKey(kvp.getKey()))
                .mapToInt(kvp -> RunicTableScreenHandler.getEnchantCost(
                    EnchantmentHelper.getEnchantmentId(kvp.getKey()),
                    kvp.getValue()
                )).sum() +
            (newItemName.isBlank() ? 0 : 1));
    }

    private void setCustomNameToResult() {
        if (newItemName.isBlank()) {
            return;
        }
        var input = getSlot(0).getStack();
        var result = getSlot(2).getStack();
        if (result.isEmpty()) {
            result = input.copy();
        }
        levelCost.set(levelCost.get() + 1);
        result.setCustomName(Text.literal(newItemName));
    }
}
