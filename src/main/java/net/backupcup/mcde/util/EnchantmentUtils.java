package net.backupcup.mcde.util;

import static net.backupcup.mcde.util.Slots.FIRST;
import static net.backupcup.mcde.util.Slots.SECOND;
import static net.backupcup.mcde.util.Slots.THIRD;
import static net.minecraft.util.registry.Registry.ENCHANTMENT;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.backupcup.mcde.MCDEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
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

    public static Formatting formatEnchantment(Identifier id) {
        return MCDEnchantments.getConfig().isEnchantmentPowerful(id) ? Formatting.RED : Formatting.LIGHT_PURPLE;
    }

    public static EnchantmentSlots generateEnchantments(ItemStack itemStack, ServerPlayerEntity player) {
        return generateEnchantments(itemStack, player, new LocalRandom(System.nanoTime()));
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

    public static Map<Slots, Float> calculateAdvancementModifiers(ServerPlayerEntity player) {
        var result = new TreeMap<>(Map.of(Slots.SECOND, 0f, Slots.THIRD, 0f));
        MCDEnchantments.getConfig().getProgressChances().entrySet().stream()
            .filter(kvp -> player.getAdvancementTracker().getProgress(player.server.getAdvancementLoader().get(kvp.getKey())).isDone())
            .map(Map.Entry::getValue)
            .reduce(result, (lhs, rhs) -> {
                for (var kvp : rhs.entrySet()) {
                    lhs.put(kvp.getKey(), lhs.get(kvp.getKey()) + kvp.getValue());
                }
                return lhs;
            });
        return result;
    }

    public static float calculateEnchantabilityModifier(float baseChance, int enchantability) {
        // Some magic math here. The idea is to make low enchantability add less chance than high one
        // the significance function really can be any other one
        // one requirement for such function is its values should be from 0 to 1
        float significance = (float)(1 / Math.PI * Math.atan(0.2 * (enchantability - 20)) + 0.5);
        return -significance * baseChance + significance;
    }

    public static EnchantmentSlots generateEnchantments(ItemStack itemStack, ServerPlayerEntity player, Random random) {
        var enchantability = itemStack.getItem().getEnchantability();
        var builder = EnchantmentSlots.builder();
        var pool = getEnchantmentsForItem(itemStack).collect(ObjectArrayList.toList());
        boolean isTwoChoiceGenerated = false;
        boolean isSecondSlotGenerated = false;
        float threeChoiceChance = 0.5f + enchantability / 100f;
        var advancementModifier = calculateAdvancementModifiers(player);
        float secondSlotChance = 0.5f + advancementModifier.get(SECOND);
        float thirdSlotChance = 0.25f + advancementModifier.get(THIRD);
        secondSlotChance += calculateEnchantabilityModifier(secondSlotChance, enchantability);
        thirdSlotChance += calculateEnchantabilityModifier(thirdSlotChance, enchantability);

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

    public static ScreenHandlerListener generatorListener(ScreenHandlerContext context, PlayerEntity player) {
        return new ScreenHandlerListener() {
            @Override
            public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
            }

            @Override
            public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
                if (slotId != 0 || stack.isEmpty() || EnchantmentSlots.fromItemStack(stack) != null) {
                    return;
                }
                context.run((world, pos) -> {
                    var server = world.getServer();
                    var serverPlayer = server.getPlayerManager().getPlayer(player.getUuid());
                    EnchantmentUtils.generateEnchantments(stack, serverPlayer).updateItemStack(stack);
                    handler.setStackInSlot(0, 0, stack);
                });
            }
        };
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
