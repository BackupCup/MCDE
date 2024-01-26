package net.backupcup.mcde.screen.handler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.block.ModBlocks;
import net.backupcup.mcde.block.entity.GildingFoundryBlockEntity;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public class GildingFoundryScreenHandler extends ScreenHandler implements ScreenHandlerListener {
    public static final Identifier GILDING_PACKET = Identifier.of(MCDEnchantments.MOD_ID, "gilding");
    private final Inventory inventory;
    private final PlayerEntity playerEntity;
    private final ScreenHandlerContext context;
    private final PropertyDelegate propertyDelegate;
    private Optional<Identifier> generatedEnchantment = Optional.empty();

    public Optional<Identifier> getGeneratedEnchantment() {
        return generatedEnchantment;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean hasEnchantmentForGilding() {
        return generatedEnchantment.isPresent();
    }

    public GildingFoundryScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, new SimpleInventory(2), new ArrayPropertyDelegate(1), ScreenHandlerContext.EMPTY);
    }

    public GildingFoundryScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate delegate, ScreenHandlerContext context) {
        super(ModScreenHandlers.GILDING_FOUNDRY_SCREEN_HANDLER, syncId);
        this.context = context;
        this.playerEntity = playerInventory.player;
        checkSize(inventory, 1);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);
        this.propertyDelegate = delegate;

        this.addSlot(new Slot(inventory, 0, 82, 17) {

            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem().isEnchantable(stack);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        this.addSlot(new Slot(inventory, 1, 82, 53) {

            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.GOLD_INGOT);
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
        var weaponStack = inventory.getStack(0);
        var enchantmentId = generatedEnchantment.get();
        if (player.isCreative()) {
            EnchantmentSlots.fromItemStack(weaponStack).ifPresent(slots -> {
                slots.addGilding(enchantmentId);
                slots.updateItemStack(weaponStack);
            });
            return false;
        }
        context.run((world, pos) -> {
            ((GildingFoundryBlockEntity)world.getBlockEntity(pos))
                .setGenerated(enchantmentId);
        });
        startProgress();
        return false;
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

    public List<Identifier> getCandidatesForGidling() {
        return EnchantmentUtils.getEnchantmentsForItem(inventory.getStack(0)).collect(Collectors.toList());
    }

    public Optional<Identifier> generateEnchantment(PlayerEntity player) {
        return EnchantmentUtils.generateEnchantment(
            inventory.getStack(0),
            context.get((world, pos) -> world.getServer().getPlayerManager().getPlayer(player.getUuid())),
            getCandidatesForGidling()
        );
    }

    public static void receiveNewEnchantment(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
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

        if (screenHandler instanceof GildingFoundryScreenHandler gfScreenHandler) {
            gfScreenHandler.generatedEnchantment = buf.readOptional(b -> b.readIdentifier());
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

        generatedEnchantment = generateEnchantment(playerEntity);
        if (generatedEnchantment.isEmpty()) {
            return;
        }
        context.run((world, pos) -> {
            var buffer = PacketByteBufs.create();
            buffer.writeInt(syncId);
            buffer.writeOptional(generatedEnchantment, (buf, e) -> buf.writeIdentifier(e));
            ServerPlayNetworking.send(world.getServer().getPlayerManager().getPlayer(playerEntity.getUuid()), GILDING_PACKET, buffer);
        });
    }
}
