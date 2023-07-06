package net.backupcup.mcd_enchantments.util;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnchantmentUtils {
    private static Predicate<String> namespaceMatcher = Pattern.compile("minecraft|mcd[aw]").asPredicate();

    public static Stream<Identifier> getEnchantmentStream() {
        return Registry.ENCHANTMENT.getIds().stream()
            .filter(id -> namespaceMatcher.test(id.getNamespace()));
    }

    public static List<Identifier> getEnchantmentsForItem(Item item) {
        return getEnchantmentStream()
            .filter(id -> Registry.ENCHANTMENT.get(id).type.isAcceptableItem(item) &&
                    !(Registry.ENCHANTMENT.getId(Enchantments.VANISHING_CURSE).equals(id) ||
                      Registry.ENCHANTMENT.getId(Enchantments.BINDING_CURSE).equals(id) ||
                      Registry.ENCHANTMENT.getId(Enchantments.MENDING).equals(id) ||
                      Registry.ENCHANTMENT.getId(Enchantments.UNBREAKING).equals(id)))
            .collect(Collectors.toList());
    }

    public static List<EnchantmentTarget> getEnchantmentTargets(Item item) {
        return Arrays.stream(EnchantmentTarget.values())
            .filter(target -> target.isAcceptableItem(item)).toList();
    }

    public static Identifier getEnchantmentId(Enchantment enchantment) {
        return Registry.ENCHANTMENT.getId(enchantment);
    }

    public static EnchantmentSlots getEnchantments(Item item) {
        if (EnchantmentTarget.DIGGER.isAcceptableItem(item)) {
            return EnchantmentSlots.builder()
                .withSlot(getEnchantmentId(Enchantments.EFFICIENCY))
                .withSlot(
                    getEnchantmentId(Enchantments.FORTUNE),
                    getEnchantmentId(Enchantments.SILK_TOUCH)
                )
                .build();
        }
        else if (EnchantmentTarget.FISHING_ROD.isAcceptableItem(item)) {
            return EnchantmentSlots.builder()
                .withSlot(getEnchantmentId(Enchantments.LURE))
                .withSlot(getEnchantmentId(Enchantments.LUCK_OF_THE_SEA))
                .build();
        }
        else if (EnchantmentTarget.TRIDENT.isAcceptableItem(item)) {
            return EnchantmentSlots.builder()
                .withSlot(
                    getEnchantmentId(Enchantments.IMPALING),
                    getEnchantmentId(Enchantments.LOYALTY)
                )
                .withSlot(
                    getEnchantmentId(Enchantments.RIPTIDE),
                    getEnchantmentId(Enchantments.CHANNELING)
                )
                .build();
        }
        else if (EnchantmentTarget.WEAPON.isAcceptableItem(item) ||
                 EnchantmentTarget.BOW.isAcceptableItem(item) || EnchantmentTarget.CROSSBOW.isAcceptableItem(item) ||
                 EnchantmentTarget.ARMOR_FEET.isAcceptableItem(item) || EnchantmentTarget.ARMOR_LEGS.isAcceptableItem(item) ||
                 EnchantmentTarget.ARMOR_CHEST.isAcceptableItem(item) || EnchantmentTarget.ARMOR_HEAD.isAcceptableItem(item)) {
            Random random = new Random(System.nanoTime());
            List<Identifier> enchantments = getEnchantmentsForItem(item);
            Collections.shuffle(enchantments, random);
            Iterator<Identifier> it = enchantments.iterator();
            EnchantmentSlots.Builder builder = EnchantmentSlots.builder();
            boolean isTwoChoiceGenerated = false;
            boolean isSecondSlotGenerated = false;
            float threeChoiceChance = 0.5f;
            float secondSlotChance = 0.5f;
            float thirdSlotChance = 0.25f;

            if (random.nextFloat() < threeChoiceChance) {
                builder.withSlot(it.next(), it.next(), it.next());
            }
            else {
                builder.withSlot(it.next(), it.next());
                isTwoChoiceGenerated = true;
            }

            if (random.nextFloat() < secondSlotChance) {
                if (!isTwoChoiceGenerated && random.nextFloat() < threeChoiceChance) {
                    builder.withSlot(it.next(), it.next(), it.next());
                }
                else {
                    builder.withSlot(it.next(), it.next());
                    isTwoChoiceGenerated = true;
                }
                isSecondSlotGenerated = true;
            }

            if (isSecondSlotGenerated && random.nextFloat() < thirdSlotChance) {
                if (!isTwoChoiceGenerated && random.nextFloat() < threeChoiceChance) {
                    builder.withSlot(it.next(), it.next(), it.next());
                }
                else {
                    builder.withSlot(it.next(), it.next());
                    isTwoChoiceGenerated = true;
                }
            }

            return builder.build();
        }
        MCDEnchantments.LOGGER.warn("Empty slots generated");
        return EnchantmentSlots.EMPTY;
    }
}
