package net.backupcup.mcde.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
import net.minecraft.text.Text;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ScreenHandler {
    
    @Final
    @Shadow private Property levelCost;
    @Shadow private String newItemName;

    @Unique private PlayerEntity playerEntity;

    protected AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/screen/ScreenHandlerContext;)V", at = @At("TAIL"))
    private void mcde$getPlayer(int syncId, PlayerInventory inventory, ScreenHandlerContext context, CallbackInfo ci){
        this.playerEntity = inventory.player;
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void mcde$mixSlots(CallbackInfo ci) {
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
            if (MCDEnchantments.getConfig().isCompatibilityRequired() && !playerEntity.isCreative()) {
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
                mcde$setCustomNameToResult();
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
        mcde$setCustomNameToResult();
        ((AnvilScreenHandler)(Object)this).sendContentUpdates();
        ci.cancel();
    }

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void mcde$adjustPrice(CallbackInfo ci) {
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

    @Unique
    private void mcde$setCustomNameToResult() {
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
