/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.startup;

import com.sun.tools.attach.VirtualMachine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import net.neoforged.fml.util.ClasspathResourceUtils;

/**
 * Used out-of-process to attach our agent to the current VM.
 * Please red JEP to understand that this will likely be disabled in a future LTS of Java, but at the time this
 * Javadoc is written, it still works on Java 21 and 25.
 * https://openjdk.org/jeps/451
 * <p>
 * We expect the ecosystem to allow for easier attachment of agents via the intended way (command-line) by then.
 */
public class SelfAttach {
    public static void main(String[] args) throws Exception {
        var parentProcess = ProcessHandle.current().parent().orElse(null);
        if (parentProcess == null) {
            System.err.println("No parent process to attach to");
            System.exit(1);
        }

        var ourExecutable = parentProcess.info().command().orElse(null);
        var parentExecutable = parentProcess.info().command().orElse(null);
        if (!Objects.equals(ourExecutable, parentExecutable)) {
            System.err.println("Can only self-attach if the processes match: " + ourExecutable + " != " + parentExecutable);
            System.exit(1);
        }

        // Write a jar-file to a temp-file, which only contains the manifest pointing to the desired class
        // This class already has to be on the system classloader
        var manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Agent-Class", DevAgent.class.getName());

        Path tempFile = null;
        VirtualMachine vm = null;
        try {
            tempFile = Files.createTempFile("agentattach", ".jar");
            new JarOutputStream(Files.newOutputStream(tempFile), manifest).close();

            vm = VirtualMachine.attach(String.valueOf(parentProcess.pid()));
            vm.loadAgent(tempFile.toAbsolutePath().toString());
        } finally {
            if (vm != null) {
                vm.detach();
            }
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Gets a Java class-path item that allows the JVM to load this class.
     */
    public static String getClassPathItem() {
        var relativeClassPath = SelfAttach.class.getName().replace('.', '/') + ".class";
        var classPathItem = ClasspathResourceUtils.findFileSystemRootOfFileOnClasspath(SelfAttach.class.getClassLoader(), relativeClassPath);

        if (classPathItem == null) {
            throw new IllegalStateException("Failed to find SelfAttach class file on classpath");
        }

        return classPathItem.toAbsolutePath().toString();
    }
}
