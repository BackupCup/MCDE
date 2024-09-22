package net.backupcup.mcde.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.SimpleMapCodec;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;

public enum SlotPosition implements StringIdentifiable {
    FIRST, SECOND, THIRD;

    @Override
    public String asString() {
        return toString();
    }

    public static final Codec<SlotPosition> CODEC =
        StringIdentifiable.createCodec(() -> values());

    public static final PacketCodec<RegistryByteBuf, SlotPosition> PACKET_CODEC = 
        PacketCodecs.indexed(i -> values()[i], SlotPosition::ordinal).cast();
        

    public static <T> SimpleMapCodec<SlotPosition, T> mapCodec(Codec<T> valueCodec) {
        return Codec.simpleMap(CODEC, valueCodec, StringIdentifiable.toKeyable(values()));
    }
}
