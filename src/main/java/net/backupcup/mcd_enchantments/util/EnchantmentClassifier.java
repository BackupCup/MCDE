package net.backupcup.mcd_enchantments.util;

import net.minecraft.util.Identifier;

import java.util.List;
import java.util.stream.Stream;

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
        "enchantmentsplus:thunderlord",
        "enchantmentsplus:lifesteal",
        "enchantmentsplus:blazewalker",
        "enchantmentsplus:flashforge",
        "enchantmentsplus:dualleap",
        "enchantmentsplus:hiker",
        "minecraft:protection",
        "minecraft:sharpness",
        "minecraft:sweeping",
        "minecraft:riptide",
        "minecraft:channeling",
        "minecraft:infinity",
        "minecraft:fortune",
        "minecraft:silk_touch",
        "minecraft:multishot").map(Identifier::tryParse).toList();

    public static List<Identifier> bannedEnchantments = Stream.of(
        "qu-enchantments:nightblood",
           "qu-enchantments:omen_of_immunity",
           "qu-enchantments:shaped_glass",
           "qu-enchantments:skywalker",
           "qu-enchantments:essence_of_ender",
           "qu-enchantments:strip_miner",
           "qu-enchantments:fidelity",
           "qu-enchantments:aggression_blessing",
           "qu-enchantments:bashing",
           "qu-enchantments:regeneration_blessing",
           "qu-enchantments:speed_blessing",
           "qu-enchantments:reflection"
    ).map(Identifier::tryParse).toList();

    public static boolean isEnchantmentPowerful(Identifier enchantmentId) {
        return powerfulEnchantments.contains(enchantmentId);
    }
}
