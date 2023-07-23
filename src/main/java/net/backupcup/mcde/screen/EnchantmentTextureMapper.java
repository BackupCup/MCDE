package net.backupcup.mcde.screen;

import net.backupcup.mcde.MCDEnchantments;
import net.backupcup.mcde.util.EnchantmentUtils;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EnchantmentTextureMapper {
    private static Map<String, Map<String, Integer>> iconMap = null;

    private static Map<String, Map<String, Integer>> getIconMap() {
        if (iconMap == null) {
            Map<String, AtomicInteger> counters = new HashMap<>();
                iconMap = EnchantmentUtils.getEnchantmentStream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.groupingBy(
                    Identifier::getNamespace,
                    Collectors.toMap(Identifier::getPath, id -> {
                        var i = counters.getOrDefault(id.getNamespace(), new AtomicInteger());
                        counters.putIfAbsent(id.getNamespace(), i);
                        return i.getAndIncrement();
                    })));
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

    public static Identifier getTextureId(Identifier id) {
        return Identifier.of(MCDEnchantments.MOD_ID, String.format("textures/gui/icons/%s.png", id.getNamespace()));
    }
}
