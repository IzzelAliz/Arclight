package io.izzel.arclight.gradle.tasks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.izzel.arclight.gradle.processor.StaticMethodInjectionSpec;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class VerifyStaticMethodInjections extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSpecification();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getClasses();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getMixinConfigs();

    @TaskAction
    public void verify() throws IOException {
        StaticMethodInjectionSpec spec = StaticMethodInjectionSpec.read(getSpecification().get().getAsFile().toPath());
        Set<String> activeMixins = new HashSet<>();
        for (var file : getMixinConfigs()) {
            try (var reader = Files.newBufferedReader(file.toPath())) {
                JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
                String prefix = config.get("package").getAsString() + ".";
                for (String side : List.of("mixins", "server")) {
                    if (config.has(side)) {
                        for (var entry : config.getAsJsonArray(side)) {
                            activeMixins.add((prefix + entry.getAsString()).replace('.', '/'));
                        }
                    }
                }
            }
        }
        for (var method : spec.methods()) {
            if (!activeMixins.contains(method.mixin())) {
                throw new IOException("Mixin not registered for server: " + method.mixin() + " for " + method.location());
            }
            byte[] bytes = null;
            for (var directory : getClasses()) {
                var candidate = directory.toPath().resolve(method.mixin() + ".class");
                if (Files.isRegularFile(candidate)) {
                    if (bytes != null) throw new IOException("Duplicate Mixin class: " + method.mixin());
                    bytes = Files.readAllBytes(candidate);
                }
            }
            if (bytes == null) throw new IOException("Missing compiled Mixin: " + method.mixin());
            ClassNode node = new ClassNode(Opcodes.ASM9);
            new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            Set<String> targets = new HashSet<>();
            for (AnnotationNode annotation : annotations(node.visibleAnnotations, node.invisibleAnnotations)) {
                if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) continue;
                for (int i = 0; annotation.values != null && i < annotation.values.size(); i += 2) {
                    String key = (String) annotation.values.get(i);
                    Object value = annotation.values.get(i + 1);
                    if (key.equals("value")) {
                        for (Object type : (List<?>) value) targets.add(((Type) type).getInternalName());
                    } else if (key.equals("targets")) {
                        for (Object target : (List<?>) value) targets.add(((String) target).replace('.', '/'));
                    }
                }
            }
            if (!targets.contains(method.owner())) {
                throw new IOException("Mixin target mismatch: " + method.mixin() + " for " + method.location());
            }
            List<MethodNode> matches = node.methods.stream().filter(f -> f.name.equals(method.name()) && f.desc.equals(method.descriptor())).toList();
            if (matches.size() != 1) throw new IOException("Expected one Mixin method: " + method.location());
            MethodNode actual = matches.getFirst();
            if (method.signature() != null && !method.signature().equals(actual.signature)) {
                throw new IOException("Mixin generic signature mismatch: " + method.location());
            }
            int effectiveAccess = actual.access;
            for (AnnotationNode annotation : annotations(actual.visibleAnnotations, actual.invisibleAnnotations)) {
                if (annotation.desc.equals("Lio/izzel/arclight/common/mod/mixins/annotation/TransformAccess;")) {
                    effectiveAccess = (Integer) annotation.values.get(1);
                }
            }
            if ((actual.access & Opcodes.ACC_STATIC) == 0 || !actual.desc.equals(method.descriptor())
                || effectiveAccess != (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC) || actual.instructions.size() == 0) {
                throw new IOException("Mixin method descriptor/access mismatch: " + method.location());
            }
            for (AnnotationNode annotation : annotations(actual.visibleAnnotations, actual.invisibleAnnotations)) {
                if (annotation.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;")
                    || annotation.desc.equals("Lorg/spongepowered/asm/mixin/Unique;")
                    || annotation.desc.equals("Lorg/spongepowered/asm/mixin/Overwrite;")) {
                    throw new IOException("Cannot expose Shadow/Unique method: " + method.location());
                }
            }
            getLogger().lifecycle("Verified injected method {} from {}", method.location(), method.mixin());
        }
    }

    private static List<AnnotationNode> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        List<AnnotationNode> result = new ArrayList<>();
        if (visible != null) result.addAll(visible);
        if (invisible != null) result.addAll(invisible);
        return result;
    }
}
