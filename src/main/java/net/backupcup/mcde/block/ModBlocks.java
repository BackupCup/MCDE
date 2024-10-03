package net.backupcup.mcde.block;

import net.backupcup.mcde.MCDE;
import net.backupcup.mcde.block.custom.GildingFoundryBlock;
import net.backupcup.mcde.block.custom.RollBenchBlock;
import net.backupcup.mcde.block.custom.RunicTableBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

public class ModBlocks {

    public static final Block RUNIC_TABLE = registerBlock("runic_table",
            new RunicTableBlock(AbstractBlock.Settings.create().solid().strength(2f).nonOpaque()), ItemGroups.FUNCTIONAL);

    public static final Block ROLL_BENCH = registerBlock("roll_bench",
            new RollBenchBlock(AbstractBlock.Settings.create().solid().strength(2f).nonOpaque()), ItemGroups.FUNCTIONAL);

    public static final Block GILDING_FOUNDRY = registerBlock("gilding_foundry",
            new GildingFoundryBlock(AbstractBlock.Settings.create().solid().strength(2f).nonOpaque()), ItemGroups.FUNCTIONAL);

    public static Block registerBlock(String name, Block block, RegistryKey<ItemGroup> tab) {
        registerBlockItem(name, block, tab);
        return Registry.register(Registries.BLOCK, MCDE.id(name), block);
    }

    private static Item registerBlockItem(String name, Block block, RegistryKey<ItemGroup> tab) {
        var item = Registry.register(Registries.ITEM, MCDE.id(name),
                new BlockItem(block, new Item.Settings()));
        ItemGroupEvents.modifyEntriesEvent(tab).register(entries -> entries.add(item));

        return item;
    }
}
