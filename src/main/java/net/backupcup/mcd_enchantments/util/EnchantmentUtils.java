package net.backupcup.mcd_enchantments.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.backupcup.mcd_enchantments.MCDEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.backupcup.mcd_enchantments.util.Slots.*;
import static net.minecraft.enchantment.Enchantments.*;
import static net.minecraft.util.registry.Registry.ENCHANTMENT;

public class EnchantmentUtils {
    private static Predicate<String> namespaceMatcher = Pattern.compile("minecraft|mcd[aw]|enchantmentsplus").asPredicate(); //ADD "|qu-enchantments" TO IT WHEN THE BUG IS FIXED

    public static Stream<Identifier> getEnchantmentStream() {
        return ENCHANTMENT.getIds().stream()
            .filter(id -> namespaceMatcher.test(id.getNamespace()));
    }

    public static Stream<Identifier> getEnchantmentsForItem(ItemStack itemStack) {
        var existing = itemStack.getEnchantments().stream()
            .map(nbt -> Identifier.tryParse(((NbtCompound)nbt).getString("id")))
            .collect(Collectors.toSet());
        return ENCHANTMENT.getIds().stream()
            .filter(id -> namespaceMatcher.test(id.getNamespace()) &&
                    !existing.contains(id) &&
                    (itemStack.isIn(ModTags.Items.WEAPONS) &&
                     ENCHANTMENT.get(id).type.equals(EnchantmentTarget.WEAPON) ||
                     ENCHANTMENT.get(id).type.isAcceptableItem(itemStack.getItem())) &&
                    !(ENCHANTMENT.get(id).isCursed() ||
                        ENCHANTMENT.getId(MENDING).equals(id) ||
                        ENCHANTMENT.getId(UNBREAKING).equals(id) ||
                        EnchantmentClassifier.bannedEnchantments.contains(id)));
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

    public static EnchantmentSlots generateEnchantments(ItemStack itemStack) {
        return generateEnchantments(itemStack, new LocalRandom(System.nanoTime()));
    }

    public static EnchantmentSlots generateEnchantments(ItemStack itemStack, Random random) {
        var item = itemStack.getItem();
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
        else if (itemStack.isIn(ModTags.Items.WEAPONS) ||
                EnchantmentTarget.WEAPON.isAcceptableItem(item) ||
                EnchantmentTarget.BOW.isAcceptableItem(item) || EnchantmentTarget.CROSSBOW.isAcceptableItem(item) ||
                EnchantmentTarget.ARMOR_FEET.isAcceptableItem(item) || EnchantmentTarget.ARMOR_LEGS.isAcceptableItem(item) ||
                EnchantmentTarget.ARMOR_CHEST.isAcceptableItem(item) || EnchantmentTarget.ARMOR_HEAD.isAcceptableItem(item)) {
            var builder = EnchantmentSlots.builder();
            var enchantmentList = getEnchantmentsForItem(itemStack).collect(ObjectArrayList.toList());
            Util.shuffle(enchantmentList, random);
            var enchantments = new ArrayDeque<>(enchantmentList);
            boolean isTwoChoiceGenerated = false;
            boolean isSecondSlotGenerated = false;
            float threeChoiceChance = 0.5f;
            float secondSlotChance = 0.5f;
            float thirdSlotChance = 0.25f;

            if (enchantmentList.isEmpty()) {
                return EnchantmentSlots.EMPTY;
            }

            if (random.nextFloat() < threeChoiceChance && enchantments.size() >= 3) {
                builder.withSlot(FIRST, enchantments.pop(), enchantments.pop(), enchantments.pop());
            }
            else if (enchantments.size() >= 2) {
                builder.withSlot(FIRST, enchantments.pop(), enchantments.pop());
                isTwoChoiceGenerated = true;
            }
            else {
                builder.withSlot(FIRST, enchantmentList.pop());
            }

            if (enchantments.isEmpty()) {
                return builder.build();
            }

            if (random.nextFloat() < secondSlotChance) {
                if (!isTwoChoiceGenerated && random.nextFloat() < threeChoiceChance && enchantments.size() >= 3) {
                    builder.withSlot(SECOND, enchantments.pop(), enchantments.pop(), enchantments.pop());
                }
                else if (enchantments.size() >= 2) {
                    builder.withSlot(SECOND, enchantments.pop(), enchantments.pop());
                    isTwoChoiceGenerated = true;
                }
                else {
                    builder.withSlot(SECOND, enchantments.pop());
                }
                isSecondSlotGenerated = true;
            }

            if (enchantments.isEmpty()) {
                return builder.build();
            }

            if (isSecondSlotGenerated && random.nextFloat() < thirdSlotChance) {
                if (!isTwoChoiceGenerated && random.nextFloat() < threeChoiceChance && enchantments.size() >= 3) {
                    builder.withSlot(THIRD, enchantments.pop(), enchantments.pop(), enchantments.pop());
                }
                else if (enchantments.size() >= 2) {
                    builder.withSlot(THIRD, enchantments.pop(), enchantments.pop());
                    isTwoChoiceGenerated = true;
                }
                else {
                    builder.withSlot(THIRD, enchantments.pop());
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

    public static Optional<Identifier> generateEnchantment(ItemStack itemStack) {
        return generateEnchantment(itemStack, new LocalRandom(System.nanoTime()));
    }

    public static Optional<Identifier> generateEnchantment(ItemStack itemStack, Random random) {
        var slots = EnchantmentSlots.fromItemStack(itemStack);
        var gilded = itemStack.getNbt().contains("Gilding") ?
            Optional.of(Identifier.tryParse(itemStack.getNbt().getString("Gilding"))) :
            Optional.empty();
        var present = slots.stream()
            .flatMap(slot -> slot.choices().stream())
            .map(choice -> choice.getEnchantment())
            .collect(Collectors.toSet());
        var newEnchantments = getEnchantmentsForItem(itemStack)
            .filter(id -> !present.contains(id) || gilded.isPresent() && gilded.get().equals(id)).toList();
        if (newEnchantments.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(newEnchantments.get(random.nextInt(newEnchantments.size())));
    }
}
