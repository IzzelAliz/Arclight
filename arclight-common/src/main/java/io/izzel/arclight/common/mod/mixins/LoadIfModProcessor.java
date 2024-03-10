package io.izzel.arclight.common.mod.mixins;

import io.izzel.arclight.common.mod.ArclightCommon;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.Objects;

public class LoadIfModProcessor {

    private static final String TYPE = Type.getDescriptor(LoadIfMod.class);

    static boolean shouldApply(ClassNode node) {
        for (var ann : node.invisibleAnnotations) {
            if (ann.desc.equals(TYPE)) {
                var loadIfModData = parse(ann);
                return switch (loadIfModData.condition()) {
                    case ABSENT -> !ArclightCommon.api().isModLoaded(loadIfModData.modid());
                    case PRESENT -> ArclightCommon.api().isModLoaded(loadIfModData.modid());
                };
            }
        }
        return true;
    }

    private static LoadIfModData parse(AnnotationNode ann) {
        LoadIfMod.ModCondition condition = null;
        String modid = null;
        for (int i = 0; i < ann.values.size(); i += 2) {
            var name = ((String) ann.values.get(i));
            var value = ann.values.get(i + 1);
            switch (name) {
                case "condition" -> {
                    var condName = ((String[]) value)[1];
                    condition = LoadIfMod.ModCondition.valueOf(condName);
                }
                case "modid" -> modid = ((String) value);
            }
        }
        return new LoadIfModData(Objects.requireNonNull(condition, "condition"),
            Objects.requireNonNull(modid, "modid"));
    }

    private record LoadIfModData(LoadIfMod.ModCondition condition, String modid) {
    }
}
