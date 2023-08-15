package net.backupcup.mcde.block.entity;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.screen.handler.GildingFoundryScreenHandler;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GildingFoundryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private int gilding_progress;
    private int gilding_tick_counter;
    private Optional<ServerPlayerEntity> lastUser = Optional.empty();

    public Optional<ServerPlayerEntity> getLastUser() {
        return lastUser;
    }

    public void setLastUser(ServerPlayerEntity lastUser) {
        this.lastUser = Optional.of(lastUser);
    }

    public DefaultedList<ItemStack> getInventory() {
        return inventory;
    }

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {

        @Override
        public int get(int index) {
            return gilding_progress;
        }

        @Override
        public void set(int index, int value) {
            gilding_progress = value;
        }

        @Override
        public int size() {
            return 1;
        }

    };

    public GildingFoundryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GILDING_FOUNDRY, pos, state);
    }

    public boolean hasProgress() {
        return gilding_progress != 0;
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.mcde.gilding_foundry");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new GildingFoundryScreenHandler(syncId, inv, this, this.propertyDelegate, ScreenHandlerContext.create(world, pos));
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, inventory);
        super.readNbt(nbt);
    }

    public static void tick(World world, BlockPos blockPos, BlockState state, GildingFoundryBlockEntity entity) {
        if (world.isClient()) {
            return;
        }

        if (entity.gilding_progress == 0) {
            return;
        }
        if (entity.inventory.get(0).isEmpty() || entity.inventory.get(1).getCount() < MCDEnchantments.getConfig().getGildingCost()) {
            entity.resetProgress();
            markDirty(world, blockPos, state);
            return;
        }
        entity.gilding_tick_counter++;
        entity.gilding_tick_counter %= MCDEnchantments.getConfig().getTicksPerGildingProcessStep();
        if (entity.gilding_tick_counter == 0) {
            entity.gilding_progress++;
            if (entity.gilding_progress >= 34) {
                entity.finishGilding();
            }
        }
        markDirty(world, blockPos, state);
    }

    private void finishGilding() {
        gilding_progress = 0;
        gilding_tick_counter = 0;
        inventory.get(1).decrement(MCDEnchantments.getConfig().getGildingCost());
        var weaponStack = inventory.get(0);
        
        var gildedEnchantment = EnchantmentUtils.generateEnchantment(weaponStack, lastUser, getCandidatesForGidling());
        if (gildedEnchantment.isEmpty()) {
            return;
        }
        var id = gildedEnchantment.get();
        var slots = EnchantmentSlots.fromItemStack(weaponStack);
        slots.setGilding(id);
        slots.updateItemStack(weaponStack);
    }

    private void resetProgress() {
        gilding_progress = 0;
        gilding_tick_counter = 0;
    }

    public List<Identifier> getCandidatesForGidling() {
        return EnchantmentUtils.getEnchantmentsForItem(inventory.get(0)).collect(Collectors.toList());
    }
}
