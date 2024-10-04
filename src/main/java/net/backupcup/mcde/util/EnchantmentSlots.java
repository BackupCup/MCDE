package net.backupcup.mcde.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.backupcup.mcde.MCDE;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.util.Identifier;

public class EnchantmentSlots implements Iterable<EnchantmentSlot> {
    public static final Codec<EnchantmentSlots> CODEC = 
        RecordCodecBuilder.create(instance -> instance.group(
            SlotPosition.mapCodec(EnchantmentSlot.CODEC).fieldOf("slots").forGetter(slots -> slots.slots),
            Codec.list(RegistryFixedCodec.of(RegistryKeys.ENCHANTMENT)).xmap(
                list -> list.stream().collect(Collectors.toSet()),
                set -> set.stream().toList()
            ).fieldOf("gilding").forGetter(EnchantmentSlots::getGilding),
            Codec.INT.fieldOf("nextRerollCost").forGetter(EnchantmentSlots::getNextRerollCost),
            Codec.INT.fieldOf("nextRerollCostPowerful").forGetter(EnchantmentSlots::getNextRerollCostPowerful)
        ).apply(instance, EnchantmentSlots::new));

    public static final PacketCodec<RegistryByteBuf, EnchantmentSlots> PACKET_CODEC =
        PacketCodec.tuple(
            PacketCodecs.map(
                n -> new EnumMap<>(SlotPosition.class),
                SlotPosition.PACKET_CODEC,
                EnchantmentSlot.PACKET_CODEC
            ), slots -> slots.slots,
            PacketCodecs.collection(
                HashSet::new,
                PacketCodecs.registryEntry(RegistryKeys.ENCHANTMENT)
            ), EnchantmentSlots::getGilding,
            PacketCodecs.VAR_INT, EnchantmentSlots::getNextRerollCost,
            PacketCodecs.VAR_INT, EnchantmentSlots::getNextRerollCostPowerful,
            EnchantmentSlots::new
        );

    public static final ComponentType<EnchantmentSlots> COMPONENT_TYPE = ComponentType.<EnchantmentSlots>builder()
        .codec(CODEC)
        .packetCodec(PACKET_CODEC)
        .build();

    private Map<SlotPosition, EnchantmentSlot> slots;
    private Set<RegistryEntry<Enchantment>> gilding;
    private int nextRerollCost;
    private int nextRerollCostPowerful;

    public EnchantmentSlots(Map<SlotPosition, EnchantmentSlot> slots, Set<RegistryEntry<Enchantment>> gilding, int nextRerollCost, int nextRerollCostPowerful) {
        this.slots = slots;
        this.gilding = gilding;
        this.nextRerollCost = nextRerollCost;
        this.nextRerollCostPowerful = nextRerollCostPowerful;
    }

    public EnchantmentSlots(Map<SlotPosition, EnchantmentSlot> slots) {
        this(
            slots,
            new HashSet<>(),
            MCDE.getConfig().getRerollCostParameters().getNormal().getStartCost(),
            MCDE.getConfig().getRerollCostParameters().getPowerful().getStartCost()
        );
    }

    public boolean hasGilding() {
        return !gilding.isEmpty();
    }

    public boolean hasGilding(RegistryEntry<Enchantment> enchantment) {
        return gilding.contains(enchantment);
    }

    public Set<RegistryEntry<Enchantment>> getGilding() {
        return gilding;
    }

    public EnchantmentSlots withGilding(RegistryEntry<Enchantment> gilding) {
        var newGilding = new HashSet<>(getGilding());
        newGilding.add(gilding);
        return new EnchantmentSlots(slots, newGilding, nextRerollCost, nextRerollCostPowerful);
    }

    public EnchantmentSlots withGildings(Collection<Identifier> gilidngs) {
        var newGilding = new HashSet<>(getGilding());
        newGilding.addAll(gilding);
        return new EnchantmentSlots(slots, newGilding, nextRerollCost, nextRerollCostPowerful);
    }

    public EnchantmentSlots withoutGilding(RegistryEntry<Enchantment> gilding) {
        var newGilding = new HashSet<>(getGilding());
        newGilding.remove(gilding);
        return new EnchantmentSlots(slots, newGilding, nextRerollCost, nextRerollCostPowerful);
    }

    public EnchantmentSlots withoutGildings() {
        return new EnchantmentSlots(slots, Set.of(), nextRerollCost, nextRerollCostPowerful);
    }

    public int getNextRerollCost() {
        return nextRerollCost;
    }

    public EnchantmentSlots withNextRerollCost(int nextRerollCost) {
        return new EnchantmentSlots(slots, gilding, nextRerollCost, nextRerollCostPowerful);
    }

    public int getNextRerollCostPowerful() {
        return nextRerollCostPowerful;
    }

    public EnchantmentSlots withNextRerollCostPowerful(int nextRerollCostPowerful) {
        return new EnchantmentSlots(slots, gilding, nextRerollCost, nextRerollCostPowerful);
    }

    public int getNextRerollCost(Identifier id) {
        return MCDE.getConfig().isEnchantmentPowerful(id) ? nextRerollCostPowerful : nextRerollCost;
    }

    public static class Builder {
        private Map<SlotPosition, EnchantmentSlot> slots = new EnumMap<>(SlotPosition.class);
        private Set<RegistryEntry<Enchantment>> gilding = new HashSet<>();
        private int nextRerollCost = MCDE.getConfig().getRerollCostParameters().getNormal().getStartCost();
        private int nextRerollCostPowerful = MCDE.getConfig().getRerollCostParameters().getPowerful().getEndCost();

