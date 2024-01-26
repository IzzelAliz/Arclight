package io.izzel.arclight.common.mod.mixins;

import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CreateConstructorProcessor implements MixinProcessor {

    private static final String SHADOW = Type.getDescriptor(ShadowConstructor.class);
    private static final String SUPER = Type.getDescriptor(ShadowConstructor.Super.class);
    private static final String CREATE = Type.getDescriptor(CreateConstructor.class);
    private static final String MERGED = Type.getDescriptor(CreateConstructor.Merged.class);

    @Override
    public void accept(String className, ClassNode classNode, IMixinInfo mixinInfo) {
        var shadow = new HashSet<String>();
        var superCall = new HashSet<String>();
        var create = new ArrayList<MethodNode>();
        for (var iterator = classNode.methods.iterator(); iterator.hasNext(); ) {
            var method = iterator.next();
            if (!Modifier.isStatic(method.access) && method.invisibleAnnotations != null) {
                for (var ann : method.invisibleAnnotations) {
                    if (SHADOW.equals(ann.desc)) {
                        shadow.add(method.name + method.desc);
                        iterator.remove();
                        break;
                    } else if (CREATE.equals(ann.desc)) {
                        create.add(method);
                        break;
                    } else if (SUPER.equals(ann.desc)) {
                        superCall.add(method.name + method.desc);
                        iterator.remove();
                        break;
                    }
                }
            }
        }
        if (!create.isEmpty()) {
            var present = new HashSet<String>();
            for (var method : classNode.methods) {
                if (method.name.equals("<init>")) {
                    present.add(method.desc);
                }
            }
            var invalid = shadow.stream().filter(it -> present.stream().noneMatch(it::endsWith)).toList();
            if (!invalid.isEmpty()) {
                throw new IllegalArgumentException("@ShadowConstructor refers to missing constructor. Class " + className + ", desc: " + String.join(", ", invalid));
            }
            var duplicate = create.stream().filter(it -> present.contains(it.desc)).map(it -> it.name + it.desc).toList();
            if (!duplicate.isEmpty()) {
                throw new IllegalArgumentException("@CreateConstructor refers to present constructor. Class " + className + ", desc: " + String.join(", ", duplicate));
            }
            for (var method : create) {
                remapCtor(classNode, method, shadow, superCall);
            }
        }
    }

    private void remapCtor(ClassNode classNode, MethodNode methodNode, Set<String> shadow, Set<String> superCall) {
        boolean initialized = false;
        for (AbstractInsnNode node : methodNode.instructions) {
            if (node instanceof MethodInsnNode methodInsnNode) {
                var sig = methodInsnNode.name + methodInsnNode.desc;
                if (shadow.contains(sig)) {
                    if (initialized) {
                        throw new ClassFormatError("Duplicate constructor call");
                    } else {
                        methodInsnNode.setOpcode(Opcodes.INVOKESPECIAL);
                        methodInsnNode.name = "<init>";
                        initialized = true;
                    }
                }
                if (superCall.contains(sig)) {
                    if (initialized) {
                        throw new ClassFormatError("Duplicate constructor call");
                    } else {
                        methodInsnNode.setOpcode(Opcodes.INVOKESPECIAL);
                        methodInsnNode.owner = classNode.superName;
                        methodInsnNode.name = "<init>";
                        initialized = true;
                    }
                }
            }
        }
        if (!initialized) {
            if (classNode.superName.equals("java/lang/Object")) {
                InsnList insnList = new InsnList();
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
                methodNode.instructions.insert(insnList);
            } else {
                throw new ClassFormatError("No super constructor call present: " + classNode.name);
            }
        }
        for (var ann : methodNode.invisibleAnnotations) {
            if (ann.desc.equals(CREATE)) {
                ann.desc = MERGED;
            }
        }
        methodNode.name = "<init>";
    }
}
