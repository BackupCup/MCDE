package net.backupcup.mcde.util;

import static net.backupcup.mcde.util.Slots.FIRST;
import static net.backupcup.mcde.util.Slots.SECOND;
import static net.backupcup.mcde.util.Slots.THIRD;
import static net.minecraft.util.registry.Registry.ENCHANTMENT;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.backupcup.mcde.MCDEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;

public class EnchantmentUtils {
    public static Stream<Identifier> getEnchantmentStream() {
        return ENCHANTMENT.getIds().stream();
    }

    public static Stream<Identifier> getEnchantmentsForItem(ItemStack itemStack) {
        var existing = EnchantmentHelper.get(itemStack).keySet().stream()
            .map(e -> ENCHANTMENT.getId(e))
            .collect(Collectors.toSet());
        return getAllEnchantmentsForItem(itemStack)
            .filter(id -> !existing.contains(id));
    }

    public static Stream<Identifier> getAllEnchantmentsForItem(ItemStack itemStack) {
        Predicate<Enchantment> target = itemStack.isIn(ModTags.Items.WEAPONS) ?
            e -> e.type.equals(EnchantmentTarget.WEAPON) :
            e -> e.isAcceptableItem(itemStack);

        return ENCHANTMENT.stream()
            .filter(MCDEnchantments.getConfig()::isEnchantmentAllowed)
            .filter(e -> e.isAvailableForRandomSelection() || !MCDEnchantments.getConfig().isAvailabilityForRandomSelectionRespected())
            .filter(e -> !e.isTreasure() || MCDEnchantments.getConfig().isTreasureAllowed())
            .filter(e -> !e.isCursed() || MCDEnchantments.getConfig().areCursedAllowed())
            .filter(target)
            .map(ENCHANTMENT::getId);
    }

    public static List<EnchantmentTarget> getEnchantmentTargets(Item item) {
        return Arrays.stream(EnchantmentTarget.values())
            .filter(target -> target.isAcceptableItem(item)).toList();
    }

    public static Identifier getEnchantmentId(Enchantment enchantment) {
        return ENCHANTMENT.getId(enchantment);
    }

    public static EnchantmentSlots generateEnchantments(ItemStack itemStack) {
        return generateEnchantments(itemStack, new LocalRandom(System.nanoTime()));
    }

    public static boolean isCompatible(Collection<Identifier> present, Identifier enchantment) {
        return present.stream().allMatch(id -> ENCHANTMENT.get(enchantment).canCombine(ENCHANTMENT.get(id)));
    }

    public static void removeIncompatible(List<Identifier> pool, EnchantmentSlots.Builder builder) {
        var it = pool.iterator();
        while (it.hasNext()) {
            var enchantment = it.next();
            if (!isCompatible(builder.getAdded().stream().map(c -> c.getEnchantmentId()).toList(), enchantment)) {
                it.remove();
            }
        }
    }

    public static EnchantmentSlots generateEnchantments(ItemStack itemStack, Random random) {
        var builder = EnchantmentSlots.builder();
        var pool = getEnchantmentsForItem(itemStack).collect(ObjectArrayList.toList());
        boolean isTwoChoiceGenerated = false;
        boolean isSecondSlotGenerated = false;
        float threeChoiceChance = 0.5f;
        float secondSlotChance = 0.5f;
        float thirdSlotChance = 0.25f;

        if (pool.isEmpty()) {
            return EnchantmentSlots.EMPTY;
        }

        Util.shuffle(pool, random);

        if (random.nextFloat() < threeChoiceChance && pool.size() >= 3) {
            builder.withSlot(FIRST, pool.pop(), pool.pop(), pool.pop());
        }
        else if (pool.size() >= 2) {
            builder.withSlot(FIRST, pool.pop(), pool.pop());
            isTwoChoiceGenerated = true;
        }
        else {
            builder.withSlot(FIRST, pool.pop());
        }

        if (MCDEnchantments.getConfig().isCompatibilityRequired()) {
            removeIncompatible(pool, builder);
        }

        if (pool.isEmpty()) {
            return builder.build();
        }

        if (random.nextFloat() < secondSlotChance) {
            if (!isTwoChoiceGenerated && random.nextFloat() < threeChoiceChance && pool.size() >= 3) {
                builder.withSlot(SECOND, pool.pop(), pool.pop(), pool.pop());
            }
            else if (pool.size() >= 2) {
                builder.withSlot(SECOND, pool.pop(), pool.pop());
                isTwoChoiceGenerated = true;
            }
            else {
                builder.withSlot(SECOND, pool.pop());
            }
            isSecondSlotGenerated = true;
        }

        if (MCDEnchantments.getConfig().isCompatibilityRequired()) {
            removeIncompatible(pool, builder);
        }

        if (pool.isEmpty()) {
            return builder.build();
        }

        if (isSecondSlotGenerated && random.nextFloat() < thirdSlotChance) {
            if (!isTwoChoiceGenerated && random.nextFloat() < threeChoiceChance && pool.size() >= 3) {
                builder.withSlot(THIRD, pool.pop(), pool.pop(), pool.pop());
            }
            else if (pool.size() >= 2) {
                builder.withSlot(THIRD, pool.pop(), pool.pop());
                isTwoChoiceGenerated = true;
            }
            else {
                builder.withSlot(THIRD, pool.pop());
            }
        }

        return builder.build();
    }

    public static boolean canGenerateEnchantment(ItemStack itemStack) {
        return !getPossibleCandidates(itemStack).isEmpty();
    }

    public static Optional<Identifier> generateEnchantment(ItemStack itemStack) {
        return generateEnchantment(itemStack, new LocalRandom(System.nanoTime()), getPossibleCandidates(itemStack));
    }

    public static Optional<Identifier> generateEnchantment(ItemStack itemStack, List<Identifier> candidates) {
        return generateEnchantment(itemStack, new LocalRandom(System.nanoTime()), candidates);
    }

    public static Optional<Identifier> generateEnchantment(ItemStack itemStack, Random random, List<Identifier> candidates) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    public static Set<Identifier> getAllEnchantmentsInItem(ItemStack itemStack) {
        var present = EnchantmentHelper.get(itemStack).keySet().stream()
            .map(key -> ENCHANTMENT.getId(key))
            .collect(Collectors.toSet());
        var slots = EnchantmentSlots.fromItemStack(itemStack);
        if (slots == null) {
            return present;
        }
        slots.stream()
            .flatMap(s -> s.choices().stream())
            .map(c -> c.getEnchantmentId()).forEach(present::add);
        return present;
    }

    public static Stream<Identifier> getEnchantmentsNotInItem(ItemStack itemStack) {
        var present = getAllEnchantmentsInItem(itemStack);
        var candidates = getAllEnchantmentsForItem(itemStack)
            .filter(id -> !present.contains(id));
        return candidates;
    }

    private static List<Identifier> getPossibleCandidates(ItemStack itemStack) {
        var present = getAllEnchantmentsInItem(itemStack);
        var candidates = getAllEnchantmentsForItem(itemStack)
            .filter(id -> !present.contains(id));
        if (MCDEnchantments.getConfig().isCompatibilityRequired()) {
            candidates = candidates.filter(id -> isCompatible(present, id));
        }
         return candidates.toList();
    }
}
