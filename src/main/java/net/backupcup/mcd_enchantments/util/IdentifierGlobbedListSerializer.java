package net.backupcup.mcd_enchantments.util;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

public class IdentifierGlobbedListSerializer implements TypeSerializer<IdentifierGlobbedList> {
    public static final IdentifierGlobbedListSerializer INSTANCE = new IdentifierGlobbedListSerializer();

    @Override
    public IdentifierGlobbedList deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (node.isList()) {
            return new IdentifierGlobbedList(node.getList(String.class));
        }
        if (!node.isMap()) {
            throw new SerializationException("Identifier list must be either list of strings or a map with nested list of strings");
        }
        var map = node.childrenMap().entrySet().stream().map(kvp -> {
            List<String> list = List.of();
            try {
                list = kvp.getValue().getList(String.class);
            }
            catch (SerializationException e) {
                e.printStackTrace();
            }
            return Map.entry(kvp.getKey().toString(), list);
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new IdentifierGlobbedList(map);
    }

    @Override
    public void serialize(Type type, @Nullable IdentifierGlobbedList list, ConfigurationNode node)
            throws SerializationException {
        if (list == null) {
            node.raw(null);
            return;
        }
        node.setList(String.class, Stream.concat(
            list.getNamespaces().stream().map(ns -> ns + ":*"),
            list.getFullySpecified().stream().map(id -> id.toString())
        ).toList());
    }
}
