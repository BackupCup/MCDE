package net.backupcup.mcd_enchantments.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow private NbtCompound nbt;

    @Shadow public abstract boolean hasNbt();

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void appendGilded(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        if (!hasNbt()) {
            return;
        }
        var tooltip = cir.getReturnValue();
        if (nbt.contains("Gilding")) {
            var gilded = Identifier.tryParse(nbt.getString("Gilding"));
            var pos = context.isAdvanced() ? tooltip.size() - 2 : tooltip.size();
            tooltip.add(pos, Text.translatable("item.tooltip.gilded", Text.translatable(gilded.toTranslationKey("enchantment")))
                    .formatted(Formatting.GOLD));
        }
    }
}
