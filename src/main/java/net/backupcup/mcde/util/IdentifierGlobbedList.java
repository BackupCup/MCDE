package net.backupcup.mcde.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.Identifier;

public class IdentifierGlobbedList {
    private final Set<String> namespaces = new HashSet<>();
    private final Set<Identifier> fullySpecified = new HashSet<>();

    public IdentifierGlobbedList(Collection<String> globs) {
        for (var glob : globs) {
            if (glob.endsWith(":*")) {
                namespaces.add(glob.substring(0, glob.length() - 2));
            }
            else {
                fullySpecified.add(Identifier.tryParse(glob));
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
            for (var path : paths) {
                fullySpecified.add(Identifier.of(namespace, path));
            }
        }
    }

    public boolean contains(Identifier id) {
        return containsNamespaceGlob(id) || fullySpecified.contains(id);
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
        return "IdentifierGlobbedList{" +namespaces.toString() + ", " + fullySpecified.toString() + "}";
    }

}
