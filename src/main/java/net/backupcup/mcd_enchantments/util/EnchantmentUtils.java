package net.backupcup.mcd_enchantments.util;

import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.backupcup.mcd_enchantments.util.Slots.*;
import static net.minecraft.enchantment.Enchantments.*;
import static net.minecraft.util.registry.Registry.ENCHANTMENT;

public class EnchantmentUtils {
    private static Predicate<String> namespaceMatcher = Pattern.compile("minecraft|mcd[aw]").asPredicate();

    public static Stream<Identifier> getEnchantmentStream() {
        return ENCHANTMENT.getIds().stream()
            .filter(id -> namespaceMatcher.test(id.getNamespace()));
    }

    public static List<Identifier> getEnchantmentsForItem(ItemStack itemStack) {
        var existing = itemStack.getEnchantments().stream()
            .map(nbt -> Identifier.tryParse(((NbtCompound)nbt).getString("id")))
            .collect(Collectors.toSet());
        return ENCHANTMENT.getIds().stream()
            .filter(id -> namespaceMatcher.test(id.getNamespace()) &&
                    !existing.contains(id) &&
                    ENCHANTMENT.get(id).type.isAcceptableItem(itemStack.getItem()) &&
                    !(ENCHANTMENT.get(id).isCursed() ||
                        ENCHANTMENT.getId(MENDING).equals(id) ||
                        ENCHANTMENT.getId(UNBREAKING).equals(id)))
            .collect(Collectors.toList());
    }

    public static List<EnchantmentTarget> getEnchantmentTargets(Item item) {
        return Arrays.stream(EnchantmentTarget.values())
            .filter(target -> target.isAcceptableItem(item)).toList();
    }

    public static int getCost(Identifier enchantmentId, short level) {
        return (EnchantmentClassifier.isEnchantmentPowerful(enchantmentId) ? 5 : 3) * level;
    }

    public static Identifier getEnchantmentId(Enchantment enchantment) {
        return ENCHANTMENT.getId(enchantment);
    }

    public static EnchantmentSlots getEnchantments(ItemStack itemStack) {
        var item = itemStack.getItem();
        // itemStack.isIn(ModTags.Items.WEAPONS) ||
        if (EnchantmentTarget.TRIDENT.isAcceptableItem(item)) {
            return EnchantmentSlots.builder()
                    .withSlot(FIRST,
                            getEnchantmentId(IMPALING),
                            getEnchantmentId(LOYALTY)
                    )
                    .withSlot(SECOND,
                            getEnchantmentId(RIPTIDE),
                            getEnchantmentId(CHANNELING)
                    )
                    .build();
        }
        else if (EnchantmentTarget.WEAPON.isAcceptableItem(item) ||
                EnchantmentTarget.BOW.isAcceptableItem(item) || EnchantmentTarget.CROSSBOW.isAcceptableItem(item) ||
                EnchantmentTarget.ARMOR_FEET.isAcceptableItem(item) || EnchantmentTarget.ARMOR_LEGS.isAcceptableItem(item) ||
                EnchantmentTarget.ARMOR_CHEST.isAcceptableItem(item) || EnchantmentTarget.ARMOR_HEAD.isAcceptableItem(item)) {
            Random random = new Random(System.nanoTime());
            List<Identifier> enchantments = getEnchantmentsForItem(itemStack);
            Collections.shuffle(enchantments, random);
            Iterator<Identifier> it = enchantments.iterator();
            EnchantmentSlots.Builder builder = EnchantmentSlots.builder();
            boolean isTwoChoiceGenerated = false;
            boolean isSecondSlotGenerated = false;
            float threeChoiceChance = 0.5f;
            float secondSlotChance = 0.5f;
            float thirdSlotChance = 0.25f;

            if (random.nextFloat() < threeChoiceChance) {
                builder.withSlot(FIRST, it.next(), it.next(), it.next());
            }
            else {
                builder.withSlot(FIRST, it.next(), it.next());
                isTwoChoiceGenerated = true;
            }

            if (random.nextFloat() < secondSlotChance) {
                if (!isTwoChoiceGenerated && random.nextFloat() < threeChoiceChance) {
                    builder.withSlot(SECOND, it.next(), it.next(), it.next());
                }
                else {
                    builder.withSlot(SECOND, it.next(), it.next());
                    isTwoChoiceGenerated = true;
                }
                isSecondSlotGenerated = true;
            }

            if (isSecondSlotGenerated && random.nextFloat() < thirdSlotChance) {
                if (!isTwoChoiceGenerated && random.nextFloat() < threeChoiceChance) {
                    builder.withSlot(THIRD, it.next(), it.next(), it.next());
                }
                else {
                    builder.withSlot(THIRD, it.next(), it.next());
                    isTwoChoiceGenerated = true;
                }
            }

            return builder.build();
        }
        else if (EnchantmentTarget.DIGGER.isAcceptableItem(item)) {
            return EnchantmentSlots.builder()
                .withSlot(FIRST, getEnchantmentId(EFFICIENCY))
                .withSlot(SECOND,
                    getEnchantmentId(FORTUNE),
                    getEnchantmentId(SILK_TOUCH)
                )
                .build();
        }
        else if (EnchantmentTarget.FISHING_ROD.isAcceptableItem(item)) {
            return EnchantmentSlots.builder()
                .withSlot(FIRST, getEnchantmentId(LURE))
                .withSlot(SECOND, getEnchantmentId(LUCK_OF_THE_SEA))
                .build();
        }
        else
        MCDEnchantments.LOGGER.warn("Empty slots generated");
        return EnchantmentSlots.EMPTY;
    }
}
