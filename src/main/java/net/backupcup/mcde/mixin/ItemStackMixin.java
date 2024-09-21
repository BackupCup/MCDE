package net.backupcup.mcde.mixin;

import java.util.List;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.backupcup.mcde.util.Choice;
import net.backupcup.mcde.util.EnchantmentSlot;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

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
    private NbtList mcde$removeMcdeManagedEnchantments(NbtList original) {
        var itemStack = (ItemStack)(Object)this;
        var slotsOptional = EnchantmentSlots.fromItemStack(itemStack);
        if (slotsOptional.isEmpty()) {
            return original;
        }
        var slots = slotsOptional.get();
        var list = original.copy();
        list.removeIf(e -> slots.stream().flatMap(slot -> slot.getChosen().stream())
                .anyMatch(c -> c.getEnchantmentId().equals(EnchantmentHelper.getIdFromNbt((NbtCompound)e))));
        list.removeIf(e -> slots.getGildingIds().contains(EnchantmentHelper.getIdFromNbt((NbtCompound)e)));
        return list;
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
    private void mcde$appendMcdeEnchantmentLines(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir, List<Text> tooltip) {
        var slotsOptional = EnchantmentSlots.fromItemStack((ItemStack)(Object)this);
        if (slotsOptional.isEmpty()) {
            return;
        }
        var slots = slotsOptional.get();
        for (var slot : slots) {
            if (slot.getChosen().isPresent()) {
                var chosen =  slot.getChosen().get();
                var name = Text.translatable(chosen.getEnchantmentId().toTranslationKey("enchantment"));
                var enchantment = Registries.ENCHANTMENT.get(chosen.getEnchantmentId());
                if (enchantment.getMaxLevel() > 1) {
                    name.append(" ")
                        .append(Text.translatable("enchantment.level." + chosen.getLevel()));
                }
                tooltip.add(name.formatted(EnchantmentUtils.formatEnchantment(chosen.getEnchantmentId())));
            }
        }
        for (var gilded : slots.getGildingIds()) {
            tooltip.add(Text.translatable("item.tooltip.gilded", Text.translatable(gilded.toTranslationKey("enchantment")))
                    .formatted(Formatting.GOLD));
        }
    }

    @ModifyReturnValue(method = "getEnchantments", at = @At("RETURN"))
    private NbtList mcde$forceLevelOfGilding(NbtList list) {
        EnchantmentSlots.fromItemStack((ItemStack)(Object)this).ifPresent(slots -> {
            for (var e : list) {
                NbtCompound c = (NbtCompound)e;
                if (slots.getGildingIds().contains((EnchantmentHelper.getIdFromNbt(c)))) {
                    EnchantmentHelper.writeLevelToNbt(c, 1);
                }
            }
        });
        return list;
    }
}
