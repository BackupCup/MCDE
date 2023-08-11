package net.backupcup.mcde.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTags {
    public static class Items {
        public static final TagKey<Item> WEAPONS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "weapons"));
    }

    public static class Enchantments {
        public static final TagKey<Enchantment> POWERFUL = TagKey.of(RegistryKeys.ENCHANTMENT, new Identifier("c", "powerful"));
    }

    public static boolean isIn(Enchantment enchantment, TagKey<Enchantment> tag) {
        var key = Registries.ENCHANTMENT.getKey(enchantment);
        if (key.isEmpty()) {
            return false;
        }
        var entry = Registries.ENCHANTMENT.getEntry(key.get());
        if (entry.isEmpty()) {
            return false;
        }
        return entry.get().isIn(tag);
    }

    public static boolean isIn(Identifier enchantmentId, TagKey<Enchantment> tag) {
        return isIn(Registries.ENCHANTMENT.get(enchantmentId), tag);
    }
}
