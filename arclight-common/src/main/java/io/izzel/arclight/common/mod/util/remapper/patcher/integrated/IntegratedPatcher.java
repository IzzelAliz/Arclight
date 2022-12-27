package io.izzel.arclight.common.mod.util.remapper.patcher.integrated;

import io.izzel.arclight.api.PluginPatcher;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class IntegratedPatcher implements PluginPatcher {

    private static final Map<String, BiConsumer<ClassNode, ClassRepo>> SPECIFIC = new HashMap<>() {};
    private static final List<BiConsumer<ClassNode, ClassRepo>> GENERAL = new ArrayList<>();

    static {
        SPECIFIC.put("com/sk89q/worldedit/bukkit/BukkitAdapter", WorldEdit::handleBukkitAdapter);
        SPECIFIC.put("com/sk89q/worldedit/bukkit/adapter/Refraction", WorldEdit::handlePickName);
        GENERAL.add(WorldEdit::handleWatchdog);
    }

    @Override
    public void handleClass(ClassNode node, ClassRepo classRepo) {
        BiConsumer<ClassNode, ClassRepo> consumer = SPECIFIC.get(node.name);
        if (consumer != null) {
            consumer.accept(node, classRepo);
        } else {
            for (BiConsumer<ClassNode, ClassRepo> general : GENERAL) {
                general.accept(node, classRepo);
            }
        }
    }
}
