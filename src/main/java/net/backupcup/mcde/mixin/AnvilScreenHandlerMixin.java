package net.backupcup.mcde.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import net.backupcup.mcde.MCDE;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {
    
    public AnvilScreenHandlerMixin(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory,
            ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    @Final @Shadow private Property levelCost;
    @Shadow private String newItemName;

    @ModifyVariable(
        method = "updateResult",
        at = @At(value = "STORE"),
        ordinal = 0,
        slice = @Slice(
            from = @At(value = "INVOKE", target = "net/minecraft/enchantment/Enchantment.getAnvilCost()I"),
            to = @At(value = "INVOKE", target = "net/minecraft/item/ItemStack.getCount()I", ordinal = 1)
        )
    )
    private int mcde$adjustPrice(int original, @Local(index = 15) RegistryEntry<Enchantment> enchantment, @Local(index = 17) int level) {
        int cost = MCDE.getConfig().getEnchantCost(enchantment, level);
        return original + cost;
    }

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void mcde$adjustResult(CallbackInfo ci) {
        var slotsOptional1 = EnchantmentSlots.fromItemStack(input.getStack(0));
        var slotsOptional2 = EnchantmentSlots.fromItemStack(input.getStack(1));
        var result = output.getStack(0);
        if (ItemStack.areItemsEqual(input.getStack(0), input.getStack(1)) && !MCDE.getConfig().isAnvilItemMixingAllowed()) {
            output.setStack(0, ItemStack.EMPTY);
            return;
        }
        if (input.getStack(1).isOf(Items.ENCHANTED_BOOK) && !MCDE.getConfig().isEnchantingWithBooksAllowed()) {
            output.setStack(0, ItemStack.EMPTY);
            return;
        }
        if (slotsOptional1.isPresent() ^ slotsOptional2.isPresent()) {
            var slots = slotsOptional1.orElse(slotsOptional2.get());
            EnchantmentHelper.apply(result, builder -> {
                for (var gild : slots.getGilding()) {
                    builder.set(gild, 1);
                }
            });
        } 
        if (slotsOptional1.isEmpty() || slotsOptional2.isEmpty()) {
            return;
        }
        var slots1 = slotsOptional1.get();
        var slots2 = slotsOptional2.get();
        var resultComponentBuilder = new ItemEnchantmentsComponent.Builder(EnchantmentHelper.getEnchantments(result));
        resultComponentBuilder.getEnchantments().removeIf(enchantment -> 
            switch (MCDE.getConfig().getGildingMergeStrategy()) {
                case REMOVE -> slots1.hasGilding(enchantment) || slots2.hasGilding(enchantment);
                case FIRST -> slots2.hasGilding(enchantment);
                case SECOND -> slots1.hasGilding(enchantment);
                case BOTH -> false;
            });
        var merged = EnchantmentSlots.merge(slots1, slots2);
        for (var gild : merged.getGilding()) {
            resultComponentBuilder.set(gild, 1);
        }
        result.applyChanges(ComponentChanges.builder()
                .add(DataComponentTypes.ENCHANTMENTS, resultComponentBuilder.build())
                .add(EnchantmentSlots.COMPONENT_TYPE, merged)
                .build());
        output.setStack(0, result);
    }
}
