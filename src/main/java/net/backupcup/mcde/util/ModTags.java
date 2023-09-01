package net.backupcup.mcde.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModTags {
    public static class Items {
        public static final TagKey<Item> WEAPONS = TagKey.of(Registry.ITEM_KEY, new Identifier("c", "weapon_enchantments_allowed"));
    }

    public static class Enchantments {
        public static final TagKey<Enchantment> POWERFUL = TagKey.of(Registry.ENCHANTMENT_KEY, new Identifier("c", "powerful"));
    }

    public static boolean isIn(Enchantment enchantment, TagKey<Enchantment> tag) {
        return isIn(enchantment, tag, Registry.ENCHANTMENT);
    }

    public static boolean isIn(Identifier enchantmentId, TagKey<Enchantment> tag) {
        return isIn(enchantmentId, tag, Registry.ENCHANTMENT);
    }

    public static <T> boolean isIn(T obj, TagKey<T> tag, Registry<T> registry) {
        var key = registry.getKey(obj);
        if (key.isEmpty()) {
            return false;
        }
        var entry = registry.getEntry(key.get());
        if (entry.isEmpty()) {
            return false;
        }
        return entry.get().isIn(tag);
    }

    public static <T> boolean isIn(Identifier id, TagKey<T> tag, Registry<T> registry) {
        return isIn(registry.get(id), tag, registry);
    }
}
