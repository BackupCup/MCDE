package net.backupcup.mcde.block.entity;

import net.backupcup.mcde.MCDE;
import net.backupcup.mcde.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static BlockEntityType<GildingFoundryBlockEntity> GILDING_FOUNDRY;

    public static void registerBlockEntities() {
        GILDING_FOUNDRY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE, new Identifier(MCDE.MOD_ID, "gilding_foundry"),
                FabricBlockEntityTypeBuilder.create(GildingFoundryBlockEntity::new,
                        ModBlocks.GILDING_FOUNDRY).build());
    }
}
