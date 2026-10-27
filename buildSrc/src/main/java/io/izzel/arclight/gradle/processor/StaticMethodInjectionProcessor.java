package io.izzel.arclight.gradle.processor;

import net.fabricmc.loom.api.processor.MinecraftJarProcessor;
import net.fabricmc.loom.api.processor.ProcessorContext;
import net.fabricmc.loom.api.processor.SpecContext;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;

public class StaticMethodInjectionProcessor implements MinecraftJarProcessor<StaticMethodInjectionSpec> {
    private final String name;
    private final File specification;

    @Inject
    public StaticMethodInjectionProcessor(String name, File specification) {
        this.name = name;
        this.specification = specification;
    }

    @Override public String getName() { return name; }

    @Override
    public StaticMethodInjectionSpec buildSpec(SpecContext context) {
        try {
            var spec = StaticMethodInjectionSpec.read(specification.toPath());
            return spec.methods().isEmpty() ? null : spec;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void processJar(Path jar, StaticMethodInjectionSpec spec, ProcessorContext context) throws IOException {
        if (!context.disableObfuscation()) throw new IOException("Static method injection requires official, unobfuscated Minecraft classes");
        Path temporary = Files.createTempFile(jar.toAbsolutePath().getParent(), ".arclight-static-methods-", ".jar");
        try {
            Files.copy(jar, temporary, StandardCopyOption.REPLACE_EXISTING);
            try (var zip = FileSystems.newFileSystem(temporary)) {
                var classes = new LinkedHashMap<String, ClassNode>();
                for (var method : spec.methods()) {
                    var path = zip.getPath("/" + method.owner() + ".class");
                    if (!Files.isRegularFile(path)) throw new IOException("Missing Minecraft static method target: " + method.location());
                    var node = classes.get(method.owner());
                    if (node == null) {
                        node = new ClassNode(Opcodes.ASM9);
                        new ClassReader(Files.readAllBytes(path)).accept(node, 0);
                        classes.put(method.owner(), node);
                    }
                    if ((node.access & Opcodes.ACC_INTERFACE) != 0) throw new IOException("Static method target must be a class: " + method.owner());
                    for (var existing : node.methods) {
                        if (existing.name.equals(method.name()) && existing.desc.equals(method.descriptor())) {
                            throw new IOException("Static method already exists: " + method.location());
                        }
                    }
                    var stub = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, method.name(), method.descriptor(), method.signature(), null);
                    // Compile ABI only; the registered Mixin supplies the runtime body.
                    stub.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                    stub.instructions.add(new InsnNode(Opcodes.ATHROW));
                    stub.maxStack = 1;
                    for (var argument : Type.getArgumentTypes(method.descriptor())) stub.maxLocals += argument.getSize();
                    node.methods.add(stub);
                }
                for (var node : classes.values()) {
                    var writer = new ClassWriter(0);
                    node.accept(writer);
                    Files.write(zip.getPath("/" + node.name + ".class"), writer.toByteArray());
                }
            }
            Files.move(temporary, jar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
