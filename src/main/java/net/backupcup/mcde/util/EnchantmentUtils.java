package net.backupcup.mcde.util;

import static net.minecraft.registry.Registries.ENCHANTMENT;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;

import net.backupcup.mcde.MCDE;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.ReloadableRegistries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class EnchantmentUtils {
    public static Stream<Reference<Enchantment>> getEnchantmentStream(World world) {
        RegistryEntry<Enchantment> e;
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
        return MCDE.getConfig().isEnchantmentPowerful(enchantment.registryKey().getValue()) ? Formatting.RED : Formatting.LIGHT_PURPLE;
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

    public static Optional<Identifier> generateEnchantment(ItemStack itemStack, Optional<ServerPlayerEntity> optionalOwner, Random random, List<Identifier> candidates) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        optionalOwner.ifPresent(player -> candidates.removeIf(getLockedEnchantments(player)::contains));
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    public static Set<RegistryEntry<Enchantment>> getAllEnchantmentsInItem(ItemStack itemStack) {
        var present = EnchantmentHelper.getEnchantments(itemStack).getEnchantments(); var slotsOptional = EnchantmentSlots.fromItemStack(itemStack);
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
                .map(slots -> slots.getGildingIds().contains(EnchantmentUtils.getEnchantmentId(enchantment)))
                .orElse(false);
    }

    private static List<Identifier> getPossibleCandidates(ItemStack itemStack) {
        var present = getAllEnchantmentsInItem(itemStack);
        var candidates = getAllEnchantmentsForItem(itemStack)
            .filter(id -> !present.contains(id));
        if (MCDE.getConfig().isCompatibilityRequired()) {
            candidates = candidates.filter(id -> isCompatible(present, id));
        }
         return candidates.toList();
    }

    public static <T> Codec<Reference<T>> refCodec(RegistryKey<Registry<T>> key) {
        return RegistryFixedCodec.of(key).xmap(((Reference<T>)null).getClass()::cast, Function.identity());
    }
    public static <T> PacketCodec<RegistryByteBuf, Reference<T>> packetRefCodec(RegistryKey<Registry<T>> key) {
        return PacketCodecs.registryEntry(key).xmap(((Reference<T>)null).getClass()::cast, Function.identity());
    }

}
