package net.backupcup.mcde.util;

import static net.backupcup.mcde.util.SlotPosition.FIRST;
import static net.backupcup.mcde.util.SlotPosition.SECOND;
import static net.backupcup.mcde.util.SlotPosition.THIRD;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.backupcup.mcde.Config.SlotChances;
import net.backupcup.mcde.MCDE;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class SlotsGenerator {
    private ObjectArrayList<Reference<Enchantment>> pool;
    private Random random;

    private float threeChoiceChance;
    private SlotChances slotChances;

    public SlotsGenerator(World world, ItemStack itemStack, Optional<ServerPlayerEntity> optionalOwner, Random random, float threeChoiceChance, SlotChances slotChances) {
        this.random = random;

        this.threeChoiceChance = threeChoiceChance;
        this.slotChances = slotChances;

        pool = EnchantmentUtils.getEnchantmentsForItem(world, itemStack).collect(ObjectArrayList.toList());

        if (optionalOwner.isPresent()) {
            pool.removeIf(EnchantmentUtils.getLockedEnchantments(optionalOwner.get())::contains);
        };

        if (MCDE.getConfig().isCompatibilityRequired()) {
            var present = EnchantmentHelper.getEnchantments(itemStack).getEnchantments();
            pool.removeIf(id -> !EnchantmentUtils.isCompatible(present, id));
        }
    }

    public EnchantmentSlots.Builder generateEnchantments() {
        var builder = EnchantmentSlots.builder();
        var pool = (ObjectArrayList<Reference<Enchantment>>)Util.copyShuffled(this.pool, random);
        boolean isTwoChoiceGenerated = false;

        if (pool.isEmpty()) {
            return builder;
        }

        isTwoChoiceGenerated = generateSlot(FIRST, pool, builder, isTwoChoiceGenerated);

        if (MCDE.getConfig().isCompatibilityRequired()) {
            removeIncompatible(pool, builder);
        }

        if (pool.isEmpty()) {
            return builder;
        }

        if (random.nextFloat() < slotChances.getSecondChance()) {
            isTwoChoiceGenerated = generateSlot(SECOND, pool, builder, isTwoChoiceGenerated);
        }
        else {
            return builder;
        }

        if (MCDE.getConfig().isCompatibilityRequired()) {
            removeIncompatible(pool, builder);
        }

        if (pool.isEmpty()) {
            return builder;
        }

        if (random.nextFloat() < slotChances.getThirdChance()) {
            isTwoChoiceGenerated = generateSlot(THIRD, pool, builder, isTwoChoiceGenerated);
        }

        return builder;
    }

    private boolean generateSlot(SlotPosition pos, ObjectArrayList<Reference<Enchantment>> pool, EnchantmentSlots.Builder builder, boolean isTwoChoiceGenerated) {
        if (!isTwoChoiceGenerated && random.nextFloat() < threeChoiceChance && pool.size() >= 3 && getPoolSize() >= 6) {
            if (MCDE.getConfig().isCompatibilityRequired()) {
                moveIncompatibleToTheEnd(pool, 3);
            }
            builder.withSlot(pos, pool.pop(), pool.pop(), pool.pop());
        }
        else if (pool.size() > 2) {
            if (MCDE.getConfig().isCompatibilityRequired()) {
                moveIncompatibleToTheEnd(pool, 2);
            }
            builder.withSlot(pos, pool.pop(), pool.pop());
            isTwoChoiceGenerated = true;
        }
        else if (pool.size() == 2 && random.nextFloat() < 1 - threeChoiceChance) {
            builder.withSlot(pos, pool.pop());
        }
        else {
            builder.withSlot(pos, pool.pop());
        }
        return isTwoChoiceGenerated;
    }

    public static Builder forItemStack(World world, ItemStack itemStack) {
        return new Builder(world, itemStack);
    }

    private int getPoolSize() {
        return pool.size();
    }

    private static SlotChances calculateAdvancementModifiers(ServerPlayerEntity player) {
        var tracker = player.getAdvancementTracker();
        var loader = player.server.getAdvancementLoader();
        return MCDE.getConfig().getProgressChances().entrySet().stream()
            .filter(kvp -> Optional.ofNullable(
                loader.get(kvp.getKey())
            ).map(a -> tracker.getProgress(a).isDone()).orElse(false))
            .map(Map.Entry::getValue)
            .reduce(new SlotChances(), SlotChances::add);
    }

    private static float calculateEnchantabilityModifier(float baseChance, int enchantability) {
        // Some magic math here. The idea is to make low enchantability add less chance than high one
        // the significance function really can be any other one
        // one requirement for such function is its values should be from 0 to 1
        float significance = (float)(1 / Math.PI * Math.atan(0.2 * (enchantability - 20)) + 0.5);
        return -significance * baseChance + significance;
    }

    private static SlotChances calculateEnchantabilityModifier(SlotChances baseChances, int enchantability) {
        return SlotChances.apply(baseChances, c -> calculateEnchantabilityModifier(c, enchantability));
    }

    private void moveIncompatibleToTheEnd(ObjectArrayList<Reference<Enchantment>> pool, int availableSlots) {
        int left = 0, right = pool.size() - 2, found = 0;
        while (left <= right) {
            var id = pool.get(left);
            var incompatible = IntStream.range(pool.size() - availableSlots, pool.size())
            .filter(i -> !EnchantmentUtils.isCompatible(pool.get(i), id))
            .boxed()
            .toList();
            if (incompatible.isEmpty()) {
                left++;
                continue;
            }
            if (found == 2 && incompatible.get(0) != pool.size() - 2) {
                Collections.swap(pool, left, right);
                return;
            }
            if (incompatible.size() == availableSlots) {
                Collections.swap(pool, left, pool.size() - availableSlots + random.nextInt(availableSlots));
                found = availableSlots;
                return;
            }
            else if (incompatible.size() == availableSlots - 1) {
                int remaining = IntStream.range(pool.size() - availableSlots, pool.size())
                    .filter(i -> EnchantmentUtils.isCompatible(pool.get(i), id))
                    .findAny().getAsInt();
                Collections.swap(pool, left, remaining);
                return;
            }
            else {
                Collections.swap(pool, incompatible.get(0), pool.size() - 1);
                Collections.swap(pool, left, pool.size() - 2);
                right -= 2;
                found = 2;
            }
        }
    }

    private static List<Reference<Enchantment>> removeIncompatible(List<Reference<Enchantment>> pool, EnchantmentSlots.Builder builder) {
        var present = builder.getAdded().stream().map(c -> c.getEnchantment()).toList();
        var incompatible = pool.stream().filter(id -> !EnchantmentUtils.isCompatible(present, id)).toList();
        pool.removeIf(incompatible::contains);
        return incompatible;
    }

    private static float getDefaultThreeChoiceChance(ItemStack itemStack) {
        return 0.5f + itemStack.getItem().getEnchantability() / 100f;
    }

    public static class Builder {
        private World world;
        private ItemStack itemStack;
        private Optional<ServerPlayerEntity> optionalOwner = Optional.empty();
        private Optional<Random> random = Optional.empty();
        private Optional<Float> threeChoiceChance = Optional.empty();
        private Optional<Float> secondSlotChance = Optional.empty();
        private Optional<Float> thirdSlotChance = Optional.empty();
        private boolean isSecondSlotChanceAbsolute = false;
        private boolean isThirdSlotChanceAbsolute = false;

        private Builder(World world, ItemStack itemStack) {
            this.world = world;
            this.itemStack = itemStack;
        }

        public Builder withOwner(ServerPlayerEntity playerEntity) {
            optionalOwner = Optional.of(playerEntity);
            return this;
        }

        public Builder withOptionalOwner(Optional<ServerPlayerEntity> optionalPlayerEntity) {
            optionalOwner = optionalPlayerEntity;
            return this;
        }

        public Builder withRandom(Random random) {
            this.random = Optional.of(random);
            return this;
        }

        public Builder withThreeChoiceChance(float chance) {
            threeChoiceChance = Optional.of(chance);
            return this;
        }

        public Builder withSecondSlotBaseChance(float chance) {
            secondSlotChance = Optional.of(chance);
            return this;
        }

        public Builder withThirdSlotBaseChance(float chance) {
            thirdSlotChance = Optional.of(chance);
            return this;
        }

        public Builder withSecondSlotAbsoluteChance(float chance) {
            secondSlotChance = Optional.of(chance);
            isSecondSlotChanceAbsolute = true;
            return this;
        }

        public Builder withThirdSlotAbsoluteChance(float chance) {
            thirdSlotChance = Optional.of(chance);
            isThirdSlotChanceAbsolute = true;
            return this;
        }

        public Builder withBaseChances(float second, float third) {
            secondSlotChance = Optional.of(second);
            thirdSlotChance = Optional.of(third);
            return this;
        }

        public Builder withAbsoluteChances(float second, float third) {
            secondSlotChance = Optional.of(second);
            thirdSlotChance = Optional.of(third);
            isSecondSlotChanceAbsolute = true;
            isThirdSlotChanceAbsolute = true;
            return this;
        }

        public SlotsGenerator build() {
            var slotChances = new SlotChances(
                    secondSlotChance.orElseGet(MCDE.getConfig()::getSecondSlotBaseChance),
                    thirdSlotChance.orElseGet(MCDE.getConfig()::getThirdSlotBaseChance)
                    );
            if (!(isSecondSlotChanceAbsolute && isThirdSlotChanceAbsolute) && optionalOwner.isPresent()) {
                var modifiers = SlotChances.add(
                        calculateAdvancementModifiers(optionalOwner.get()),
                        calculateEnchantabilityModifier(slotChances, itemStack.getItem().getEnchantability())
                        );
                if (isSecondSlotChanceAbsolute) {
                    modifiers.setSecondChance(0);
                }
                if (isThirdSlotChanceAbsolute) {
                    modifiers.setThirdChance(0);
                }
                slotChances = SlotChances.add(slotChances, modifiers);
            }
            return new SlotsGenerator(
                    world,
                    itemStack,
                    optionalOwner,
                    random.orElseGet(() -> new LocalRandom(System.nanoTime())),
                    threeChoiceChance.orElse(getDefaultThreeChoiceChance(itemStack)),
                    slotChances
                    );
        }
    }

}
