package net.backupcup.mcde.util;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
import net.minecraft.util.dynamic.Codecs;

public class EnchantmentSlot {
    public static final Codec<EnchantmentSlot> CODEC =
        RecordCodecBuilder.create(instance -> instance.group(
            SlotPosition.CODEC.fieldOf("slot").forGetter(EnchantmentSlot::getSlotPosition),
            SlotPosition.mapCodec(RegistryFixedCodec.of(RegistryKeys.ENCHANTMENT)).fieldOf("enchantments").forGetter(slot -> slot.enchantments),
            Codec.INT.fieldOf("level").forGetter(EnchantmentSlot::getLevel),
            Codecs.optional(SlotPosition.CODEC).fieldOf("chosen").forGetter(EnchantmentSlot::getChosenPosition)
        ).apply(instance, EnchantmentSlot::new));

    public static final PacketCodec<RegistryByteBuf, EnchantmentSlot> PACKET_CODEC =
        PacketCodec.tuple(
            SlotPosition.PACKET_CODEC, EnchantmentSlot::getSlotPosition,
            PacketCodecs.map(
                n -> new EnumMap<>(SlotPosition.class),
                SlotPosition.PACKET_CODEC,
                PacketCodecs.registryEntry(RegistryKeys.ENCHANTMENT)
            ), slot -> slot.enchantments,
            PacketCodecs.VAR_INT, EnchantmentSlot::getLevel,
            PacketCodecs.optional(SlotPosition.PACKET_CODEC), EnchantmentSlot::getChosenPosition,
            EnchantmentSlot::new
        );


    private SlotPosition slot;
    private Map<SlotPosition, RegistryEntry<Enchantment>> enchantments;
    private int level = 0;
    private Optional<SlotPosition> chosen = Optional.empty();

    public EnchantmentSlot(SlotPosition slot, Map<SlotPosition, RegistryEntry<Enchantment>> enchantments) {
        this(slot, enchantments, 0, Optional.empty());
    }

    public EnchantmentSlot(SlotPosition slot, Map<SlotPosition, RegistryEntry<Enchantment>> enchantments, int level) {
        this(slot, enchantments, level, Optional.empty());
    }

    public EnchantmentSlot(SlotPosition slot, Map<SlotPosition, RegistryEntry<Enchantment>> enchantments, int level, SlotPosition chosen) {
        this(slot, enchantments, level, Optional.of(chosen));
    }

    public EnchantmentSlot(SlotPosition slot, Map<SlotPosition, RegistryEntry<Enchantment>> enchantments, int level, Optional<SlotPosition> chosen) {
        this.slot = slot;
        this.enchantments = enchantments;
        this.level = level;
        this.chosen = chosen;
    }

    public Optional<SlotPosition> getChosenPosition() {
        return chosen;
    }

    public Optional<Choice> getChosen() {
        return chosen.map(pos -> new Choice(this, pos));
    }

    public Optional<EnchantmentSlot> withChosen(SlotPosition pos, int level) {
        if (enchantments.containsKey(pos)) {
            return Optional.of(new EnchantmentSlot(slot, enchantments, level, pos));
        }
        return Optional.empty();
    }

    public EnchantmentSlot withoutChoice() {
        return new EnchantmentSlot(slot, enchantments);
    }

    public SlotPosition getSlotPosition() {
        return slot;
    }

    public int ordinal() {
        return slot.ordinal();
    }

    public Optional<RegistryEntry<Enchantment>> getChoice(SlotPosition pos) {
        return Optional.ofNullable(enchantments.get(pos));
    }

    public List<Choice> choices() {
        return enchantments.keySet().stream().map(pos -> new Choice(this, pos)).toList();
    }

    public int getLevel() {
        return level;
    }

    public EnchantmentSlot withLevel(int level) {
        return new EnchantmentSlot(slot, enchantments, level);
    }

    public EnchantmentSlot withUpgrade() {
        if (!isMaxedOut()) {
            return new EnchantmentSlot(slot, enchantments, level + 1);
        }
        return this;
    }

    public boolean isMaxedOut() {
        return chosen.map(pos -> level >= enchantments.get(pos).value().getMaxLevel()).orElse(false);
    }

    public void removeChosenEnchantment(ItemStack itemStack) {
        getChosen().ifPresent(c -> {
            var builder = new ItemEnchantmentsComponent.Builder(EnchantmentHelper.getEnchantments(itemStack));
            builder.set(c.getEnchantment(), 0);
            EnchantmentHelper.set(itemStack, builder.build());
        });
    }

    public static EnchantmentSlot of(SlotPosition slot, RegistryEntry<Enchantment> first) {
        return new EnchantmentSlot(slot, Map.of(SlotPosition.FIRST, first));
    }

    public static EnchantmentSlot of(SlotPosition slot, RegistryEntry<Enchantment> first, RegistryEntry<Enchantment> second) {
        return new EnchantmentSlot(slot, Map.of(SlotPosition.FIRST, first, SlotPosition.SECOND, second));
    }

    public static EnchantmentSlot of(SlotPosition slot, RegistryEntry<Enchantment> first, RegistryEntry<Enchantment> second, RegistryEntry<Enchantment> third) {
        return new EnchantmentSlot(slot, Map.of(SlotPosition.FIRST, first, SlotPosition.SECOND, second, SlotPosition.THIRD, third));
    }

    @Override
    public String toString() {
        var encoded = CODEC.encodeStart(JsonOps.INSTANCE, this);
        if (encoded.isError()) {
            return "Decode error: " + encoded.error().get().message();
        }
        return encoded.result().get().toString();
    }
}
