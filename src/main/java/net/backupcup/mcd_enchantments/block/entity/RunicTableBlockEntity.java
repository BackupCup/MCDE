package net.backupcup.mcd_enchantments.block.entity;

import org.jetbrains.annotations.Nullable;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.backupcup.mcd_enchantments.screen.RunicTableScreenHandler;
import net.backupcup.mcd_enchantments.util.EnchantmentSlots;
import net.backupcup.mcd_enchantments.util.EnchantmentUtils;
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
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class RunicTableBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private boolean slotsRead = false;

    public DefaultedList<ItemStack> getInventory() {
        return inventory;
    }
    protected final PropertyDelegate propertyDelegate;


    public RunicTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNIC_TABLE, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                return 0;
            }

            @Override
            public void set(int index, int value) {

            }

            @Override
            public int size() {
                return 0;
            }
        };
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.mcd_enchantments.runic_table");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new RunicTableScreenHandler(syncId, inv, this, this.propertyDelegate);
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

    public static void tick(World world, BlockPos blockPos, BlockState state, RunicTableBlockEntity entity) {
        if (world.isClient()) {
            return;
        }
        DefaultedList<ItemStack> inventory = entity.getItems();
        ItemStack itemStack = inventory.get(0);

        if (itemStack.isEmpty()) {
            entity.slotsRead = false;
            return;
        }
        if (!itemStack.getNbt().contains("Slots")) {
            EnchantmentSlots slots = EnchantmentUtils.getEnchantments(itemStack.getItem());
            slots.updateItemStack(itemStack);
            MCDEnchantments.LOGGER.info("Generated slots for [{}]: {}", Registry.ITEM.getId(itemStack.getItem()), slots);
        }
        else if (!entity.slotsRead) {
            EnchantmentSlots slots = EnchantmentSlots.fromItemStack(itemStack);
            MCDEnchantments.LOGGER.info("Read slots [{}]: {}", Registry.ITEM.getId(itemStack.getItem()), slots);
            entity.slotsRead = true;
        }
    }
}
