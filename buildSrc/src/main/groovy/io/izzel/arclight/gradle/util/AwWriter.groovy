package io.izzel.arclight.gradle.util

import net.fabricmc.loom.configuration.providers.minecraft.mapped.NamedMinecraftProvider
import net.md_5.specialsource.Jar
import org.cadixdev.at.AccessChange
import org.cadixdev.at.AccessTransform
import org.cadixdev.at.AccessTransformSet
import org.cadixdev.at.ModifierChange
import org.cadixdev.bombe.type.signature.MethodSignature

class AwWriter {

    private final BufferedWriter writer
    private final NamedMinecraftProvider<?> mc
    private final Jar jar

    AwWriter(BufferedWriter writer, NamedMinecraftProvider<?> mc) {
        this.writer = writer
        this.mc = mc
        this.jar = Jar.init(mc.minecraftJarPaths.get(0).toFile())
    }

    void write(AccessTransformSet set) throws IOException {
        this.writer.writeLine("accessWidener v2 named")
        set.getClasses().forEach((originalClassName, classSet) -> {
            def className = originalClassName.replace('/', '.')
            writeClass(className, classSet.get())
            writeField(className, null, classSet.allFields())
            classSet.getFields().forEach((name, transform) -> writeField(className, name, transform))
            writeMethod(className, null, classSet.allMethods())
            classSet.getMethods().forEach((name, transform) -> writeMethod(className, name, transform))
        })
    }

    private void writeAccessTransform(String remaining, boolean field, AccessTransform transform) throws IOException {
        if (transform.access == AccessChange.PUBLIC) {
            this.writer.write('accessible ')
            this.writer.writeLine(remaining)
        }
        if ((transform.access == AccessChange.PROTECTED || transform.final == ModifierChange.REMOVE) && !field) {
            this.writer.write('extendable ')
            this.writer.writeLine(remaining)
        }
        if (transform.final == ModifierChange.REMOVE && field) {
            this.writer.write('mutable ')
            this.writer.writeLine(remaining)
        }
    }

    private void writeClass(String className, AccessTransform transform) throws IOException {
        if (transform.isEmpty()) {
            return
        }

        writeAccessTransform("class " + className.replace('.', '/'), false, transform)
    }

    private void writeField(String className, String name, AccessTransform transform) throws IOException {
        if (transform.isEmpty()) {
            return
        }
        def classNode = jar.getNode(className.replace('.', '/'))
        if (!classNode) {
            println("Not found class " + className)
            this.writer.writeLine("# TODO field " + className.replace('.', '/') + " " + name)
            return
        }
        def field = classNode.fields.find { it.name == name }
        if (!field) {
            println("Not found field " + className + " " + name)
            this.writer.writeLine("# TODO field " + className.replace('.', '/') + " " + name)
            return
        }

        writeAccessTransform("field " + classNode.name + " " + field.name + " " + field.desc, true, transform)
    }

    private void writeMethod(String className, MethodSignature signature, AccessTransform transform) throws IOException {
        if (transform.isEmpty()) {
            return
        }

        writeAccessTransform("method " + className.replace('.', '/') + " " + signature.name + " " + signature.descriptor.toString(),
                false, transform)
    }


}
