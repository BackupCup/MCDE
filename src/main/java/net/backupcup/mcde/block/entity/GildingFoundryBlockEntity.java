package net.backupcup.mcde.block.entity;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.backupcup.mcde.MCDE;
import net.backupcup.mcde.screen.handler.GildingFoundryScreenHandler;
import net.backupcup.mcde.util.EnchantmentSlots;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GildingFoundryBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<Optional<RegistryEntry<Enchantment>>>, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private int gilding_progress;
    private Optional<RegistryEntry<Enchantment>> generated = Optional.empty();

    public void setGenerated(RegistryEntry<Enchantment> enchantment) {
        generated = Optional.of(enchantment);
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
        var weaponStack  = inventory.get(0);
        if (!weaponStack.isEmpty() && generated.isEmpty()) {
            generated = EnchantmentUtils.generateEnchantment(
                weaponStack,
                Optional.of(world.getServer().getPlayerManager().getPlayer(player.getUuid())),
                GildingFoundryScreenHandler.getCandidatesForGidling(world, weaponStack)
            ).map(RegistryEntry.class::cast);
        }
        return new GildingFoundryScreenHandler(
            syncId,
            inv,
            this,
            this.propertyDelegate,
            ScreenHandlerContext.create(world, pos),
            generated
        );
    }

    public static void tick(World world, BlockPos blockPos, BlockState state, GildingFoundryBlockEntity entity) {
        if (world.isClient()) {
            return;
        }

        if (entity.gilding_progress == 0) {
            return;
        }
        if (entity.inventory.get(0).isEmpty() || entity.inventory.get(1).getCount() < MCDE.getConfig().getGildingCost()) {
            entity.resetProgress();
            markDirty(world, blockPos, state);
            return;
        }
        entity.gilding_progress++;
        if (entity.gilding_progress > MCDE.getConfig().getGildingDuration()) {
            entity.finishGilding();
        }

        markDirty(world, blockPos, state);
    }

    private void finishGilding() {
        gilding_progress = 0;
        var weaponStack = inventory.get(0);
        
        if (generated.isEmpty()) {
            return;
        }
        var ingridient = inventory.get(1);
        var item = ingridient.getItem();
        ingridient.decrement(MCDE.getConfig().getGildingCost());
        var enchantment = generated.get();
        EnchantmentSlots.fromItemStack(weaponStack).ifPresent(slots -> {
            var slotsBuilder = EnchantmentSlots.builder(slots);
            var enchantmentBuilder = new ItemEnchantmentsComponent.Builder(weaponStack.getEnchantments());
            if (item.equals(Items.EMERALD)) {
                for (var gild : slots.getGilding()) {
                    enchantmentBuilder.set(gild, 0);
                }
                slotsBuilder.clearGildings();
            }
            slotsBuilder.addGilding(enchantment);
            enchantmentBuilder.add(enchantment, 1);
            weaponStack.applyChanges(ComponentChanges.builder()
                    .add(DataComponentTypes.ENCHANTMENTS, enchantmentBuilder.build())
                    .add(EnchantmentSlots.COMPONENT_TYPE, slotsBuilder.build())
                    .build());
        });
    }

    private void resetProgress() {
        gilding_progress = 0;
    }

    @Override
    public Optional<RegistryEntry<Enchantment>> getScreenOpeningData(ServerPlayerEntity player) {
        return generated;
    }
}
