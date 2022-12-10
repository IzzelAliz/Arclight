package io.izzel.arclight.boot.asm;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.lang.reflect.Modifier;
import java.util.Set;

public class EnumDefinalizer implements Implementer {

    static final Set<String> ENUM = Set.of(
        "org/bukkit/Material",
        "org/bukkit/potion/PotionType",
        "org/bukkit/entity/EntityType",
        "org/bukkit/entity/Villager$Profession",
        "org/bukkit/block/Biome",
        "org/bukkit/Art",
        "org/bukkit/Statistic",
        "org/bukkit/inventory/CreativeCategory",
        "org/bukkit/entity/SpawnCategory",
        "org/bukkit/entity/EnderDragon$Phase",
        "org/bukkit/inventory/recipe/CookingBookCategory"
    );

    @Override
    public boolean processClass(ClassNode node, ILaunchPluginService.ITransformerLoader transformerLoader) {
        if (ENUM.contains(node.name)) {
            var find = false;
            for (FieldNode field : node.fields) {
                if (Modifier.isStatic(field.access) && Modifier.isFinal(field.access)
                    && field.name.equals("ENUM$VALUES")) {
                    field.access &= ~Opcodes.ACC_FINAL;
                    ArclightImplementer.LOGGER.debug("Definalize enum class {} values field {}", node.name, field.name);
                    if (find) {
                        throw new IllegalStateException("Duplicate static final field found for " + node.name + ": " + field.name);
                    } else {
                        find = true;
                    }
                }
            }
            if (!find) {
                throw new IllegalStateException("No static final field found for " + node.name);
            }
            return true;
        }
        return false;
    }
}
