package net.backupcup.mcde.block.entity;

import net.backupcup.mcde.MCDE;
import net.backupcup.mcde.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModBlockEntities {
    public static BlockEntityType<GildingFoundryBlockEntity> GILDING_FOUNDRY;

    public static void registerBlockEntities() {
        GILDING_FOUNDRY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE, MCDE.id("gilding_foundry"),
                BlockEntityType.Builder.create(GildingFoundryBlockEntity::new,
                        ModBlocks.GILDING_FOUNDRY).build());
    }
}
