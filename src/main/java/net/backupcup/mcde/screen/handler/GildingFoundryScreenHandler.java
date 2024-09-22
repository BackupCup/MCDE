package net.backupcup.mcde.screen.handler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.backupcup.mcde.MCDE;
import net.backupcup.mcde.block.ModBlocks;
import net.backupcup.mcde.block.entity.GildingFoundryBlockEntity;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
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
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class GildingFoundryScreenHandler extends ScreenHandler implements ScreenHandlerListener {
    private final Inventory inventory;
    private final PlayerEntity playerEntity;
    private final ScreenHandlerContext context;
    private final PropertyDelegate propertyDelegate;
    private Optional<Identifier> generatedEnchantment = Optional.empty();
    public static final PacketCodec<RegistryByteBuf, Optional<Identifier>> GENERATED_ENCHANTMENT_CODEC =
        PacketCodecs.optional(Identifier.PACKET_CODEC).cast();

    public Optional<Identifier> getGeneratedEnchantment() {
        return generatedEnchantment;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean hasEnchantmentForGilding() {
        return generatedEnchantment.isPresent();
    }

    public GildingFoundryScreenHandler(int syncId, PlayerInventory inventory, Optional<Identifier> generatedEnchantment) {
        this(
            syncId,
            inventory,
            new SimpleInventory(2),
            new ArrayPropertyDelegate(1),
            ScreenHandlerContext.EMPTY,
            generatedEnchantment
        );
    }

    public GildingFoundryScreenHandler(
        int syncId,
        PlayerInventory playerInventory,
        Inventory inventory,
        PropertyDelegate delegate,
        ScreenHandlerContext context,
        Optional<Identifier> generatedEnchantment
    ) {
        super(ModScreenHandlers.GILDING_FOUNDRY_SCREEN_HANDLER, syncId);
        this.context = context;
        this.playerEntity = playerInventory.player;
        checkSize(inventory, 2);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);
        this.propertyDelegate = delegate;
        this.generatedEnchantment = generatedEnchantment;

        this.addSlot(new Slot(inventory, 0, 74, 9) {

            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem().isEnchantable(stack);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        this.addSlot(new Slot(inventory, 1, 74, 37) {

            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.GOLD_INGOT) || stack .isOf(Items.EMERALD);
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

        addProperties(delegate);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (generatedEnchantment.isEmpty()) {
            return false;
        }
        var weaponStack = player.isCreative() ? inventory.getStack(0) : inventory.getStack(0).copy();
        var enchantmentId = generatedEnchantment.get();
        EnchantmentSlots.fromItemStack(weaponStack).ifPresent(slots -> {
            if (slots.hasGilding()) {
                var map = EnchantmentHelper.get(weaponStack);
                map.keySet().removeAll(slots.getGildingEnchantments());
                EnchantmentHelper.set(map, weaponStack);
                slots.removeAllGildings();
            }
            slots.addGilding(enchantmentId);
            slots.updateItemStack(weaponStack);
        });
        setNewEnchantment(player, weaponStack);

        if (!player.isCreative()) {
            context.run((world, pos) -> {
                ((GildingFoundryBlockEntity)world.getBlockEntity(pos))
                    .setGenerated(enchantmentId);
            });
            startProgress();
        }
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
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 2 + l * 18, 80 + i * 19));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 2 + i * 18, 144));
        }
    }

    public static List<Identifier> getCandidatesForGidling(ItemStack itemStack) {
        return EnchantmentUtils.getEnchantmentsForItem(itemStack).collect(Collectors.toList());
    }

    public List<Identifier> getCandidatesForGidling() {
        return getCandidatesForGidling(inventory.getStack(0));
    }

    public void setNewEnchantment(PlayerEntity player, ItemStack weaponStack) {
        context.run((world, pos) -> {
            ServerPlayerEntity serverPlayer = world.getServer().getPlayerManager().getPlayer(player.getUuid());
            generatedEnchantment = EnchantmentUtils.generateEnchantment(
                weaponStack,
                Optional.of(serverPlayer),
                getCandidatesForGidling()
            );
            ServerPlayNetworking.send(serverPlayer, new GildingPacket(syncId, generatedEnchantment));
        });
    }

    public static void receiveNewEnchantment(GildingPacket packet, Context context) {
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

        if (screenHandler instanceof GildingFoundryScreenHandler gfScreenHandler) {
            gfScreenHandler.generatedEnchantment = packet.enchantment;
        }
    }

    @Override
    public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
    }

    @Override
    public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
        if (slotId != 0 || EnchantmentSlots.fromItemStack(stack).isEmpty()) {
            return;
        }

        setNewEnchantment(playerEntity, stack);
    }

    public static record GildingPacket(int syncId, Optional<Identifier> enchantment) implements CustomPayload {
        public static final CustomPayload.Id<GildingPacket> PACKET_ID = new CustomPayload.Id<>(MCDE.id("gilding"));
        public static final PacketCodec<RegistryByteBuf, GildingPacket> PACKET_CODEC =
            PacketCodec.tuple(
                PacketCodecs.VAR_INT, GildingPacket::syncId,
                PacketCodecs.optional(Identifier.PACKET_CODEC), GildingPacket::enchantment,
                GildingPacket::new
            );

        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }

}
