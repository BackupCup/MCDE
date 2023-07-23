package net.backupcup.mcde.block.entity;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlockEntities {
    public static BlockEntityType<RunicTableBlockEntity> RUNIC_TABLE;
    public static BlockEntityType<RollBenchBlockEntity> ROLL_BENCH;
    public static BlockEntityType<GildingFoundryBlockEntity> GILDING_FOUNDRY;

    public static void registerBlockEntities() {
        RUNIC_TABLE = Registry.register(
                Registry.BLOCK_ENTITY_TYPE, new Identifier(MCDEnchantments.MOD_ID, "runic_table"),
                FabricBlockEntityTypeBuilder.create(RunicTableBlockEntity::new,
                        ModBlocks.RUNIC_TABLE).build());

        ROLL_BENCH = Registry.register(
                Registry.BLOCK_ENTITY_TYPE, new Identifier(MCDEnchantments.MOD_ID, "roll_bench"),
                FabricBlockEntityTypeBuilder.create(RollBenchBlockEntity::new,
                        ModBlocks.ROLL_BENCH).build());

        GILDING_FOUNDRY = Registry.register(
                Registry.BLOCK_ENTITY_TYPE, new Identifier(MCDEnchantments.MOD_ID, "gilding_foundry"),
                FabricBlockEntityTypeBuilder.create(GildingFoundryBlockEntity::new,
                        ModBlocks.GILDING_FOUNDRY).build());
    }
}
