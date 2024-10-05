package net.backupcup.mcde.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
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

        @ModifyExpressionValue(method = "getExperience(Lnet/minecraft/item/ItemStack;)I", at = @At(target = "Lnet/minecraft/registry/entry/RegistryEntry;isIn(Lnet/minecraft/registry/tag/TagKey;)Z", value = "INVOKE"))
        private boolean mcde$isCursedOrGilding(boolean original, ItemStack itemStack, @Local(index = 6) RegistryEntry<Enchantment> enchantment) {
            return original || EnchantmentUtils.isGilding(enchantment, itemStack);
        }
    }

    @ModifyReturnValue(method = "grind", at = @At("RETURN"))
    private ItemStack mcde$removeChoiceOnGrind(ItemStack itemStack, ItemStack item) {
        return EnchantmentSlots.fromItemStack(item).map(slots -> {
            var builder = EnchantmentSlots.builder(slots);
            for (var slot : slots) {
                builder.withSlot(slot.withoutChoice());
            }
            itemStack.set(EnchantmentSlots.COMPONENT_TYPE, builder.build());
            return itemStack;
        }).orElse(itemStack);
    }

    @Inject(
        method = "grind",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/enchantment/EnchantmentHelper.apply(Lnet/minecraft/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/component/type/ItemEnchantmentsComponent;",
            shift = At.Shift.AFTER
        )
    )
    private void mcde$reAddGilding(ItemStack stack, CallbackInfoReturnable<ItemStack> ci) {
        EnchantmentHelper.apply(stack, builder -> {
            for (var gild : EnchantmentSlots.fromItemStack(stack).map(slots -> slots.getGilding()).orElse(Set.of())) {
                builder.set(gild, 1);
            }
        });
    }
}
