package net.backupcup.mcde.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class IdentifierGlobbedList {
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

    public boolean containsNamespaceGlob(Identifier id) {
        return namespaces.contains(id.getNamespace());
    }

    public Set<String> getNamespaces() {
        return namespaces;
    }

    public Set<Identifier> getFullySpecified() {
        return fullySpecified;
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
