package net.backupcup.mcde.mixin;

import java.util.List;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.Registry;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow private NbtCompound nbt;

    @Shadow public abstract boolean hasNbt();

    @Shadow public static void appendEnchantments(List<Text> tooltip, NbtList list) { }

    @ModifyArg(
        method = "getTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;appendEnchantments(Ljava/util/List;Lnet/minecraft/nbt/NbtList;)V",
            ordinal = 0
        ),
        index = 1
    )
    private NbtList removeMcdeManagedEnchantments(NbtList list) {
        var itemStack = (ItemStack)(Object)this;
        var slots = EnchantmentSlots.fromItemStack(itemStack);

        if (slots == null) {
            return list;
        }
        var map = EnchantmentHelper.get(itemStack);
        for (var slot : slots) {
            slot.getChosen().ifPresent(chosen -> map.remove(chosen.getEnchantment()));
        }
        if (slots.hasGilding()) {
            map.remove(Registry.ENCHANTMENT.get(slots.getGilding().get()));
        }
        return map.entrySet().stream()
            .map(kvp -> EnchantmentHelper.createNbt(EnchantmentHelper.getEnchantmentId(kvp.getKey()), kvp.getValue()))
            .collect(Collectors.toCollection(() -> new NbtList()));
    }

    @Inject(
        method = "getTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;appendEnchantments(Ljava/util/List;Lnet/minecraft/nbt/NbtList;)V",
            ordinal = 0,
            shift = Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void appendEnchantmentLines(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir, List<Text> tooltip) {
        var slots = EnchantmentSlots.fromItemStack((ItemStack)(Object)this);
        if (slots == null) {
            return;
        }

        for (var slot : slots) {
            if (slot.getChosen().isPresent()) {
                var chosen =  slot.getChosen().get();
                var name = Text.translatable(chosen.getEnchantmentId().toTranslationKey("enchantment"));
                if (Registry.ENCHANTMENT.get(chosen.getEnchantmentId()).getMaxLevel() > 1) {
                    name.append(" ")
                        .append(Text.translatable("enchantment.level." + chosen.getLevel()));
                }
                tooltip.add(name.formatted(EnchantmentUtils.formatEnchantment(chosen.getEnchantmentId())));
            }
        }
        if (slots.hasGilding()) {
            var gilded = slots.getGilding().get();
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
                newList.add(EnchantmentHelper.createNbt(chosen.getEnchantmentId(), chosen.getLevel()));
            });
        }

        if (!slots.hasGilding()) {
            return newList;
        }
        var gilded = slots.getGilding().get();
        newList.add(EnchantmentHelper.createNbt(gilded, 1));
        return newList;
    }

    @Inject(method = "hasGlint", at = @At("HEAD"), cancellable = true)
    private void hasEnchantments(CallbackInfoReturnable<Boolean> cir) {
        var slots = EnchantmentSlots.fromItemStack((ItemStack)(Object)this);
        if (slots == null) {
            return;
        }
        cir.setReturnValue(slots.stream()
                .anyMatch(s -> s.getChosen().isPresent()));
    }
}
