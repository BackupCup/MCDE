package net.backupcup.mcde.util;

import static net.minecraft.registry.Registries.ENCHANTMENT;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            e -> e.target.equals(EnchantmentTarget.WEAPON) || e.isAcceptableItem(itemStack) :
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

    public static Enchantment getEnchantment(Identifier enchantmentId) {
        return ENCHANTMENT.get(enchantmentId);
    }

    public static Identifier getEnchantmentId(Enchantment enchantment) {
        return ENCHANTMENT.getId(enchantment);
    }

    public static Formatting formatEnchantment(Identifier id) {
        return MCDEnchantments.getConfig().isEnchantmentPowerful(id) ? Formatting.RED : Formatting.LIGHT_PURPLE;
    }

    public static boolean isCompatible(Collection<Identifier> present, Identifier enchantment) {
        return present.stream().allMatch(id -> ENCHANTMENT.get(enchantment).canCombine(ENCHANTMENT.get(id)));
    }

    public static boolean isCompatible(Identifier present, Identifier enchantment) {
        return ENCHANTMENT.get(enchantment).canCombine(ENCHANTMENT.get(present));
    }


    public static ScreenHandlerListener generatorListener(ScreenHandlerContext context, PlayerEntity player) {
        return new ScreenHandlerListener() {
            @Override
            public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
            }

            @Override
            public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
                if (slotId != 0 || stack.isEmpty() || EnchantmentSlots.fromItemStack(stack).isPresent()) {
                    return;
                }
                context.run((world, pos) -> {
                    var server = world.getServer();
                    var serverPlayerEntity = Optional.ofNullable(server.getPlayerManager().getPlayer(player.getUuid()));
                    SlotsGenerator.forItemStack(stack)
                        .withOptionalOwner(serverPlayerEntity)
                        .build()
                        .generateEnchantments()
                        .updateItemStack(stack);
                    handler.setStackInSlot(0, 0, stack);
                });
            }
        };
    }

    public static Optional<Identifier> generateEnchantment(ItemStack itemStack, Optional<ServerPlayerEntity> optionalOwner) {
        return generateEnchantment(itemStack, optionalOwner, new LocalRandom(System.nanoTime()), getPossibleCandidates(itemStack));
    }

    public static Optional<Identifier> generateEnchantment(ItemStack itemStack, Optional<ServerPlayerEntity> optionalOwner, List<Identifier> candidates) {
        return generateEnchantment(itemStack, optionalOwner, new LocalRandom(System.nanoTime()), candidates);
    }

    public static Set<Identifier> getLockedEnchantments(ServerPlayerEntity player) {
        if (player.isCreative()) {
            return Set.of();
        }
        var advancements = player.server.getAdvancementLoader().getAdvancements();
        var tracker = player.getAdvancementTracker();
        var unlocks = MCDEnchantments.getConfig().getUnlocks().stream()
            .collect(Collectors.partitioningBy(u -> advancements.stream()
                .filter(u.getAdvancements()::contains)
                .allMatch(a -> tracker.getProgress(a).isDone()),
                Collectors.flatMapping(u -> getEnchantmentStream().filter(u.getEnchantments()::contains),
                    Collectors.toSet())));
        unlocks.get(false).removeIf(unlocks.get(true)::contains);
        return unlocks.get(false);
    }

    public static Optional<Identifier> generateEnchantment(ItemStack itemStack, Optional<ServerPlayerEntity> optionalOwner, Random random, List<Identifier> candidates) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        optionalOwner.ifPresent(player -> candidates.removeIf(getLockedEnchantments(player)::contains));
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    public static Set<Identifier> getAllEnchantmentsInItem(ItemStack itemStack) {
        var present = EnchantmentHelper.get(itemStack).keySet().stream()
            .map(key -> ENCHANTMENT.getId(key))
            .collect(Collectors.toSet());
        var slotsOptional = EnchantmentSlots.fromItemStack(itemStack);
        if (slotsOptional.isEmpty()) {
            return present;
        }
        var slots = slotsOptional.get();
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

    public static boolean isGilding(Enchantment enchantment, ItemStack itemStack) {
        return EnchantmentSlots.fromItemStack(itemStack)
                .flatMap(slots -> slots.getGilding().map(EnchantmentUtils::getEnchantment))
                .filter(gilding -> enchantment.equals(gilding)).isPresent();
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
