package io.izzel.arclight.gradle.processor;

import net.fabricmc.loom.api.processor.MinecraftJarProcessor;
import net.fabricmc.loom.api.processor.ProcessorContext;
import net.fabricmc.loom.api.processor.SpecContext;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipFile;

public abstract class FieldInjectionProcessor implements MinecraftJarProcessor<FieldInjectionSpec> {
    private final String name;
    private final File specification;
    private final Project project;

    @Inject
    public FieldInjectionProcessor(String name, File specification, Project project) {
        this.name = name;
        this.specification = specification;
        this.project = project;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public FieldInjectionSpec buildSpec(SpecContext context) {
        try {
            FieldInjectionSpec spec = FieldInjectionSpec.read(specification.toPath());
            return spec.fields().isEmpty() ? null : spec;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void processJar(Path jar, FieldInjectionSpec spec, ProcessorContext context) throws IOException {
        if (!context.disableObfuscation()) {
            throw new IOException("Arclight field injection requires official, unobfuscated Minecraft classes");
        }
        Path temporary = Files.createTempFile(jar.toAbsolutePath().getParent(), ".arclight-fields-", ".jar");
        try {
            Files.copy(jar, temporary, StandardCopyOption.REPLACE_EXISTING);
            try (var zip = FileSystems.newFileSystem(temporary)) {
                Map<String, ClassNode> classes = new HashMap<>();
                Map<String, ClassNode> modified = new LinkedHashMap<>();
                for (FieldInjectionSpec.Field field : spec.fields()) {
                    if (!Files.isRegularFile(zip.getPath("/" + field.owner() + ".class"))) {
                        throw new IOException("Missing Minecraft field target: " + field.location());
                    }
                    ClassNode owner = readClass(zip.getPath("/"), field.owner(), classes);
                    if ((owner.access & Opcodes.ACC_INTERFACE) != 0) {
                        throw new IOException("Cannot inject instance field into interface: " + field.location());
                    }
                    ClassNode current = owner;
                    var visited = new HashSet<String>();
                    while (current != null) {
                        if (!visited.add(current.name)) throw new IOException("Cyclic hierarchy for " + field.location());
                        for (FieldNode existing : current.fields) {
                            if (current != owner && (existing.access & Opcodes.ACC_PRIVATE) != 0) continue;
                            if (existing.name.equals(field.name()) && (current != owner
                                || !existing.desc.equals(field.descriptor())
                                || (existing.access & FieldInjectionSpec.ACCESS_MASK) != field.access()
                                || !java.util.Objects.equals(existing.signature, field.signature()))) {
                                throw new IOException("Conflicting field " + current.name + "." + existing.name
                                    + ":" + existing.desc + " while injecting " + field.location());
                            }
                        }
                        if (current.superName == null || current.superName.equals("java/lang/Object")) break;
                        current = readClass(zip.getPath("/"), current.superName, classes);
                    }
                    if (owner.fields.stream().noneMatch(f -> f.name.equals(field.name()))) {
                        owner.fields.add(new FieldNode(field.access(), field.name(), field.descriptor(), field.signature(), null));
                        modified.put(owner.name, owner);
                    }
                }
                for (ClassNode node : modified.values()) {
                    ClassWriter writer = new ClassWriter(0);
                    node.accept(writer);
                    Files.write(zip.getPath("/" + node.name + ".class"), writer.toByteArray());
                }
            }
            Files.move(temporary, jar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (RuntimeException e) {
            throw new IOException("Failed field injection into " + jar + ": " + e.getMessage(), e);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private byte[] readLoaderClass(String name) throws IOException {
        for (String configuration : new String[]{"minecraftLibraries", "minecraftClientLibraries", "minecraftServerLibraries", "forgeUniversal", "forgeDependencies"}) {
            var dependency = project.getConfigurations().findByName(configuration);
            if (dependency == null || !dependency.isCanBeResolved()) continue;
            for (File file : dependency.resolve()) {
                if (!file.getName().endsWith(".jar")) continue;
                try (ZipFile zip = new ZipFile(file)) {
                    var entry = zip.getEntry(name + ".class");
                    if (entry != null) {
                        try (var input = zip.getInputStream(entry)) {
                            return input.readAllBytes();
                        }
                    }
                }
            }
        }
        return null;
    }

    private ClassNode readClass(Path root, String name, Map<String, ClassNode> classes) throws IOException {
        ClassNode cached = classes.get(name);
        if (cached != null) return cached;
        Path path = root.resolve(name + ".class");
        byte[] bytes = Files.isRegularFile(path) ? Files.readAllBytes(path) : readLoaderClass(name);
        if (bytes == null) throw new IOException("Missing field injection target or superclass: " + name);
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(bytes).accept(node, 0);
        classes.put(name, node);
        return node;
    }
}
