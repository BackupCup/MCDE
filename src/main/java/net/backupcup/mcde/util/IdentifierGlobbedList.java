package net.backupcup.mcde.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Jankson;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.JsonArray;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.JsonObject;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.JsonPrimitive;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Deserializer;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Serializer;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.api.Marshaller;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.api.SyntaxError;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class IdentifierGlobbedList {
    private static final Jankson JANKSON = Jankson.builder().build();
    private final Set<String> namespaces = new HashSet<>();
    private final Set<Identifier> fullySpecified = new HashSet<>();
    private final Set<Identifier> tags = new HashSet<>();
    private final Set<String> namespace_tags = new HashSet<>();

    public IdentifierGlobbedList(Collection<String> globs) {
        for (var glob : globs) {
            if (glob.endsWith(":*")) {
                if (glob.startsWith("#")) {
                    namespace_tags.add(glob.substring(1, glob.length() - 2));
                } else {
                    namespaces.add(glob.substring(0, glob.length() - 2));
                }
            }
            else {
                if (glob.startsWith("#")) {
                    tags.add(Identifier.tryParse(glob.substring(1)));
                } else {
                    fullySpecified.add(Identifier.tryParse(glob));
                }
            }
        }
    }

    public IdentifierGlobbedList(Map<String, List<String>> nested) {
        for (var kvp : nested.entrySet()) {
            var namespace = kvp.getKey();
            var paths = kvp.getValue();
            if (paths.size() >= 1 && paths.get(0).equals("*")) {
                namespaces.add(namespace);
                continue;
            }
            if (paths.size() >= 1 && paths.get(0).equals("#*")) {
                namespace_tags.add(namespace);
            }
            for (var path : paths) {
                if (path.startsWith("#")) {
                    tags.add(Identifier.of(namespace, path.substring(1)));
                } else {
                    fullySpecified.add(Identifier.of(namespace, path));
                }
            }
        }
    }

    public boolean contains(Identifier id) {
        return containsNamespaceGlob(id) ||
            fullySpecified.contains(id) ||
            tags.stream().anyMatch(tag -> ModTags.isIn(id, TagKey.of(Registry.ENCHANTMENT_KEY, tag))) ||
            namespace_tags.stream().flatMap(ns -> Registry.ENCHANTMENT.streamTags().filter(tag -> tag.id().getNamespace().equals(ns)))
                .anyMatch(tag -> ModTags.isIn(Registry.ENCHANTMENT.get(id), tag));
    }

    public boolean contains(Enchantment enchantment) {
        return contains(Registry.ENCHANTMENT.getId(enchantment));
    }

    public boolean containsNamespaceGlob(Identifier id) {
        return namespaces.contains(id.getNamespace());
    }

    public Set<String> getNamespaces() {
        return namespaces;
    }

    public Set<Identifier> getFullySpecified() {
        return fullySpecified;
    }

    @Serializer
    public JsonArray toJson(Marshaller marshaller) {
        return (JsonArray)JANKSON.toJson(Stream.concat(
            getNamespaces().stream().map(ns -> ns + ":*"),
            getFullySpecified().stream().map(id -> id.toString())
        ).toList(), marshaller);
    }

    @Deserializer
    public static IdentifierGlobbedList fromArray(JsonArray array) throws SyntaxError {
        return new IdentifierGlobbedList(array.stream().map(e -> ((JsonPrimitive)e).asString()).toList());
    }

    @Deserializer
    public static IdentifierGlobbedList fromObject(JsonObject obj) throws SyntaxError {
        return new IdentifierGlobbedList(obj.entrySet().stream()
                .map(kvp -> Map.entry(
                    kvp.getKey(),
                    ((JsonArray)kvp.getValue()).stream()
                        .map(e -> ((JsonPrimitive)e).asString()).toList()
                ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @Override
    public String toString() {
        return String.format("IdentifierGlobbedList{%s, %s, %s, %s}",
            namespaces.toString(),
            fullySpecified.toString(),
            tags.toString(),
            namespace_tags.toString());
        }

}
