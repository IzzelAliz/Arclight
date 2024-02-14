package io.izzel.arclight.common.mod.mixins;

import io.izzel.arclight.common.mod.mixins.annotation.InlineMethod;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.util.Locals;

import java.lang.reflect.Modifier;

public class InlineMethodProcessor implements MixinProcessor {

    private static final String TYPE = Type.getDescriptor(InlineMethod.class);
    private static final String MERGED = Type.getDescriptor(InlineMethod.Merged.class);

    @Override
    public void accept(String className, ClassNode classNode, IMixinInfo mixinInfo) {
        for (var iterator = classNode.methods.iterator(); iterator.hasNext(); ) {
            var methodNode = iterator.next();
            if (methodNode.invisibleAnnotations != null) {
                for (var ann : methodNode.invisibleAnnotations) {
                    if (ann.desc.equals(TYPE)) {
                        new MethodInliner(methodNode, classNode).accept();
                        ann.desc = MERGED;
                        iterator.remove();
                    }
                }
            }
        }
    }

    private record MethodInliner(MethodNode target, ClassNode callerClass) {

        public void accept() {
            for (var node : callerClass.methods) {
                this.accept(node);
            }
        }

        public void accept(MethodNode node) {
            for (var iterator = node.instructions.iterator(); iterator.hasNext(); ) {
                var insn = iterator.next();
                if (insn instanceof MethodInsnNode method) {
                    if (method.owner.equals(callerClass.name) && method.name.equals(target.name) && method.desc.equals(target.desc)) {
                        var buf = new MethodNode(Opcodes.ASM9, node.access, node.name, node.desc, node.signature, node.exceptions.toArray(String[]::new));
                        target.accept(new RemappingVisitor(Opcodes.ASM9, buf, Locals.getLocalsAt(callerClass, node, insn, Locals.Settings.DEFAULT)));
                        node.instructions.insertBefore(insn, buf.instructions);
                        node.tryCatchBlocks.addAll(buf.tryCatchBlocks);
                        node.localVariables.addAll(buf.localVariables);
                        node.maxLocals = Math.max(node.maxLocals, buf.maxLocals);
                        node.maxStack = Math.max(node.maxStack, buf.maxStack);
                        iterator.remove();
                    }
                }
            }
        }

        protected class RemappingVisitor extends MethodVisitor {

            private final Label end = new Label();
            private final Type returnType = Type.getReturnType(target.desc);

            private final LocalVariableNode[] locals;
            private final int localOffset, localCount;

            protected RemappingVisitor(int api, MethodVisitor mv, LocalVariableNode[] locals) {
                super(api, mv);
                this.locals = locals;
                var offset = 0;
                var count = 0;
                for (int i = 0; i < locals.length; i++) {
                    var local = locals[i];
                    if (local != null) {
                        offset = Math.max(offset, local.index + Type.getType(local.desc).getSize());
                        count = i + 1;
                    }
                }
                this.localOffset = offset;
                this.localCount = count;
                this.visitStart();
            }

            protected void visitStart() {
                var offset = Modifier.isStatic(target.access) ? 0 : 1;
                var args = Type.getArgumentTypes(target.desc);
                for (var i = args.length - 1; i >= 0; i--) {
                    super.visitVarInsn(args[i].getOpcode(Opcodes.ISTORE), localOffset + i + offset);
                }
                if (offset > 0) {
                    super.visitVarInsn(Opcodes.ASTORE, localOffset);
                }
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if (owner.equals(callerClass.name) && name.equals(target.name) && descriptor.equals(target.desc)) {
                    throw new IllegalStateException("Inlining recursive method");
                }
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }

            @Override
            public void visitVarInsn(int opcode, int varIndex) {
                super.visitVarInsn(opcode, localOffset + varIndex);
            }

            @Override
            public void visitIincInsn(int varIndex, int increment) {
                super.visitIincInsn(localOffset + varIndex, increment);
            }

            @Override
            public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                super.visitLocalVariable(name, descriptor, signature, start, end, localOffset + index);
            }

            @Override
            public void visitMaxs(int maxStack, int maxLocals) {
                super.visitMaxs(maxStack, localOffset + maxLocals);
            }

            @Override
            public void visitInsn(int opcode) {
                if (opcode == returnType.getOpcode(Opcodes.IRETURN)) {
                    super.visitJumpInsn(Opcodes.GOTO, this.end);
                } else {
                    super.visitInsn(opcode);
                }
            }

            @Override
            public void visitEnd() {
                super.visitLabel(this.end);
                Object[] locals = new Object[localCount];
                for (int i = 0; i < localCount; i++) {
                    var local = this.locals[i];
                    if (local == null) {
                        locals[i] = null;
                    } else {
                        var type = Type.getType(local.desc);
                        Object v = switch (type.getSort()) {
                            case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> Opcodes.INTEGER;
                            case Type.FLOAT -> Opcodes.FLOAT;
                            case Type.LONG -> Opcodes.LONG;
                            case Type.DOUBLE -> Opcodes.DOUBLE;
                            case Type.ARRAY, Type.OBJECT -> type.getInternalName();
                            default -> Opcodes.TOP;
                        };
                        locals[i] = v;
                    }
                }
                super.visitFrame(Opcodes.F_FULL, localCount, locals, 0, null);
                super.visitEnd();
            }
        }
    }
}
