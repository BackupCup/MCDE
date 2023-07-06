package net.backupcup.mcd_enchantments.util;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.util.Identifier;

public class EnchantmentClassifier {
    private static List<Identifier> powerfulEnchantments = Stream.of(
        "mcdw:critical_hit",
        "mcdw:exploding",
        "mcdw:gravity",
        "mcdw:radiance",
        "mcdw:refreshment",
        "mcdw:shockwave",
        "mcdw:swirling",
        "mcdw:void_strike",
        "mcdw:chain_reaction",
        "mcdw:levitation_shot",
        "mcdw:overcharge",
        "mcdw:tempo_theft",
        "mcdw:void_shot",
        "mcdw:shared_pain",
        "mcda:chilling",
        "mcda:death_barter",
        "mcda:fire_focus",
        "mcda:poison_focus",
        "minecraft:protection",
        "minecraft:sharpness",
        "minecraft:sweeping",
        "minecraft:riptide",
        "minecraft:channeling",
        "minecraft:infinity",
        "minecraft:fortune",
        "minecraft:silk_touch",
        "minecraft:multishot").map(Identifier::tryParse).toList();

    public static boolean isEnchantmentPowerful(Identifier enchantmentId) {
        return powerfulEnchantments.contains(enchantmentId);
    }
}
