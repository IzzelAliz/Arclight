package io.izzel.arclight.common.mod.mixins;

import io.izzel.arclight.common.mod.ArclightCommon;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Objects;

public class LoadIfModProcessor {

    private static final String TYPE = Type.getDescriptor(LoadIfMod.class);

    static boolean shouldApply(ClassNode node) {
        for (var ann : node.invisibleAnnotations) {
            if (ann.desc.equals(TYPE)) {
                var loadIfModData = parse(ann);
                return switch (loadIfModData.condition()) {
                    case ABSENT -> {
                        for (var modid : loadIfModData.modids()) {
                            if (ArclightCommon.api().isModLoaded(modid)) {
                                yield false;
                            }
                        }
                        yield true;
                    }
                    case PRESENT -> {
                        for (var modid : loadIfModData.modids()) {
                            if (ArclightCommon.api().isModLoaded(modid)) {
                                yield true;
                            }
                        }
                        yield false;
                    }
                };
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static LoadIfModData parse(AnnotationNode ann) {
        LoadIfMod.ModCondition condition = null;
        List<String> modids = null;
        for (int i = 0; i < ann.values.size(); i += 2) {
            var name = ((String) ann.values.get(i));
            var value = ann.values.get(i + 1);
            switch (name) {
                case "condition" -> {
                    var condName = ((String[]) value)[1];
                    condition = LoadIfMod.ModCondition.valueOf(condName);
                }
                case "modid" -> modids = ((List<String>) value);
            }
        }
        return new LoadIfModData(Objects.requireNonNull(condition, "condition"),
            Objects.requireNonNull(modids, "modid"));
    }

    private record LoadIfModData(LoadIfMod.ModCondition condition, List<String> modids) {
    }
}
