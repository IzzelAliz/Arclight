package io.izzel.arclight.common.mod.mixins;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

public class ShouldApplyProcessor {

    private static final List<Predicate<ClassNode>> PREDICATES = List.of(
        PlatformMixinProcessor::shouldApply,
        LoadIfModProcessor::shouldApply
    );

    public static boolean shouldApply(String mixinClass) {
        try (var stream = PlatformMixinProcessor.class.getClassLoader().getResourceAsStream(mixinClass.replace('.', '/') + ".class")) {
            if (stream != null) {
                var bytes = stream.readAllBytes();
                var cr = new ClassReader(bytes);
                var node = new ClassNode();
                cr.accept(node, ClassReader.SKIP_CODE);
                for (var predicate : PREDICATES) {
                    if (!predicate.test(node)) {
                        return false;
                    }
                }
                return true;
            } else {
                System.out.println(mixinClass);
            }
            return true;
        } catch (IOException e) {
            return true;
        }
    }
}
