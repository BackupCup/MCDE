package net.backupcup.mcd_enchantments.screen;

import net.backupcup.mcd_enchantments.util.EnchantmentSlots;
import net.backupcup.mcd_enchantments.util.Slots;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class RunicTableScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    public Inventory getInventory() {
        return inventory;
    }

    private final PropertyDelegate propertyDelegate;

    public RunicTableScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, new SimpleInventory(1), new ArrayPropertyDelegate(0));
    }

    public RunicTableScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate delegate) {
        super(ModScreenHandlers.RUNIC_TABLE_SCREEN_HANDLER, syncId);

        checkSize(inventory, 1);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);
        this.propertyDelegate = delegate;

        this.addSlot(new Slot(inventory, 0, 145, 46) {

            @Override
            public boolean canInsert(ItemStack stack) {
                return (EnchantmentTarget.TRIDENT.isAcceptableItem(stack.getItem()) ||
                        EnchantmentTarget.BOW.isAcceptableItem(stack.getItem()) ||
                        EnchantmentTarget.CROSSBOW.isAcceptableItem(stack.getItem()) ||
                        EnchantmentTarget.ARMOR.isAcceptableItem(stack.getItem()) ||
                        EnchantmentTarget.WEAPON.isAcceptableItem(stack.getItem()));
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        addProperties(delegate);
    }

    public void enchantItem(Identifier enchantment, EnchantmentSlots slots) {
        ItemStack itemStack = inventory.getStack(0);
        itemStack.addEnchantment(Registry.ENCHANTMENT.get(enchantment), 1);
        itemStack.setSubNbt("Slots", slots.asNbt());
        syncState();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        ItemStack itemStack = inventory.getStack(0);
        EnchantmentSlots slots = EnchantmentSlots.fromNbt(itemStack.getSubNbt("Slots"));
        var slotsSize = Slots.values().length;
        var clickedSlot = slots.getSlot(Slots.values()[id / slotsSize]).get();
        var clickedChoice = clickedSlot.getChoice(Slots.values()[id % slotsSize]).get();
        itemStack.addEnchantment(Registry.ENCHANTMENT.get(clickedChoice), 1);
        clickedSlot.setChosen(Slots.values()[id % slotsSize]);
        itemStack.setSubNbt("Slots", slots.asNbt());
        return super.onButtonClick(player, id);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int invSlot) {
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

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
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
