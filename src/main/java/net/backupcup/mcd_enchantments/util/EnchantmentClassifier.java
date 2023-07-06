package net.backupcup.mcd_enchantments.util;

import java.util.Arrays;
import java.util.List;

public class EnchantmentClassifier {
    private List<String> powerfulEnchantments;

    public EnchantmentClassifier() {
        powerfulEnchantments = Arrays.asList("mcdw:critical_hit", "mcdw:exploding", "mcdw:gravity", "mcdw:radiance", "mcdw:refreshment", "mcdw:shockwave", "mcdw:swirling", "mcdw:void_strike",
                "mcdw:chain_reaction", "mcdw:levitation_shot", "mcdw:overcharge", "mcdw:tempo_theft", "mcdw:void_shot", "mcdw:shared_pain", "mcda:chilling", "mcda:death_barter", "mcda:fire_focus", "mcda:poison_focus", "minecraft:protection",
                "minecraft:sharpness", "minecraft:sweeping", "minecraft:riptide", "minecraft:channeling", "minecraft:infinity", "minecraft:fortune", "minecraft:silk_touch", "minecraft:multishot");
    }

    public boolean isEnchantmentPowerful(String enchantmentId) {
        return powerfulEnchantments.contains(enchantmentId);
    }
}
