package net.backupcup.mcd_enchantments.screen;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.backupcup.mcd_enchantments.util.EnchantmentUtils;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class EnchantmentTextureMapper {
    private static Map<Identifier, Integer> textureMap = null;
    private static Map<Identifier, Integer> moddedMap = Map.of(
        Identifier.of("mcda", "ricochet"), 39
    );

    private static Map<Identifier, Integer> getTextureMap() {
        if (textureMap == null) {
            textureMap = EnchantmentUtils.getEnchantmentStream()
                .collect(Collectors.toMap(Function.identity(), id -> {
                    if (id.getNamespace().equals("minecraft")) {
                        return Registry.ENCHANTMENT.getRawId(Registry.ENCHANTMENT.get(id));
                    }
                    else {
                        return moddedMap.get(id);
                    }
                }));
        }
        return textureMap;
    }

    public static int getTextureId(Identifier enchantmentId) {
        return getTextureMap().get(enchantmentId);
    }
}
