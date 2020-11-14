package io.izzel.arclight.common.mod.util.remapper;

import com.google.common.collect.ImmutableSet;
import net.md_5.specialsource.provider.InheritanceProvider;
import net.md_5.specialsource.repo.ClassRepo;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PluginInheritanceProvider implements InheritanceProvider {

    private static final Map<String, Collection<String>> SHARED_INHERITANCE_MAP = new ConcurrentHashMap<>();

    private final ClassRepo classRepo;

    public PluginInheritanceProvider(ClassRepo classRepo) {
        this.classRepo = classRepo;
    }

    @Override
    public Collection<String> getParents(String className) {
        ClassNode node = classRepo.findClass(className);
        if (node == null) return Collections.emptyList();

        Collection<String> parents = new HashSet<>(node.interfaces);
        if (node.superName != null) {
            parents.add(node.superName);
        }

        return parents;
    }

    public Collection<String> getAll(String className) {
        Collection<String> collection = SHARED_INHERITANCE_MAP.get(className);
        if (collection != null) return collection;

        ClassNode node = classRepo.findClass(className);
        if (node == null) return ImmutableSet.of("java/lang/Object");
        Collection<String> parents = new HashSet<>(node.interfaces);
        parents.add(node.name);
        if (node.superName != null) {
            parents.add(node.superName);
            parents.addAll(getAll(node.superName));
        } else {
            parents.add("java/lang/Object");
        }

        SHARED_INHERITANCE_MAP.put(className, parents);
        return parents;
    }
}
