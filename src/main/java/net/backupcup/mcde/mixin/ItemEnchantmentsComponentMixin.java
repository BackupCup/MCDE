package net.backupcup.mcde.mixin;

import java.util.Optional;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.backupcup.mcde.util.FunctionalWrapper;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Mixin(ItemEnchantmentsComponent.class)
public abstract class ItemEnchantmentsComponentMixin implements TooltipAppender {
    @WrapOperation(
        method = "appendTooltip",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"
        )
    )
    private void mcde$formatMcdeManagedEnchantments(
        Consumer<Text> textConsumer,
        Object text,
        Operation<Void> original,
        @Local RegistryEntry<Enchantment> enchantment
    ) {
        Optional<EnchantmentSlots> slots;
        try {
            slots = FunctionalWrapper.<Optional<EnchantmentSlots>>getDataFromWrapper(textConsumer);
        } catch (ClassCastException e) {
            slots = Optional.empty();
        }
        if (slots.isEmpty()) {
            original.call(textConsumer, text);
            return;
        }
        MutableText mutableText = (MutableText)text;
        if (slots.get().stream().flatMap(slot -> slot.getChosen().stream()).anyMatch(c -> c.getEnchantment().equals(enchantment))) {
           original.call(textConsumer, mutableText.formatted(EnchantmentUtils.formatEnchantment(enchantment)));
        }
        else if (slots.get().hasGilding(enchantment)) {
            original.call(textConsumer, Text.translatable("item.tooltip.gilded", enchantment.value().description())
                .formatted(Formatting.GOLD));
        }
    }
}
