/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.startup;

import java.lang.instrument.Instrumentation;

/**
 * Our javaagent which we attach to the JVM to be able to add opens/exports to modules when the receiving modules
 * aren't defined at boot time, and we can't use JVM CLI args. It's also useful for JUnit scenarios, where adding
 * JVM arguments for IDEs is very annoying.
 */
public final class DevAgent {
    private static Instrumentation instrumentation;

    private DevAgent() {}

    /**
     * Invoked only from {@link InstrumentationHelper}. Needs to be public because it makes it easier for reflection
     * to access it.
     */
    public static Instrumentation getInstrumentation() {
        var stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        var callingPackage = stackWalker.getCallerClass().getPackageName();
        if (!callingPackage.equals(DevAgent.class.getPackageName())) {
            throw new IllegalStateException("This method may only be called by FML");
        }
        return instrumentation;
    }

    /**
     * This method is called when the agent was added to the JVMs command line arguments.
     */
    public static void premain(String arguments, Instrumentation instrumentation) {
        DevAgent.instrumentation = instrumentation;
    }

    /**
     * This method is called when the agent was dynamically attached.
     */
    public static void agentmain(String arguments, Instrumentation instrumentation) {
        DevAgent.instrumentation = instrumentation;
    }
}
