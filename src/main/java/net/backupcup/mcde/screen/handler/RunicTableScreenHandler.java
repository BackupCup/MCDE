package net.backupcup.mcde.screen.handler;

import net.backupcup.mcde.MCDE;
import net.backupcup.mcde.block.ModBlocks;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.backupcup.mcde.util.SlotPosition;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;

public class RunicTableScreenHandler extends ScreenHandler {
    private final Inventory inventory = new SimpleInventory(1);
    private final ScreenHandlerContext context;
    public Inventory getInventory() {
        return inventory;
    }

    public RunicTableScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, ScreenHandlerContext.EMPTY);
    }

    public RunicTableScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(ModScreenHandlers.RUNIC_TABLE_SCREEN_HANDLER, syncId);

        checkSize(inventory, 1);
        this.context = context;
        inventory.onOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, 0, 132, 43) {

            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem().isEnchantable(stack);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        addListener(EnchantmentUtils.generatorListener(context, playerInventory.player));
        inventory.markDirty();

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        ItemStack itemStack = inventory.getStack(0);
        var slotsOptional = EnchantmentSlots.fromItemStack(itemStack);
        if (slotsOptional.isEmpty()) {
            return onButtonClick(player, id);
        }
        var slots = slotsOptional.get();
        var posAmount = SlotPosition.values().length;
        var clickedSlot = slots.getEnchantmentSlot(SlotPosition.values()[id / posAmount]).get();
        var chosen = clickedSlot.getChosen();
        int level = 1;
        RegistryEntry<Enchantment> enchantment;
        if (chosen.isPresent()) {
            if (chosen.get().isMaxedOut()) {
                return super.onButtonClick(player, id);
            }
            clickedSlot = clickedSlot.withUpgrade();
            enchantment = chosen.get().getEnchantment();
            level = clickedSlot.getLevel();
            if (!canEnchant(player, enchantment, level)) {
                return super.onButtonClick(player, id);
            }
        } else {
            int choicePos = id % posAmount;
            enchantment = clickedSlot.getChoice(SlotPosition.values()[choicePos]).get();
            if (!canEnchant(player, enchantment, level)) {
                return super.onButtonClick(player, id);
            }
            clickedSlot = clickedSlot.withChosen(SlotPosition.values()[choicePos], level).orElse(clickedSlot);
        }
        var componentBuilder = new ItemEnchantmentsComponent.Builder(EnchantmentHelper.getEnchantments(itemStack));
        componentBuilder.set(enchantment, level);
        itemStack.applyChanges(ComponentChanges.builder()
                .add(DataComponentTypes.ENCHANTMENTS, componentBuilder.build())
                .add(EnchantmentSlots.COMPONENT_TYPE, EnchantmentSlots.builder(slots).withSlot(clickedSlot).build())
                .build());
        if (clickedSlot.isMaxedOut()) {
            player.incrementStat(Stats.ENCHANT_ITEM);
        }
        player.playSoundToPlayer(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 0.5f, 1f);
        if (!player.isCreative()) {
            player.addExperienceLevels(-MCDE.getConfig().getEnchantCost(enchantment, level));
        }
        inventory.markDirty();
        context.run((world, pos) -> {
            var server = world.getServer();
            var tracker = server.getPlayerManager().getPlayer(player.getUuid()).getAdvancementTracker();
            var advancement = server.getAdvancementLoader().get(Identifier.of("minecraft", "story/enchant_item"));
            tracker.grantCriterion(advancement, "enchanted_item");
        });
        return super.onButtonClick(player, id);
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
        return canUse(context, player, ModBlocks.RUNIC_TABLE);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        context.run((world, pos) -> {
            dropInventory(player, inventory);
        });
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 3 + l * 18, 84 + i * 19));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 3 + i * 18, 148));
        }
    }

    public static boolean canEnchant(PlayerEntity player, RegistryEntry<Enchantment> enchantment, int level) {
        if (player.isCreative()) {
            return true;
        }
        return player.experienceLevel >= MCDE.getConfig().getEnchantCost(enchantment, level);
    }
}
