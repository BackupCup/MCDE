package net.backupcup.mcde.block;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.block.custom.GildingFoundryBlock;
import net.backupcup.mcde.block.custom.RollBenchBlock;
import net.backupcup.mcde.block.custom.RunicTableBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block RUNIC_TABLE = registerBlock("runic_table",
            new RunicTableBlock(FabricBlockSettings.of(Material.WOOD).strength(2f).nonOpaque()), ItemGroups.FUNCTIONAL);

    public static final Block ROLL_BENCH = registerBlock("roll_bench",
            new RollBenchBlock(FabricBlockSettings.of(Material.STONE).strength(2f).nonOpaque()), ItemGroups.FUNCTIONAL);
    public static final Block GILDING_FOUNDRY = registerBlock("gilding_foundry",
            new GildingFoundryBlock(FabricBlockSettings.of(Material.STONE).strength(2f).nonOpaque()), ItemGroups.FUNCTIONAL);

    public static Block registerBlock(String name, Block block, ItemGroup tab) {
        registerBlockItem(name, block, tab);
        return Registry.register(Registries.BLOCK, new Identifier(MCDEnchantments.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block, ItemGroup tab) {
        var item = Registry.register(Registries.ITEM, new Identifier(MCDEnchantments.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
        ItemGroupEvents.modifyEntriesEvent(tab).register(entries -> entries.add(item));

        return item;
    }
}
