package net.backupcup.mcde.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModTags {
    public static class Items {
        public static final TagKey<Item> WEAPONS = TagKey.of(Registry.ITEM_KEY, new Identifier("c", "weapons"));
    }

    public static class Enchantments {
        public static final TagKey<Enchantment> POWERFUL = TagKey.of(Registry.ENCHANTMENT_KEY, new Identifier("c", "powerful"));
    }

    public static boolean isIn(Enchantment enchantment, TagKey<Enchantment> tag) {
        var key = Registry.ENCHANTMENT.getKey(enchantment);
        if (key.isEmpty()) {
            return false;
        }
        var entry = Registry.ENCHANTMENT.getEntry(key.get());
        if (entry.isEmpty()) {
            return false;
        }
        return entry.get().isIn(tag);
    }

    public static boolean isIn(Identifier enchantmentId, TagKey<Enchantment> tag) {
        return isIn(Registry.ENCHANTMENT.get(enchantmentId), tag);
    }
}
