package net.backupcup.mcde.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTags {
    public static class Items {
        public static final TagKey<Item> WEAPONS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "weapon_enchantments_allowed"));
    }

    public static class Enchantments {
        public static final TagKey<Enchantment> POWERFUL = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("c", "powerful"));
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
