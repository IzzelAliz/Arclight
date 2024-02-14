package io.izzel.arclight.common.mod.mixins;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.common.mod.mixins.annotation.OnlyInPlatform;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.List;

public class PlatformMixinProcessor {

    private static final String TYPE = Type.getDescriptor(OnlyInPlatform.class);

    @SuppressWarnings("unchecked")
    public static boolean shouldApply(String mixinClass) {
        var current = ArclightPlatform.current();
        try (var stream = PlatformMixinProcessor.class.getClassLoader().getResourceAsStream(mixinClass.replace('.', '/') + ".class")) {
            if (stream != null) {
                var bytes = stream.readAllBytes();
                var cr = new ClassReader(bytes);
                var node = new ClassNode();
                cr.accept(node, ClassReader.SKIP_CODE);
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
            } else {
                System.out.println(mixinClass);
            }
            return true;
        } catch (IOException e) {
            return true;
        }
    }
}