        public Builder() {}

        public Builder(EnchantmentSlots slots) {
            this.slots = new EnumMap<>(slots.slots);
            this.gilding = new HashSet<>(slots.gilding);
            this.nextRerollCost = slots.nextRerollCost;
            this.nextRerollCostPowerful = slots.nextRerollCostPowerful;
        }

        public Builder withSlot(SlotPosition slot, RegistryEntry<Enchantment> first) {
            slots.put(slot, EnchantmentSlot.of(slot, first));
            return this;
        }

        public Builder withSlot(SlotPosition slot, RegistryEntry<Enchantment> first, RegistryEntry<Enchantment> second) {
            slots.put(slot, EnchantmentSlot.of(slot, first, second));
            return this;
        }

        public Builder withSlot(SlotPosition slot, RegistryEntry<Enchantment> first, RegistryEntry<Enchantment> second, RegistryEntry<Enchantment> third) {
            slots.put(slot, EnchantmentSlot.of(slot, first, second, third));
            return this;
        }

        public List<Choice> getAdded() {
            var added = new ArrayList<Choice>();
            for (var slot : slots.values()) {
                added.addAll(slot.choices());
            }
            return added;
        }

        public Optional<EnchantmentSlot> getSlot(SlotPosition pos) {
            return Optional.ofNullable(slots.get(pos));
        }

        public Builder addGilding(RegistryEntry<Enchantment> enchantment) {
            gilding.add(enchantment);
            return this;
        }

        public Builder removeGilding(RegistryEntry<Enchantment> enchantment) {
            gilding.remove(enchantment);
            return this;
        }

        public Builder clearGildings() {
            gilding.clear();
            return this;
        }

        public Builder setNextRerollCost(int nextRerollCost) {
            this.nextRerollCost = nextRerollCost;
            return this;
        }

        public Builder setNextRerollCostPowerful(int nextRerollCost) {
            this.nextRerollCostPowerful = nextRerollCost;
            return this;
        }

        public EnchantmentSlots build() {
            return new EnchantmentSlots(
                Collections.unmodifiableMap(slots),
                gilding,
                nextRerollCost,
                nextRerollCostPowerful
            );
        }
    }

    public static EnchantmentSlots EMPTY = new EnchantmentSlots(Map.of(), Set.of(), 0, 0);

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(EnchantmentSlots slots) {
        return new Builder(slots);
    }

    public Optional<EnchantmentSlot> getEnchantmentSlot(SlotPosition pos) {
        return Optional.ofNullable(slots.get(pos));
    }

    @Override
    public String toString() {
        return String.format("%s gilded with %s", slots.toString(), gilding.toString());
    }

    public void putEnchantmentsIntoComponent(ItemStack itemStack, ItemEnchantmentsComponent.Builder componentBuilder) {
        for (var slot : this) {
            slot.getChosen().ifPresent(c -> componentBuilder.add(c.getEnchantment(), c.getLevel()));
        }
        for (var gild : gilding) {
            componentBuilder.add(gild, 1);
        }
    }

    public void removeChosenFromComponent(ItemEnchantmentsComponent.Builder componentBuilder) {
        for (var slot : this) {
            slot.getChosen().ifPresent(c -> componentBuilder.set(c.getEnchantment(), 0));
        }
    }

    public void removeGildingFromComponent(ItemEnchantmentsComponent.Builder componentBuilder) {
        for (var gild : gilding) {
            componentBuilder.set(gild, 0);
        }
    }

    public static Optional<EnchantmentSlots> fromItemStack(ItemStack itemStack) {
        return Optional.ofNullable(itemStack.get(COMPONENT_TYPE));
    }

    @Override
    public Iterator<EnchantmentSlot> iterator() {
        return slots.values().iterator();
    }

    public Stream<EnchantmentSlot> stream() {
        return slots.values().stream();
    }

    public static EnchantmentSlots merge(EnchantmentSlots first, EnchantmentSlots second) {
        var mergedSlotMap = new EnumMap<SlotPosition, EnchantmentSlot>(SlotPosition.class);
        mergedSlotMap.putAll(first.slots);
        for (var kvp : second.slots.entrySet()) {
            mergedSlotMap.putIfAbsent(kvp.getKey(), kvp.getValue());
        }
        Set<RegistryEntry<Enchantment>> newGilding = switch (MCDE.getConfig().getGildingMergeStrategy()) {
            case REMOVE -> new HashSet<>();
            case FIRST -> new HashSet<>(first.gilding);
            case SECOND -> new HashSet<>(second.gilding);
            case BOTH -> Stream.concat(first.gilding.stream(), second.gilding.stream()).collect(Collectors.toSet());
        };
        for (var kvp : mergedSlotMap.entrySet()) {
            if (kvp.getValue().getChosen().map(c -> newGilding.contains(c.getEnchantment())).orElse(false)) {
                kvp.setValue(kvp.getValue().withoutChoice());
            }
        }
        return new EnchantmentSlots(
            mergedSlotMap,
            newGilding,
            (first.nextRerollCost + second.nextRerollCost) / 2,
            (first.nextRerollCostPowerful + second.nextRerollCostPowerful) / 2
        );
    }
}
