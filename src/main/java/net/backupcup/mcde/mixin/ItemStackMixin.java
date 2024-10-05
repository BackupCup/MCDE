package net.backupcup.mcde.mixin;
import java.util.Optional;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.backupcup.mcde.util.ConsumerWrapper;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.FunctionalWrapper;
import net.minecraft.component.ComponentHolder;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ComponentHolder {
    @WrapOperation(method = "appendTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/tooltip/TooltipAppender;appendTooltip(Lnet/minecraft/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/item/tooltip/TooltipType;)V"))
    private <T extends TooltipAppender> void mcde$stack$setSlots(
        TooltipAppender tooltipAppender,
        Item.TooltipContext context,
        Consumer<Text> textConsumer,
        TooltipType type,
        Operation<Void> original,
        ComponentType<T> componentType
    ) {
        if (componentType.equals(DataComponentTypes.ENCHANTMENTS)) {
            original.call(
                tooltipAppender,
                context,
                FunctionalWrapper.wrapConsumer(textConsumer, Optional.ofNullable(get(EnchantmentSlots.COMPONENT_TYPE))),
                type
            );
            return;
        }
        original.call(tooltipAppender, context, textConsumer, type);
    }

}
