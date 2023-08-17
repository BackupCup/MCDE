package net.backupcup.mcde.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public abstract class IdentifierGlobTagList<T> extends IdentifierGlobList<T> {
    private final Map<String, List<Glob>> tags;

    public abstract Registry<T> getRegistry();

    protected IdentifierGlobTagList(Map<String, List<Glob>> globs, Map<String, List<Glob>> tags) {
        super(globs);
        this.tags = tags;
    }


    protected IdentifierGlobTagList(Map<Boolean, Map<String, List<Glob>>> map) {
        this(map.get(false), map.get(true));
    }

    public IdentifierGlobTagList(String... globs) {
        this(Arrays.stream(globs).collect(toGlobsWithTags()));
    }

    public boolean contains(Identifier id) {
        return super.contains(id) ||
            getRegistry().streamTags()
            .filter(tag -> tags.getOrDefault(tag.id().getNamespace(), List.of()).stream().anyMatch(glob -> glob.engine().matches(tag.id().getPath())))
            .anyMatch(tag -> ModTags.isIn(id, tag, getRegistry()));
    }

    @Override
    public JsonArray toJson() {
        return (JsonArray)JANKSON.toJson(Stream.concat(
            tags.entrySet().stream().flatMap(kvp -> kvp.getValue().stream().map(glob -> "#" + kvp.getKey() + ":" + glob.pattern())),
            globs.entrySet().stream().flatMap(kvp -> kvp.getValue().stream().map(glob -> kvp.getKey() + ":" + glob.pattern()))
        ).toList());
    }

    protected static Collector<String, ?, Map<Boolean, Map<String, List<Glob>>>> toGlobsWithTags() {
        return Collectors.partitioningBy(
                s -> s.startsWith("#"),
                Collectors.mapping(s -> s.replaceFirst("#", ""), IdentifierGlobList.toGlobs())
            );
    }

    protected static Collector<Map.Entry<String, String>, ?, Map<Boolean, Map<String, List<Glob>>>> fromMapToGlobsWithTags() {
        return Collectors.partitioningBy(
                kvp -> kvp.getValue().startsWith("#"),
                Collectors.mapping(
                    kvp -> Map.entry(kvp.getKey(), kvp.getValue().replace("#", "")),
                    IdentifierGlobList.fromEntriesToGlobs()
                )
            );
    }

    protected static Map<Boolean, Map<String, List<Glob>>> mapFromArrayWithTags(JsonArray array) throws SyntaxError {
        return array.stream().map(e -> ((JsonPrimitive)e).asString())
            .collect(toGlobsWithTags());
    }

    protected static Map<Boolean, Map<String, List<Glob>>> mapFromObjectWithTags(JsonObject obj) throws SyntaxError {
        return obj.entrySet().stream()
            .flatMap(kvp -> ((JsonArray)kvp.getValue()).stream()
                        .map(e -> Map.entry(kvp.getKey(), ((JsonPrimitive)e).asString())))
            .collect(fromMapToGlobsWithTags());
    }

    @Override
    public String toString() {
        return String.format("IdentifierGlobbedList{%s, %s}",
            globs.toString(),
            tags.toString());
        }

    @Override
    protected Identifier getId(T obj) {
        return getRegistry().getId(obj);
    }
}
