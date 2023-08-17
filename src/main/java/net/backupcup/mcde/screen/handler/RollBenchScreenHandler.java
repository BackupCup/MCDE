package net.backupcup.mcde.screen.handler;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.block.ModBlocks;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.backupcup.mcde.util.SlotPosition;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class RollBenchScreenHandler extends ScreenHandler implements ScreenHandlerListener {
    private final Inventory inventory = new SimpleInventory(2);
    private final ScreenHandlerContext context;
    private final PlayerEntity player;
    private Map<SlotPosition, Boolean> locked = new EnumMap<>(Map.of(SlotPosition.FIRST, false, SlotPosition.SECOND, false, SlotPosition.THIRD, false));
    public static final Identifier LOCKED_SLOTS_PACKET = Identifier.of(MCDEnchantments.MOD_ID, "locked_slots");

    public Inventory getInventory() {
        return inventory;
    }

    public boolean isSlotLocked(SlotPosition slot) {
        return locked.get(slot);
    }

    public RollBenchScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, ScreenHandlerContext.EMPTY);
    }

    public RollBenchScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(ModScreenHandlers.ROLL_BENCH_SCREEN_HANDLER, syncId);
        this.player = playerInventory.player;
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
        addListener(this);

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        ItemStack itemStack = inventory.getStack(0);
        ItemStack lapisLazuliStack = inventory.getStack(1);
        EnchantmentSlots slots = EnchantmentSlots.fromItemStack(itemStack);

        var slotsSize = SlotPosition.values().length;
        var clickedSlot = slots.getEnchantmentSlot(SlotPosition.values()[id / slotsSize]).get();
        SlotPosition toChange;
        Identifier enchantmentId;
        var newEnchantment = generateEnchantment(player, clickedSlot.getSlotPosition());
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
            toChange = chosen.getChoicePosition();
        } else {
            toChange = SlotPosition.values()[id % slotsSize];
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

    public List<Identifier> getCandidatesForReroll(SlotPosition clickedSlot) {
        var itemStack = inventory.getStack(0);
        var slots = EnchantmentSlots.fromItemStack(itemStack);
        var candidates = EnchantmentUtils.getEnchantmentsNotInItem(itemStack);
        if (!MCDEnchantments.getConfig().isCompatibilityRequired()) {
            return candidates.toList();
        }
        var enchantmentsNotInClickedSlot =
            slots.stream().filter(s -> !s.getSlotPosition().equals(clickedSlot))
            .flatMap(s -> s.choices().stream())
            .map(c -> c.getEnchantmentId())
            .toList();
        candidates = candidates.filter(id -> EnchantmentUtils.isCompatible(enchantmentsNotInClickedSlot, id))
            .filter(id -> EnchantmentUtils.isCompatible(EnchantmentHelper.get(itemStack).keySet().stream()
                        .map(EnchantmentUtils::getEnchantmentId).toList(), id));
        return candidates.collect(Collectors.toList());
    }

    public Optional<Identifier> generateEnchantment(PlayerEntity player, SlotPosition clickedSlot) {
        return EnchantmentUtils.generateEnchantment(
            inventory.getStack(0),
            context.get((world, pos) -> world.getServer().getPlayerManager().getPlayer(player.getUuid())),
            getCandidatesForReroll(clickedSlot)
        );
    }

    private void sendLockedSlots(EnchantmentSlots slots, PlayerEntity player) {
        var buffer = PacketByteBufs.create();
        var locked = slots.stream().collect(Collectors.toMap(
            s -> s.getSlotPosition(),
            s -> generateEnchantment(player, s.getSlotPosition()).isEmpty(),
            (lhs, rhs) -> lhs,
            () -> new EnumMap<>(SlotPosition.class)
        ));
        buffer.writeInt(syncId);
        buffer.writeMap(locked, (buf, s) -> buf.writeEnumConstant(s), (buf, b) -> buf.writeBoolean(b));
        ServerPlayNetworking.send((ServerPlayerEntity)player, LOCKED_SLOTS_PACKET, buffer);
    }


    public static void receiveNewLocks(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        var player = client.player;
        if (player == null) {
            return;
        }
        var screenHandler = player.currentScreenHandler;
        if (screenHandler == null) {
            return;
        }

        if (screenHandler.syncId != buf.readInt()) {
            return;
        }

        var map = buf.readMap(b -> b.readEnumConstant(SlotPosition.class), b -> b.readBoolean());

        if (screenHandler instanceof RollBenchScreenHandler rbScreenHandler) {
            client.execute(() -> {
                rbScreenHandler.locked = map;
            });
        }
    }


    @Override
    public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
    }

    @Override
    public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
        var slots = EnchantmentSlots.fromItemStack(stack);
        if (slotId != 0 || slots == null) {
            return;
        }
        sendLockedSlots(slots, player);
    }
}
