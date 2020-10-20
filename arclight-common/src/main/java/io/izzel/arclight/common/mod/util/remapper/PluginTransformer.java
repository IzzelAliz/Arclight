package io.izzel.arclight.common.mod.util.remapper;

import org.objectweb.asm.tree.ClassNode;

public interface PluginTransformer {

    void handleClass(ClassNode node, ClassLoaderRemapper remapper);

    default int priority() {
        return 0;
    }
}
