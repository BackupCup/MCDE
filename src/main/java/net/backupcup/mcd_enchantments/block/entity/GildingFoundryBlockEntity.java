package net.backupcup.mcd_enchantments.block.entity;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.backupcup.mcd_enchantments.screen.GildingFoundryScreenHandler;
import net.backupcup.mcd_enchantments.util.EnchantmentSlots;
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
import org.jetbrains.annotations.Nullable;

public class GildingFoundryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private int gilding_progress;
    private int gilding_tick_counter;
    private static final int TICKS_PER_PROGRESS_STEP = 1;
    private boolean slotsRead = false;

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
        if (world.isClient()) {
            return;
        }
        entity.generateEnchantments(entity);

        if (entity.gilding_progress == 0) {
            return;
        }
        if (entity.inventory.get(0).isEmpty() || entity.inventory.get(1).getCount() < 8) {
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

    private void generateEnchantments(GildingFoundryBlockEntity entity) {
        DefaultedList<ItemStack> inventory = entity.getItems();
        ItemStack itemStack = inventory.get(0);

        if (itemStack.isEmpty()) {
            entity.slotsRead = false;
            return;
        }
        if (!itemStack.getNbt().contains("Slots")) {
            EnchantmentSlots slots = EnchantmentUtils.generateEnchantments(itemStack);
            slots.updateItemStack(itemStack);
            MCDEnchantments.LOGGER.info("Generated slots for [{}]: {}", Registry.ITEM.getId(itemStack.getItem()), slots);
        }
        else if (!entity.slotsRead) {
            EnchantmentSlots slots = EnchantmentSlots.fromItemStack(itemStack);
            MCDEnchantments.LOGGER.info("Read slots [{}]: {}", Registry.ITEM.getId(itemStack.getItem()), slots);
            entity.slotsRead = true;
            for (var nbt : itemStack.getEnchantments()) {
                MCDEnchantments.LOGGER.info("Existing: {}", nbt.asString());
            }
        }
    }

    private void finishGilding() {
        gilding_progress = 0;
        gilding_tick_counter = 0;
        inventory.get(1).decrement(8);
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
