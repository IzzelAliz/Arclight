package io.izzel.arclight.common.mod.util.remapper;

import com.google.common.collect.Maps;
import io.izzel.arclight.common.mod.ArclightMod;
import net.md_5.specialsource.provider.InheritanceProvider;
import net.md_5.specialsource.repo.ClassRepo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ArclightInterfaceInvokerGen implements PluginTransformer {

    public static final ArclightInterfaceInvokerGen INSTANCE = new ArclightInterfaceInvokerGen();
    private static final String PREFIX = "net/minecraft/";

    @Override
    public void handleClass(ClassNode node, ClassLoaderRemapper remapper) {
        generate(node, remapper, GlobalClassRepo.inheritanceProvider());
    }

    private static void generate(ClassNode classNode, ClassLoaderRemapper remapper, InheritanceProvider inheritanceProvider) {
        if (shouldGenerate(classNode.name, inheritanceProvider)) {
            HashSet<Map.Entry<String, String>> set = new HashSet<>();
            interfaceMethods(classNode.name, set, GlobalClassRepo.INSTANCE);
            for (Map.Entry<String, String> entry : set) {
                String name = entry.getKey();
                String desc = entry.getValue();
                MethodInsnNode node = remapper.mapMethod(classNode.name, name, desc);
                desc = remapper.mapMethodDesc(desc);
                if (node != null && !node.name.equals(name)) {
                    boolean extend = false;
                    for (MethodNode methodNode : classNode.methods) {
                        if (methodNode.name.equals(name) && methodNode.desc.equals(desc)) {
                            extend = true;
                            break;
                        }
                    }
                    if (!extend) {
                        MethodNode methodNode = generateSynthetic(name, desc, node, remapper);
                        classNode.methods.add(methodNode);
                        ArclightMod.LOGGER.debug("Generated {} redirecting to {}", classNode.name + "/" + name + " " + desc, node.owner + "/" + node.name + " " + node.desc);
                    }
                }
            }
        }
    }

    private static MethodNode generateSynthetic(String name, String desc, MethodInsnNode node, ClassLoaderRemapper remapper) {
        name = remapper.mapType(name);
        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
        methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        Type[] types = Type.getArgumentTypes(desc);
        int offset = 1;
        for (Type type : types) {
            methodNode.instructions.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), offset));
            offset += type.getSize();
        }
        methodNode.instructions.add(node);
        Type returnType = Type.getReturnType(desc);
        methodNode.instructions.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
        return methodNode;
    }

    private static boolean shouldGenerate(String internalName, InheritanceProvider provider) {
        if (internalName == null) return false;
        if (internalName.startsWith(PREFIX)) return true;
        for (String parent : provider.getParents(internalName)) {
            if (shouldGenerate(parent, provider)) return true;
        }
        return false;
    }

    private static void interfaceMethods(String internalName, Set<Map.Entry<String, String>> set, ClassRepo classRepo) {
        if (internalName == null || internalName.equals("java/lang/Object") || internalName.startsWith(PREFIX)) return;
        ClassNode classNode = classRepo.findClass(internalName);
        if (classNode == null) return;
        interfaceMethods(classNode.superName, set, classRepo);
        if (classNode.interfaces != null && classNode.interfaces.size() > 0) {
            for (String intf : classNode.interfaces) {
                interfaceMethods(intf, set, classRepo);
            }
        }
        for (MethodNode methodNode : classNode.methods) {
            set.add(Maps.immutableEntry(methodNode.name, methodNode.desc));
        }
    }
}
