package net.backupcup.mcde.block;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.block.custom.GildingFoundryBlock;
import net.backupcup.mcde.block.custom.RollBenchBlock;
import net.backupcup.mcde.block.custom.RunicTableBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlocks {

    public static final Block RUNIC_TABLE = registerBlock("runic_table",
            new RunicTableBlock(FabricBlockSettings.of(Material.WOOD).strength(2f).nonOpaque()), ItemGroup.DECORATIONS);

    public static final Block ROLL_BENCH = registerBlock("roll_bench",
            new RollBenchBlock(FabricBlockSettings.of(Material.STONE).strength(2f).nonOpaque()), ItemGroup.DECORATIONS);
    public static final Block GILDING_FOUNDRY = registerBlock("gilding_foundry",
            new GildingFoundryBlock(FabricBlockSettings.of(Material.STONE).strength(2f).nonOpaque()), ItemGroup.DECORATIONS);

    public static Block registerBlock(String name, Block block, ItemGroup tab) {
        registerBlockItem(name, block, tab);
        return Registry.register(Registry.BLOCK, new Identifier(MCDEnchantments.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block, ItemGroup tab) {
        return Registry.register(Registry.ITEM, new Identifier(MCDEnchantments.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings().group(tab)));
    }

    public static void RegisterModBlocks() {
        MCDEnchantments.LOGGER.debug("Registering Mod Blocks");
    }
}
