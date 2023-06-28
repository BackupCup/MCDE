package net.backupcup.mcd_enchantments.item;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {

    public static final Item TEST_RUNE = registerItem("test_rune",
            new Item(new FabricItemSettings().maxCount(1).group(ModItemGroup.MCDEncahntments)));


    public static void RegisterModItems() {
        MCDEnchantments.LOGGER.debug("Registering Mod Items");
    }

    public static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new Identifier(MCDEnchantments.MOD_ID, name), item);
    }
}