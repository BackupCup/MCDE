package net.backupcup.mcd_enchantments.block.entity;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.backupcup.mcd_enchantments.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlockEntities {
    public static BlockEntityType<RerollStationBlockEntity> REROLL_STATION;
    public static BlockEntityType<RunicTableBlockEntity> RUNIC_TABLE;

    public static void registerBlockEntities() {
        RUNIC_TABLE = Registry.register(
                Registry.BLOCK_ENTITY_TYPE, new Identifier(MCDEnchantments.MOD_ID, "runic_table"),
                FabricBlockEntityTypeBuilder.create(RunicTableBlockEntity::new,
                        ModBlocks.RUNIC_TABLE).build());

        REROLL_STATION = Registry.register(
                Registry.BLOCK_ENTITY_TYPE, new Identifier(MCDEnchantments.MOD_ID, "reroll_station"),
                FabricBlockEntityTypeBuilder.create(RerollStationBlockEntity::new,
                        ModBlocks.REROLL_STATION).build());
    }
}
