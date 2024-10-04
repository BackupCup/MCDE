package net.backupcup.mcde.screen.handler;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.backupcup.mcde.MCDE;
import net.backupcup.mcde.block.ModBlocks;
import net.backupcup.mcde.util.Choice;
import net.backupcup.mcde.util.EnchantmentSlot;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.backupcup.mcde.util.SlotPosition;
import net.backupcup.mcde.util.SlotsGenerator;
import net.backupcup.mcde.util.SlotsGenerator.Builder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
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
    private final PlayerEntity playerEntity;
    private Map<SlotPosition, Boolean> locked = new EnumMap<>(Map.of(SlotPosition.FIRST, false, SlotPosition.SECOND, false, SlotPosition.THIRD, false));
    public static final int REROLL_BUTTON_ID = -1;

    public Inventory getInventory() {
        return inventory;
    }

    public Optional<Boolean> isSlotLocked(SlotPosition slot) {
        return Optional.ofNullable(locked.get(slot));
    }

    public RollBenchScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, ScreenHandlerContext.EMPTY);
    }

    public RollBenchScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(ModScreenHandlers.ROLL_BENCH_SCREEN_HANDLER, syncId);
        this.playerEntity = playerInventory.player;
        this.context = context;
        inventory.onOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, 0, 131, 33) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem().isEnchantable(stack);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        this.addSlot(new Slot(inventory, 1, 131, 52) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.LAPIS_LAZULI) || stack.isOf(Items.ECHO_SHARD);
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
        ItemStack rerollMaterialStack = inventory.getStack(1);
        var slotsOptional = EnchantmentSlots.fromItemStack(itemStack);
        if (slotsOptional.isEmpty()) {
            return onButtonClick(player, id);
        }
        var slots = slotsOptional.get();
        if (id == REROLL_BUTTON_ID) {
            var serverPlayerEntity = context.get((world, pos) -> world.getServer().getPlayerManager().getPlayer(player.getUuid()));
            var gilding = slots.getGildingIds();
            EnchantmentSlots newSlots;
            if (MCDE.getConfig().canFullRerollRemoveSlots()) {
                newSlots = SlotsGenerator.forItemStack(itemStack)
                    .withOptionalOwner(serverPlayerEntity)
                    .build()
                    .generateEnchantments();
            } else {
                var generatorBuilder = SlotsGenerator.forItemStack(itemStack)
                    .withOptionalOwner(serverPlayerEntity);
                if (slots.getEnchantmentSlot(SlotPosition.SECOND).isPresent()) {
                    generatorBuilder.withSecondSlotAbsoluteChance(1f);
                }
                if (slots.getEnchantmentSlot(SlotPosition.THIRD).isPresent()) {
                    generatorBuilder.withThirdSlotAbsoluteChance(1f);
                } 
                newSlots = generatorBuilder.build().generateEnchantments();
            }
            newSlots.addAllGilding(gilding);
            slots.removeChosenFromComponent(itemStack);
            newSlots.updateItemStack(itemStack);
            if (!player.isCreative()) {
                rerollMaterialStack.decrement(1);
            }
            player.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 0.5f, 1f);
            inventory.markDirty();
            return false;
        }
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
            clickedSlot.removeChosenEnchantment(itemStack);
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
            rerollMaterialStack.decrement(slots.getNextRerollCost(enchantmentId));
        }
        MCDE.getConfig().getRerollCostParameters().updateCost(slots);
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
        return lapisLazuliStack.isOf(Items.LAPIS_LAZULI) && lapisLazuliStack.getCount() >= slots.getNextRerollCost(enchantmentId);
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
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        context.run((world, pos) -> {
            dropInventory(player, inventory);
        });
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 2 + l * 18, 84 + i * 19));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 2 + i * 18, 148));
        }
    }

    public List<Identifier> getCandidatesForReroll(SlotPosition clickedSlot) {
        var itemStack = inventory.getStack(0);
        var slotsOptional = EnchantmentSlots.fromItemStack(itemStack);
        if (slotsOptional.isEmpty()) {
            return List.of();
        }
        var slots = slotsOptional.get();
        var candidates = EnchantmentUtils.getEnchantmentsNotInItem(itemStack);
        if (!MCDE.getConfig().isCompatibilityRequired()) {
            return candidates.collect(Collectors.toList());
        }
        var enchantmentsNotInClickedSlot =
            slots.stream().filter(s -> !s.getSlotPosition().equals(clickedSlot))
            .flatMap(s -> s.choices().stream())
            .map(c -> c.getEnchantmentId())
            .toList();
        candidates = candidates.filter(id -> EnchantmentUtils.isCompatible(enchantmentsNotInClickedSlot, id))
            .filter(id -> EnchantmentUtils.isCompatible(EnchantmentHelper.get(itemStack).keySet().stream()
                        .map(EnchantmentUtils::getEnchantmentId)
                        .filter(enchantmentId -> slots.getEnchantmentSlot(clickedSlot)
                            .flatMap(slot -> slot.getChosen())
                            .map(c -> !c.getEnchantmentId().equals(enchantmentId))
                            .orElse(true))
                        .toList(), id));
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
        var locked = slots.stream().collect(Collectors.toMap(
            s -> s.getSlotPosition(),
            s -> generateEnchantment(player, s.getSlotPosition()).isEmpty(),
            (lhs, rhs) -> lhs,
            () -> new EnumMap<>(SlotPosition.class)
        ));
        ServerPlayNetworking.send((ServerPlayerEntity)player, new LockedSlotsPacket(syncId, locked));
    }


    public static void receiveNewLocks(LockedSlotsPacket packet, Context context) {
        var player = context.player();
        if (player == null) {
            return;
        }
        var screenHandler = player.currentScreenHandler;
        if (screenHandler == null) {
            return;
        }

        if (screenHandler.syncId != packet.syncId) {
            return;
        }

        if (screenHandler instanceof RollBenchScreenHandler rbScreenHandler) {
            context.client().execute(() -> {
                rbScreenHandler.locked = packet.locked();
            });
        }
    }

    @Override
    public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
    }

    @Override
    public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
        if (slotId != 0) {
            return;
        }
        EnchantmentSlots.fromItemStack(stack).ifPresent(
            slots -> sendLockedSlots(slots, playerEntity)
        );
    }


    public static record LockedSlotsPacket(int syncId, Map<SlotPosition, Boolean> locked) implements CustomPayload {
        public static final CustomPayload.Id<LockedSlotsPacket> PACKET_ID = new CustomPayload.Id<>(MCDE.id("locked_slots"));
        public static final PacketCodec<RegistryByteBuf, LockedSlotsPacket> PACKET_CODEC = 
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, LockedSlotsPacket::syncId,
                    PacketCodecs.map(
                        n -> new EnumMap<>(SlotPosition.class),
                        SlotPosition.PACKET_CODEC,
                        PacketCodecs.BOOL
                    ), LockedSlotsPacket::locked,
                    LockedSlotsPacket::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }

}
