package net.backupcup.mcde.util;

import java.util.List;
import java.util.Map;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.annotation.Deserializer;
import blue.endless.jankson.annotation.Serializer;
import blue.endless.jankson.api.SyntaxError;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class EnchantmentList extends IdentifierGlobTagList<Enchantment> {
    public EnchantmentList(String... globs) {
        super(globs);
    }

    protected EnchantmentList(Map<Boolean, Map<String, List<Glob>>> map) {
        super(map);
    }

    @Override
    public Registry<Enchantment> getRegistry() {
        return Registries.ENCHANTMENT;
    }

    @Override
    @Serializer
    public JsonArray toJson() {
        return super.toJson();
    }

    @Deserializer
    public static EnchantmentList fromObject(JsonObject obj) throws SyntaxError {
        return new EnchantmentList(IdentifierGlobTagList.mapFromObjectWithTags(obj));
    }

    @Deserializer
    public static EnchantmentList fromArray(JsonArray arr) throws SyntaxError {
        return new EnchantmentList(IdentifierGlobTagList.mapFromArrayWithTags(arr));
    }
}
