package io.izzel.arclight.common.mod.mixins;

import com.google.common.collect.MultimapBuilder;
import io.izzel.arclight.common.mod.mixins.annotation.InlineField;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.throwables.MixinApplicatorException;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.stream.Collectors;

public class InlineFieldProcessor implements MixinProcessor {

    private static final String TYPE = Type.getDescriptor(InlineField.class);

    @Override
    public void accept(String className, ClassNode classNode, IMixinInfo mixinInfo) {
        var inlineFields = new HashMap<String, FieldNode>();
        for (var field : classNode.fields) {
            if (field.invisibleAnnotations != null) {
                for (var ann : field.invisibleAnnotations) {
                    if (ann.desc.equals(TYPE)) {
                        inlineFields.put(field.name + ";" + field.desc, field);
                    }
                }
            }
        }
        if (!inlineFields.isEmpty()) {
            var fieldAccess = MultimapBuilder.hashKeys().hashSetValues().<String, MethodNode>build();
            for (var method : classNode.methods) {
                for (var insn : method.instructions) {
                    if (insn instanceof FieldInsnNode fi && fi.owner.equals(classNode.name) && inlineFields.containsKey(fi.name + ";" + fi.desc)) {
                        fieldAccess.put(fi.name + ";" + fi.desc, method);
                    }
                }
            }
            for (var field : fieldAccess.keys()) {
                if (fieldAccess.get(field).size() > 1) {
                    throw new MixinApplicatorException(mixinInfo, "@InlineField field " + field + " is accessed by multiple methods: " +
                        fieldAccess.get(field).stream().map(it -> it.name + it.desc).collect(Collectors.joining(", ")));
                }
                doInline(classNode, inlineFields.get(field), fieldAccess.get(field).iterator().next());
            }
            classNode.fields.removeAll(inlineFields.values());
        }
    }

    private void doInline(ClassNode classNode, FieldNode fieldNode, MethodNode methodNode) {
        var indice = methodNode.maxLocals;
        methodNode.maxLocals += Type.getType(fieldNode.desc).getSize();
        for (var iterator = methodNode.instructions.iterator(); iterator.hasNext(); ) {
            var node = iterator.next();
            if (node instanceof FieldInsnNode fi && fi.owner.equals(classNode.name) && fi.name.equals(fieldNode.name) && fi.desc.equals(fieldNode.desc)) {
                if (Modifier.isStatic(fieldNode.access) && !Modifier.isStatic(methodNode.access)) {
                    methodNode.instructions.insertBefore(node, new InsnNode(Opcodes.POP));
                }
                var opcode = (node.getOpcode() == Opcodes.GETFIELD || node.getOpcode() == Opcodes.GETSTATIC) ? Opcodes.ILOAD : Opcodes.ISTORE;
                iterator.set(new VarInsnNode(Type.getType(fieldNode.desc).getOpcode(opcode), indice));
            }
        }
        LOGGER.debug("Inlined field " + classNode.name + " " + fieldNode.name + " " + fieldNode.desc);
    }
}
