package io.izzel.arclight.common.mod.mixins;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.common.mod.mixins.annotation.OnlyInPlatform;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

public class PlatformMixinProcessor {

    private static final String TYPE = Type.getDescriptor(OnlyInPlatform.class);

    @SuppressWarnings("unchecked")
    static boolean shouldApply(ClassNode node) {
        var current = ArclightPlatform.current();
        for (var ann : node.invisibleAnnotations) {
            if (ann.desc.equals(TYPE)) {
                var list = (List<String[]>) ann.values.get(1);
                for (String[] platform : list) {
                    if (platform[1].equals(current.name())) {
                        return true;
                    }
                }
                return false;
            }
        }
        return true;
    }
}
