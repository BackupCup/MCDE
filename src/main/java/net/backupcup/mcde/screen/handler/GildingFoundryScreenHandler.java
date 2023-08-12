package net.backupcup.mcde.screen.handler;

import net.backupcup.mcde.block.ModBlocks;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;

public class GildingFoundryScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final ScreenHandlerContext context;
    public Inventory getInventory() {
        return inventory;
    }

    private final PropertyDelegate propertyDelegate;

    public GildingFoundryScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, new SimpleInventory(2), new ArrayPropertyDelegate(1), ScreenHandlerContext.EMPTY);
    }

    public GildingFoundryScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate delegate, ScreenHandlerContext context) {
        super(ModScreenHandlers.GILDING_FOUNDRY_SCREEN_HANDLER, syncId);
        this.context = context;

        checkSize(inventory, 1);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);
        this.propertyDelegate = delegate;

        this.addSlot(new Slot(inventory, 0, 82, 17) {

            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isDamageable();
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        this.addSlot(new Slot(inventory, 1, 82, 53) {

            @Override
            public boolean canInsert(ItemStack stack) {
                return (stack.getItem() == Items.GOLD_INGOT);
            }

            @Override
            public int getMaxItemCount() {
                return 64;
            }
        });

        addListener(EnchantmentUtils.generatorListener(context, playerInventory.player));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        addProperties(delegate);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!player.isCreative()) {
            startProgress();
            return false;
        }
        var weaponStack = inventory.getStack(0);
        var gildedEnchantment = EnchantmentUtils.generateEnchantment(weaponStack);
        if (gildedEnchantment.isEmpty()) {
            return false;
        }
        var enchantmentId = gildedEnchantment.get();
        var slots = EnchantmentSlots.fromItemStack(weaponStack);
        slots.setGilding(enchantmentId);
        slots.updateItemStack(weaponStack);
        return false;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    public int getProgress() {
        return propertyDelegate.get(0);
    }

    public void startProgress() {
        propertyDelegate.set(0, 1);
    }

    public boolean hasProgress() {
        return getProgress() != 0;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(context, player, ModBlocks.GILDING_FOUNDRY);
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 10 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 10 + i * 18, 142));
        }
    }
}
