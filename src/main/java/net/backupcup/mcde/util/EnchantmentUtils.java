package net.backupcup.mcde.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.backupcup.mcde.MCDE;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class EnchantmentUtils {
    public static Stream<Reference<Enchantment>> getEnchantmentStream(World world) {
        return world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).streamEntries();
    }

    public static Stream<Reference<Enchantment>> getEnchantmentsForItem(World world, ItemStack itemStack) {
        var existing = EnchantmentHelper.getEnchantments(itemStack).getEnchantments();
        return getAllEnchantmentsForItem(world, itemStack)
            .filter(entry -> !existing.contains(entry));
    }

    public static Stream<Reference<Enchantment>> getAllEnchantmentsForItem(World world, ItemStack itemStack) {
        Predicate<Reference<Enchantment>> target = itemStack.isIn(ModTags.Items.WEAPONS) ?
            e -> e.value().isAcceptableItem(Items.WOODEN_SWORD.getDefaultStack()) || e.value().isAcceptableItem(itemStack) :
            e -> e.value().isAcceptableItem(itemStack);

        return getEnchantmentStream(world)
            .filter(MCDE.getConfig()::isEnchantmentAllowed)
            .filter(e -> e.isIn(EnchantmentTags.IN_ENCHANTING_TABLE) || !MCDE.getConfig().isAvailabilityForRandomSelectionRespected())
            .filter(e -> e.isIn(EnchantmentTags.TREASURE) || MCDE.getConfig().isTreasureAllowed())
            .filter(e -> e.isIn(EnchantmentTags.CURSE) || MCDE.getConfig().areCursedAllowed())
            .filter(target);
    }

    public static Formatting formatEnchantment(Reference<Enchantment> enchantment) {
        return MCDE.getConfig().isEnchantmentPowerful(enchantment) ? Formatting.RED : Formatting.LIGHT_PURPLE;
    }

    public static boolean isCompatible(Collection<RegistryEntry<Enchantment>> present, RegistryEntry<Enchantment> enchantment) {
        return present.stream().allMatch(entry -> Enchantment.canBeCombined(entry, enchantment));
    }

    public static boolean isCompatible(RegistryEntry<Enchantment> present, RegistryEntry<Enchantment> enchantment) {
        return Enchantment.canBeCombined(present, enchantment);
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
                    var slots = SlotsGenerator.forItemStack(world, stack)
                        .withOptionalOwner(serverPlayerEntity)
                        .build()
                        .generateEnchantments()
                        .build();
                    stack.set(EnchantmentSlots.COMPONENT_TYPE, slots);
                    handler.setStackInSlot(0, 0, stack);
                });
            }
        };
    }

    public static Optional<Reference<Enchantment>> generateEnchantment(
        World world,
        ItemStack itemStack,
        Optional<ServerPlayerEntity> optionalOwner
    ) {
        return generateEnchantment(itemStack, optionalOwner, new LocalRandom(System.nanoTime()), getPossibleCandidates(world, itemStack));
    }

    public static <T extends RegistryEntry<Enchantment>> Optional<T> generateEnchantment(
        ItemStack itemStack,
        Optional<ServerPlayerEntity> optionalOwner,
        List<T> candidates
    ) {
        return generateEnchantment(itemStack, optionalOwner, new LocalRandom(System.nanoTime()), candidates);
    }

    public static Set<Reference<Enchantment>> getLockedEnchantments(ServerPlayerEntity player) {
        if (player.isCreative()) {
            return Set.of();
        }
        var world = player.getWorld();
        var advancements = player.server.getAdvancementLoader().getAdvancements();
        var tracker = player.getAdvancementTracker();
        var unlocks = MCDE.getConfig().getUnlocks().stream()
            .collect(Collectors.partitioningBy(u -> advancements.stream()
                .filter(u.getAdvancements()::contains)
                .allMatch(a -> tracker.getProgress(a).isDone()),
                Collectors.flatMapping(u -> getEnchantmentStream(world).filter(e -> u.getEnchantments().contains(world, e)),
                    Collectors.toSet())));
        unlocks.get(false).removeIf(unlocks.get(true)::contains);
        return unlocks.get(false);
    }

    public static <T extends RegistryEntry<Enchantment>> Optional<T> generateEnchantment(
        ItemStack itemStack,
        Optional<ServerPlayerEntity> optionalOwner,
        Random random,
        List<T> candidates
    ) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        optionalOwner.ifPresent(player -> candidates.removeIf(getLockedEnchantments(player)::contains));
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    public static Set<RegistryEntry<Enchantment>> getAllEnchantmentsInItem(ItemStack itemStack) {
        var present = new HashSet<>(EnchantmentHelper.getEnchantments(itemStack).getEnchantments());
        var slotsOptional = EnchantmentSlots.fromItemStack(itemStack);
        if (slotsOptional.isEmpty()) {
            return present;
        }
        var slots = slotsOptional.get();
        slots.stream()
            .flatMap(s -> s.choices().stream())
            .map(c -> c.getEnchantment()).forEach(present::add);
        return present;
    }

    public static Stream<Reference<Enchantment>> getEnchantmentsNotInItem(World world, ItemStack itemStack) {
        var present = getAllEnchantmentsInItem(itemStack);
        var candidates = getAllEnchantmentsForItem(world, itemStack)
            .filter(id -> !present.contains(id));
        return candidates;
    }

    public static boolean isGilding(RegistryEntry<Enchantment> enchantment, ItemStack itemStack) {
        return EnchantmentSlots.fromItemStack(itemStack)
                .map(slots -> slots.getGilding().contains(enchantment))
                .orElse(false);
    }

    private static List<Reference<Enchantment>> getPossibleCandidates(World world, ItemStack itemStack) {
        var present = getAllEnchantmentsInItem(itemStack);
        var candidates = getAllEnchantmentsForItem(world, itemStack)
            .filter(id -> !present.contains(id));
        if (MCDE.getConfig().isCompatibilityRequired()) {
            candidates = candidates.filter(id -> isCompatible(present, id));
        }
         return candidates.toList();
    }
}
