package net.backupcup.mcde.util;

import static net.backupcup.mcde.util.SlotPosition.FIRST;
import static net.backupcup.mcde.util.SlotPosition.SECOND;
import static net.backupcup.mcde.util.SlotPosition.THIRD;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import com.ibm.icu.impl.units.MeasureUnitImpl.InitialCompoundPart;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.backupcup.mcde.Config.SlotChances;
import net.backupcup.mcde.MCDEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;

public class SlotsGenerator {
    private ItemStack itemStack;
    private ObjectArrayList<Identifier> pool;
    private Optional<ServerPlayerEntity> optionalOwner;
    private Random random;

    private float threeChoiceChance;
    private SlotChances slotChances;

    public SlotsGenerator(ItemStack itemStack, Optional<ServerPlayerEntity> optionalOwner, Random random, float threeChoiceChance, SlotChances slotChances) {
        this.itemStack = itemStack;
        this.optionalOwner = optionalOwner;
        this.random = random;

        this.threeChoiceChance = threeChoiceChance;
        this.slotChances = slotChances;

        pool = EnchantmentUtils.getEnchantmentsForItem(itemStack).collect(ObjectArrayList.toList());

        if (optionalOwner.isPresent()) {
            MCDEnchantments.LOGGER.info("Removing locked enchantments: {}", EnchantmentUtils.getLockedEnchantments(optionalOwner.get()));
            pool.removeIf(EnchantmentUtils.getLockedEnchantments(optionalOwner.get())::contains);
        };

        if (MCDEnchantments.getConfig().isCompatibilityRequired()) {
            pool.removeIf(id -> !EnchantmentUtils.isCompatible(
                EnchantmentHelper.get(itemStack).keySet()
                    .stream().map(EnchantmentHelper::getEnchantmentId).toList(), id));
        }
    }

    public EnchantmentSlots generateEnchantments() {
        var builder = EnchantmentSlots.builder();
        var pool = (ObjectArrayList<Identifier>)Util.copyShuffled(this.pool, random);
        boolean isTwoChoiceGenerated = false;

        // TODO: prioritize incompatible enchantments in the same slot

        MCDEnchantments.LOGGER.info("Pool: {}", pool);
        MCDEnchantments.LOGGER.info("Pool's size: {}", pool.size());

        if (pool.isEmpty()) {
            return EnchantmentSlots.EMPTY;
        }

        isTwoChoiceGenerated = generateSlot(FIRST, pool, builder, isTwoChoiceGenerated);

        MCDEnchantments.LOGGER.info("Generated first slot: {}", builder.getSlot(FIRST).get());

        if (MCDEnchantments.getConfig().isCompatibilityRequired()) {
            MCDEnchantments.LOGGER.info("Removed incompatible enchantments: {}", removeIncompatible(pool, builder));
        }

        MCDEnchantments.LOGGER.info("Remaining pool: {}", pool);

        if (pool.isEmpty()) {
            return builder.build();
        }

        if (random.nextFloat() < slotChances.getSecondChance()) {
            isTwoChoiceGenerated = generateSlot(SECOND, pool, builder, isTwoChoiceGenerated);
            MCDEnchantments.LOGGER.info("Generated Second slot: {}", builder.getSlot(SECOND).get());
        }
        else {
            return builder.build();
        }

        if (MCDEnchantments.getConfig().isCompatibilityRequired()) {
            MCDEnchantments.LOGGER.info("Removed incompatible enchantments: {}", removeIncompatible(pool, builder));
        }
        MCDEnchantments.LOGGER.info("Remaining pool: {}", pool);

        if (pool.isEmpty()) {
            return builder.build();
        }

        if (random.nextFloat() < slotChances.getThirdChance()) {
            isTwoChoiceGenerated = generateSlot(THIRD, pool, builder, isTwoChoiceGenerated);
            MCDEnchantments.LOGGER.info("Generated third slot: {}", builder.getSlot(THIRD).get());
        }

        return builder.build();
    }

    private boolean generateSlot(SlotPosition pos, ObjectArrayList<Identifier> pool, EnchantmentSlots.Builder builder, boolean isTwoChoiceGenerated) {
        if (!isTwoChoiceGenerated && random.nextFloat() < threeChoiceChance && pool.size() >= 3 && getPoolSize() >= 6) {
            if (MCDEnchantments.getConfig().isCompatibilityRequired()) {
                moveIncompatibleToTheEnd(pool, 3);
            }
            builder.withSlot(pos, pool.pop(), pool.pop(), pool.pop());
        }
        else if (pool.size() > 2) {
            if (MCDEnchantments.getConfig().isCompatibilityRequired()) {
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

    public static Builder forItemStack(ItemStack itemStack) {
        return new Builder(itemStack);
    }

    private int getPoolSize() {
        return pool.size();
    }

    private static SlotChances calculateAdvancementModifiers(ServerPlayerEntity player) {
        return MCDEnchantments.getConfig().getProgressChances().entrySet().stream()
            .filter(kvp -> player.getAdvancementTracker().getProgress(player.server.getAdvancementLoader().get(kvp.getKey())).isDone())
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

    private void moveIncompatibleToTheEnd(ObjectArrayList<Identifier> pool, int availableSlots) {
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
            pool.remove(left);
            right--;
        }
    }

    private static List<Identifier> removeIncompatible(List<Identifier> pool, EnchantmentSlots.Builder builder) {
        var present = builder.getAdded().stream().map(c -> c.getEnchantmentId()).toList();
        var incompatible = pool.stream().filter(id -> !EnchantmentUtils.isCompatible(present, id)).toList();
        pool.removeIf(incompatible::contains);
        return incompatible;
    }

    private static float getDefaultThreeChoiceChance(ItemStack itemStack) {
        return 0.5f + itemStack.getItem().getEnchantability() / 100f;
    }

    public static class Builder {
        private ItemStack itemStack;
        private Optional<ServerPlayerEntity> optionalOwner = Optional.empty();
        private Optional<Random> random = Optional.empty();
        private Optional<Float> threeChoiceChance = Optional.empty();
        private Optional<Float> secondSlotChance = Optional.empty();
        private Optional<Float> thirdSlotChance = Optional.empty();
        private boolean isSecondSlotChanceAbsolute = false;
        private boolean isThirdSlotChanceAbsolute = false;

        private Builder(ItemStack itemStack) {
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
                    secondSlotChance.orElseGet(MCDEnchantments.getConfig()::getSecondSlotBaseChance),
                    thirdSlotChance.orElseGet(MCDEnchantments.getConfig()::getThirdSlotBaseChance)
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
                    itemStack,
                    optionalOwner,
                    random.orElseGet(() -> new LocalRandom(System.nanoTime())),
                    threeChoiceChance.orElse(getDefaultThreeChoiceChance(itemStack)),
                    slotChances
                    );
        }
    }

}
