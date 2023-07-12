package net.backupcup.mcd_enchantments.screen;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.backupcup.mcd_enchantments.util.EnchantmentUtils;
import net.minecraft.util.Identifier;

public class EnchantmentTextureMapper {
    private static Map<String, Map<String, Integer>> iconMap = null;
    private static Map<String, Identifier> textureMap = Map.of(
        "minecraft", Identifier.of(MCDEnchantments.MOD_ID, "textures/gui/enchantment_icons_vanilla.png"),
        "mcda", Identifier.of(MCDEnchantments.MOD_ID, "textures/gui/enchantment_icons_mcda.png"),
        "mcdw", Identifier.of(MCDEnchantments.MOD_ID, "textures/gui/enchantment_icons_mcdw.png")
    );

    private static Map<String, Map<String, Integer>> getIconMap() {
        if (iconMap == null) {
            Map<String, AtomicInteger> counters = List.of("minecraft", "mcda", "mcdw")
                .stream().collect(Collectors.toMap(Function.identity(), ns -> new AtomicInteger()));
            iconMap = EnchantmentUtils.getEnchantmentStream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.groupingBy(
                    Identifier::getNamespace,
                    Collectors.toMap(Identifier::getPath, id -> counters.get(id.getNamespace()).getAndIncrement())
                ));
            for (var e : iconMap.entrySet()) {
                MCDEnchantments.LOGGER.debug("Map of {} enchantments: ", e.getKey());
                for (var inner : e.getValue().entrySet().stream().sorted(Comparator.comparing(kvp -> kvp.getValue())).toList()) {
                    MCDEnchantments.LOGGER.debug("  {} -> {}", inner.getKey(), inner.getValue());
                }
            }
        }
        return iconMap;
    }

    public static record TexturePos(int x, int y) {
        public static TexturePos of(int x, int y) {
            return new TexturePos(x, y);
        }

        public TexturePos add(TexturePos other) {
            return new TexturePos(x() + other.x(), y() + other.y());
        }

        public TexturePos add(int x, int y) {
            return new TexturePos(x() + x, y() + y);
        }

    };

    public static TexturePos getPos(Identifier id) {
        int ordinal = getIconMap().get(id.getNamespace()).get(id.getPath());
        return new TexturePos(1 + 25 * (ordinal % 10), 1 + 25 * (ordinal / 10));
    }

    public static Identifier getTexture(Identifier id) {
        return textureMap.get(id.getNamespace());
    }
}
