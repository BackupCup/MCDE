package net.backupcup.mcde.screen.handler;

import java.util.List;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.block.ModBlocks;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.backupcup.mcde.util.Slots;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class RollBenchScreenHandler extends ScreenHandler {
    private final Inventory inventory = new SimpleInventory(2);
    private final ScreenHandlerContext context;
    public Inventory getInventory() {
        return inventory;
    }

    public RollBenchScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, ScreenHandlerContext.EMPTY);
    }

    public RollBenchScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(ModScreenHandlers.ROLL_BENCH_SCREEN_HANDLER, syncId);

        this.context = context;
        inventory.onOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, 0, 145, 33) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem().isEnchantable(stack);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        this.addSlot(new Slot(inventory, 1, 145, 52) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return (stack.getItem() == Items.LAPIS_LAZULI);
            }

            @Override
            public int getMaxItemCount() {
                return 64;
            }
        });

        addListener(EnchantmentUtils.generatorListener(context, playerInventory.player));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        ItemStack itemStack = inventory.getStack(0);
        ItemStack lapisLazuliStack = inventory.getStack(1);
        EnchantmentSlots slots = EnchantmentSlots.fromItemStack(itemStack);

        var slotsSize = Slots.values().length;
        var clickedSlot = slots.getSlot(Slots.values()[id / slotsSize]).get();
        Slots toChange;
        Identifier enchantmentId;
        var newEnchantment = EnchantmentUtils.generateEnchantment(itemStack, getCandidatesForReroll(itemStack, slots, clickedSlot.getSlot()));
        if (newEnchantment.isEmpty()) {
            return super.onButtonClick(player, id);
        }

        if (clickedSlot.getChosen().isPresent()) {
            var chosen = clickedSlot.getChosen().get();
            enchantmentId = chosen.getEnchantmentId();

            if (!canReroll(player, enchantmentId, slots)) {
                return super.onButtonClick(player, id);
            }
            clickedSlot.clearChoice();
            toChange = chosen.getSlot();
        } else {
            toChange = Slots.values()[id % slotsSize];
            enchantmentId = clickedSlot.getChoice(toChange).get();

            if (!canReroll(player, enchantmentId, slots)) {
                return super.onButtonClick(player, id);
            }
        }

        clickedSlot.changeEnchantment(toChange, newEnchantment.get());
        if (!player.isCreative()) {
            lapisLazuliStack.decrement(slots.getNextRerollCost(enchantmentId));
        }
        MCDEnchantments.getConfig().getRerollCostParameters().updateCost(slots);
        slots.updateItemStack(itemStack);
        player.playSound(SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.5f, 1f);
        inventory.markDirty();
        return super.onButtonClick(player, id);
    }

    public boolean canReroll(PlayerEntity player, Identifier enchantmentId, EnchantmentSlots slots) {
        if (player.isCreative()) {
            return true;
        }
        ItemStack lapisLazuliStack = inventory.getStack(1);
        return lapisLazuliStack.getCount() >= slots.getNextRerollCost(enchantmentId);
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

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(context, player, ModBlocks.ROLL_BENCH);
    }

    @Override
    public void close(PlayerEntity player) {
        super.close(player);
        context.run((world, pos) -> {
            dropInventory(player, inventory);
        });
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

    public static List<Identifier> getCandidatesForReroll(ItemStack itemStack, EnchantmentSlots slots, Slots clickedSlot) {
        var candidates = EnchantmentUtils.getEnchantmentsNotInItem(itemStack);
        if (!MCDEnchantments.getConfig().isCompatibilityRequired()) {
            return candidates.toList();
        }
        var enchantmentsNotInClickedSlot =
            slots.stream().filter(s -> !s.getSlot().equals(clickedSlot))
            .flatMap(s -> s.choices().stream())
            .map(c -> c.getEnchantmentId())
            .toList();
        candidates = candidates.filter(id -> EnchantmentUtils.isCompatible(enchantmentsNotInClickedSlot, id))
            .filter(id -> EnchantmentUtils.isCompatible(EnchantmentHelper.get(itemStack).keySet().stream()
                        .map(EnchantmentUtils::getEnchantmentId).toList(), id));
        return candidates.toList();
    }
}
