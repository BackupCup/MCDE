package net.backupcup.mcde.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.hrakaroo.glob.GlobPattern;
import com.hrakaroo.glob.MatchingEngine;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;
import net.minecraft.util.Identifier;

public abstract class IdentifierGlobList<T> {
    protected static record Glob(MatchingEngine engine, String pattern) {
        private Glob(String pattern) {
            this(GlobPattern.compile(pattern), pattern);
        }
    };
    protected static final Jankson JANKSON = Jankson.builder().build();
    protected final Map<String, List<Glob>> globs;

    protected IdentifierGlobList(Map<String, List<Glob>> namespaces) {
        this.globs = namespaces;
    }

    public IdentifierGlobList(String... globs) {
        this(Arrays.stream(globs).collect(toGlobs()));
    }

    public Set<String> getGlobs() {
        return globs.keySet();
    }

    public boolean contains(Identifier id) {
        return globs.getOrDefault(id.getNamespace(), List.of()).stream()
            .anyMatch(glob -> glob.engine.matches(id.getPath()));
    }

    public boolean contains(T obj) {
        return contains(getId(obj));
    }

    protected abstract Identifier getId(T obj);

    public JsonArray toJson() {
        return (JsonArray)JANKSON.toJson(globs.entrySet().stream()
                .flatMap(kvp -> kvp.getValue().stream().map(glob -> kvp.getKey() + ":" + glob.pattern)).toList());
    }

    protected static Collector<String, ?, Map<String, List<Glob>>> toGlobs() {
        return Collectors.mapping(
            glob -> glob.split(":", 2),
            Collectors.groupingBy(
                parts -> parts[0],
                Collectors.mapping(parts -> new Glob(parts[1]), Collectors.toList())
            )
        );
    }

    protected static Collector<Map.Entry<String, String>, ?, Map<String, List<Glob>>> fromEntriesToGlobs() {
        return Collectors.groupingBy(
                kvp -> kvp.getKey(),
                Collectors.mapping(kvp -> new Glob(kvp.getValue()), Collectors.toList())
            );
    }

    protected static Collector<Map.Entry<String, Stream<String>>, ?, Map<String, List<Glob>>> toGlobsFromMap() {
        return Collectors.mapping(kvp -> Map.entry(
            kvp.getKey(),
            kvp.getValue().map(Glob::new).toList()
        ), Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected static Map<String, List<Glob>> mapFromArray(JsonArray arr) throws SyntaxError {
        return arr.stream().map(e -> ((JsonPrimitive)e).asString()).collect(toGlobs());
    }

    protected static Map<String, List<Glob>> mapFromObject(JsonObject obj) throws SyntaxError {
        return obj.entrySet().stream()
            .map(kvp -> Map.entry(
                kvp.getKey(),
                ((JsonArray)kvp.getValue()).stream().map(e -> ((JsonPrimitive)e).asString())
            )).collect(toGlobsFromMap());
    }
}
