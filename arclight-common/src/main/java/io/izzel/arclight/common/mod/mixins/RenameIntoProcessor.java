package io.izzel.arclight.common.mod.mixins;

import io.izzel.arclight.common.mod.mixins.annotation.RenameInto;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class RenameIntoProcessor implements MixinProcessor {

    private static final String TYPE = Type.getDescriptor(RenameInto.class);

    @Override
    public void accept(String className, ClassNode classNode, IMixinInfo mixinInfo) {
        field:
        for (var field : classNode.fields) {
            if (field.invisibleAnnotations != null) {
                for (var ann : field.invisibleAnnotations) {
                    if (TYPE.equals(ann.desc)) {
                        field.name = (String) ann.values.get(1);
                        continue field;
                    }
                }
            }
        }
        method:
        for (var method : classNode.methods) {
            if (method.invisibleAnnotations != null) {
                for (var ann : method.invisibleAnnotations) {
                    if (TYPE.equals(ann.desc)) {
                        method.name = (String) ann.values.get(1);
                        continue method;
                    }
                }
            }
        }
    }
}
