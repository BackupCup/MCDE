package net.backupcup.mcde.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
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

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void mcde$mixSlots(CallbackInfo ci) {
        var input = getSlot(0).getStack();
        var other = getSlot(1).getStack();
        var slotsOptional = EnchantmentSlots.fromItemStack(input);
        ItemStack result = ItemStack.EMPTY;
        if (slotsOptional.isEmpty()) {
            if (EnchantmentSlots.fromItemStack(other).isPresent()) {
                getSlot(2).setStack(ItemStack.EMPTY);
                ci.cancel();
            }
            return;
        }
        var slots = slotsOptional.get();
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
            if (!map.isEmpty() || (levelCost.get() > 0)) {
                result = input.copy();
                slots.updateItemStack(result);
                getSlot(2).setStack(result);

                levelCost.set(map.entrySet().stream()
                        .mapToInt(kvp -> MCDEnchantments.getConfig().getEnchantCost(
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
            if (EnchantmentSlots.fromItemStack(input).isPresent() || EnchantmentSlots.fromItemStack(other).isPresent()) {
                getSlot(2).setStack(ItemStack.EMPTY);
                ci.cancel();
                return;
            }
        }

        if (!ItemStack.areItemsEqual(input, other) && !other.isOf(Items.ENCHANTED_BOOK)) {
            return;
        }

        result = input.copy();
        levelCost.set(slots.merge(other));
        slots.removeGilding();
        slots.updateItemStack(result);

        if (input.isDamageable() && other.isDamageable()) {
            int inputDamage = input.getMaxDamage() - input.getDamage();
            int otherDamage = other.getMaxDamage() - other.getDamage();
            int newDamage = input.getMaxDamage() - (inputDamage + otherDamage + input.getMaxDamage() * 12 / 100);
            if (newDamage < 0) {
                newDamage = 0;
            }

            if (newDamage < input.getDamage()) {
                result.setDamage(newDamage);
                levelCost.set(levelCost.get() + 2);
            }
        }

        if (levelCost.get() == 0) {
            result = ItemStack.EMPTY;
        }

        getSlot(2).setStack(result);
        mcde$setCustomNameToResult();
        sendContentUpdates();
        ci.cancel();
    }

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void mcde$adjustPrice(CallbackInfo ci) {
        var input = getSlot(0).getStack();
        var other = getSlot(1).getStack();
        var left = EnchantmentHelper.get(input);
        var right = EnchantmentHelper.get(other);
        if (right.isEmpty() || EnchantmentSlots.fromItemStack(input).isPresent()) {
            return;
        }
        levelCost.set(left.entrySet().stream()
                .filter(kvp -> right.containsKey(kvp.getKey()) && right.get(kvp.getKey()) >= kvp.getValue())
                .mapToInt(kvp -> MCDEnchantments.getConfig().getEnchantCost(
                    EnchantmentHelper.getEnchantmentId(kvp.getKey()),
                    kvp.getValue() == right.get(kvp.getKey()) ?
                        1 : right.get(kvp.getKey()) - kvp.getValue()
                )).sum() +
            right.entrySet().stream()
                .filter(kvp -> !left.containsKey(kvp.getKey()))
                .mapToInt(kvp -> MCDEnchantments.getConfig().getEnchantCost(
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
