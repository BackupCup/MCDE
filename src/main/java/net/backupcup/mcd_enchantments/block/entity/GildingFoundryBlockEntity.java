package net.backupcup.mcd_enchantments.block.entity;

import org.jetbrains.annotations.Nullable;

import net.backupcup.mcd_enchantments.screen.GildingFoundryScreenHandler;
import net.backupcup.mcd_enchantments.util.EnchantmentUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GildingFoundryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private int gilding_progress;
    private int gilding_tick_counter;
    private static final int TICKS_PER_PROGRESS_STEP = 1;

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
        return Text.translatable("block.mcd_enchantments.gilding_foundry");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new GildingFoundryScreenHandler(syncId, inv, this, this.propertyDelegate);
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
        if (world.isClient() || entity.gilding_progress == 0) {
            return;
        }
        if (entity.inventory.get(0).isEmpty() || entity.inventory.get(1).isEmpty()) {
            entity.resetProgress();
            markDirty(world, blockPos, state);
            return;
        }
        entity.gilding_tick_counter++;
        entity.gilding_tick_counter %= TICKS_PER_PROGRESS_STEP;
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
        inventory.get(1).decrement(1);
        var weaponStack = inventory.get(0);
        var gildedEnchantment = EnchantmentUtils.generateEnchantment(weaponStack);
        if (gildedEnchantment.isEmpty()) {
            return;
        }
        var id = gildedEnchantment.get();
        weaponStack.setSubNbt("Gilding", NbtString.of(id.toString()));
    }

    private void resetProgress() {
        gilding_progress = 0;
        gilding_tick_counter = 0;
    }
}
