package net.backupcup.mcde.block.entity;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlockEntities {
    public static BlockEntityType<GildingFoundryBlockEntity> GILDING_FOUNDRY;

    public static void registerBlockEntities() {
        GILDING_FOUNDRY = Registry.register(
                Registry.BLOCK_ENTITY_TYPE, new Identifier(MCDEnchantments.MOD_ID, "gilding_foundry"),
                FabricBlockEntityTypeBuilder.create(GildingFoundryBlockEntity::new,
                        ModBlocks.GILDING_FOUNDRY).build());
    }
}
