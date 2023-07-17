package net.backupcup.mcd_enchantments.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.backupcup.mcd_enchantments.util.EnchantmentSlots;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow private NbtCompound nbt;

    @Shadow public abstract boolean hasNbt();

    @Shadow public static void appendEnchantments(List<Text> tooltip, NbtList list) { }

    @Redirect(
        method = "getTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;appendEnchantments(Ljava/util/List;Lnet/minecraft/nbt/NbtList;)V",
            ordinal = 0
        )
    )
    private void nullifyAppendEnchantments(List<Text> tooltip, NbtList list) {
        return;
    }

    @Inject(
        method = "getTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;appendEnchantments(Ljava/util/List;Lnet/minecraft/nbt/NbtList;)V",
            ordinal = 0
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void appendEnchantmentLines(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir, List<Text> tooltip) {
        if (nbt.contains("Enchantments")) {
            appendEnchantments(tooltip, nbt.getList("Enchantments", NbtElement.COMPOUND_TYPE));
        }

        var slots = EnchantmentSlots.fromItemStack((ItemStack)(Object)this);
        if (slots == null) {
            return;
        }

        for (var slot : slots) {
            if (slot.getChosen().isPresent()) {
                var chosen =  slot.getChosen().get();
                var name = Text.translatable(chosen.getEnchantment().toTranslationKey("enchantment"));
                if (Registry.ENCHANTMENT.get(chosen.getEnchantment()).getMaxLevel() > 1) {
                    name.append(" ")
                        .append(Text.translatable("enchantment.level." + chosen.getLevel()));
                }
                tooltip.add(name.formatted(Formatting.LIGHT_PURPLE));
            }
        }
        if (nbt.contains("Gilding")) {
            var gilded = Identifier.tryParse(nbt.getString("Gilding"));
            tooltip.add(Text.translatable("item.tooltip.gilded", Text.translatable(gilded.toTranslationKey("enchantment")))
                    .formatted(Formatting.GOLD));
        }
    }

    @ModifyReturnValue(method = "getEnchantments", at = @At("RETURN"))
    private NbtList getEnchantments(NbtList list) {
        if (!hasNbt()) {
            return list;
        }
    
        var slots = EnchantmentSlots.fromItemStack((ItemStack)(Object)this);
        if (slots == null) {
            return list;
        }

        var newList = list.copy();

        for (var slot : slots) {
            slot.getChosen().ifPresent(chosen -> {
                newList.add(EnchantmentHelper.createNbt(chosen.getEnchantment(), chosen.getLevel()));
            });
        }

        if (!nbt.contains("Gilding")) {
            return newList;
        }
        var gilded = Identifier.tryParse(nbt.getString("Gilding"));
        newList.add(EnchantmentHelper.createNbt(gilded, 1));
        return newList;
    }

    @Inject(method = "hasEnchantments", at = @At("HEAD"), cancellable = true)
    private void hasEnchantments(CallbackInfoReturnable<Boolean> cir) {
        if (!hasNbt() || !nbt.contains("Slots")) {
            return;
        }
        cir.setReturnValue(EnchantmentSlots.fromItemStack((ItemStack)(Object)this).stream()
                .anyMatch(s -> s.getChosen().isPresent()));
    }
}
