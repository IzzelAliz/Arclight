package io.izzel.arclight.common.mod.util;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;

public class ClassDump {

    public static void print(ClassNode classNode) {
        classNode.accept(new TraceClassVisitor(new PrintWriter(System.out)));
    }
}
