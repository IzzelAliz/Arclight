// todo 这玩意儿先不用，但是说不定以后有用呢

function redirectStaticFieldToMethod(classNode, source, target, type) {
    var Opcodes = Java.type("org.objectweb.asm.Opcodes");
    var FieldInsnNode = Java.type("org.objectweb.asm.tree.FieldInsnNode");
    var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");
    var idx = source.lastIndexOf('.');
    var owner = source.substring(0, idx);
    var name = source.substring(idx + 1);
    var desc = "L" + type.replace('.', '/') + ";";
    var fieldInsn = new FieldInsnNode(Opcodes.GETSTATIC, owner, name, desc);
    var index = target.lastIndexOf('.');
    var targetClass = target.substring(0, index).replace('.', '/');
    var targetMethod = target.substring(index + 1);
    var methodInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, targetClass, targetMethod, "()" + fieldInsn.desc);
    classNode.methods.forEach(function (methodNode) {
        var iter = methodNode.instructions.iterator();
        while (iter.hasNext()) {
            var insnNode = iter.next();
            if (insnNode.opcode == fieldInsn.opcode && insnNode.name == fieldInsn.name
                && insnNode.desc == fieldInsn.desc && insnNode.owner == fieldInsn.owner) {
                iter.remove();
                iter.add(methodInsn);
                print("replaced");
            }
        }
    });
}

function initializeCoreMod() {
    return {}
}