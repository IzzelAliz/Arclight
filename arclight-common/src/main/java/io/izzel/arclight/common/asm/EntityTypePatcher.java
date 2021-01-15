package io.izzel.arclight.common.asm;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import io.izzel.arclight.api.ArclightVersion;
import org.objectweb.asm.tree.ClassNode;

public class EntityTypePatcher implements Implementer {

    public static final EntityTypePatcher INSTANCE = new EntityTypePatcher();

    private final String entityPackage = "org/bukkit/craftbukkit/" + ArclightVersion.current().packageName() + "/entity";

    @Override
    public boolean processClass(ClassNode node, ILaunchPluginService.ITransformerLoader transformerLoader) {
        if (node.name.startsWith(entityPackage) && !node.name.endsWith("CraftEntity")) {
            return node.methods.removeIf(methodNode -> methodNode.name.equals("getType")
                && methodNode.desc.equals("()Lorg/bukkit/entity/EntityType;"));
        }
        return false;
    }
}
