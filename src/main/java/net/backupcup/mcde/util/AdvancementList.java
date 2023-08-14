package net.backupcup.mcde.util;

import java.util.List;
import java.util.Map;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.annotation.Deserializer;
import blue.endless.jankson.annotation.Serializer;
import blue.endless.jankson.api.SyntaxError;
import net.minecraft.advancement.Advancement;
import net.minecraft.util.Identifier;

public class AdvancementList extends IdentifierGlobList<Advancement> {
    public AdvancementList(String... globs) {
        super(globs);
    }

    protected AdvancementList(Map<String, List<Glob>> namespaces) {
        super(namespaces);
    }

    @Override
    public Identifier getId(Advancement obj) {
        return obj.getId();
    }

    @Override
    @Serializer
    public JsonArray toJson() {
        return super.toJson();
    }

    @Deserializer
    public static AdvancementList fromObject(JsonObject obj) throws SyntaxError {
        return new AdvancementList(IdentifierGlobList.mapFromObject(obj));
    }

    @Deserializer
    public static AdvancementList fromArray(JsonArray arr) throws SyntaxError {
        return new AdvancementList(IdentifierGlobList.mapFromArray(arr));
    }
}
