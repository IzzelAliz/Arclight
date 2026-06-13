/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.startup;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade for obtaining {@link Instrumentation} in {@link FMLLoader}.
 */
public final class InstrumentationHelper {
    private static final Logger LOG = LoggerFactory.getLogger(InstrumentationHelper.class);

    private InstrumentationHelper() {}

    /**
     * {@returns a javaagent instrumentation object if possible}
     * 
     * @throws IllegalStateException If instrumentation cannot be obtained.
     */
    public static Instrumentation obtainInstrumentation() {
        var stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        var callingPackage = stackWalker.getCallerClass().getPackageName();
        if (!callingPackage.equals(FMLLoader.class.getPackageName())) {
            throw new IllegalStateException("This method may only be called by FML");
        }

        var storedExceptions = new ArrayList<Exception>();

        // Obtain instrumentation as early as possible. We use reflection here since we want to make sure that even if
        // we are loaded through other means, we get the agent class from the system CL.
        try {
            return getFromOurOwnAgent();
        } catch (Exception e) {
            storedExceptions.add(e);
        }

        // Still don't have it? Try self-attach!
        // This most likely will go away in the next Java LTS, but until then, it's convenient for unit tests.
        try {
            var classpathItem = SelfAttach.getClassPathItem();
            var command = ProcessHandle.current().info().command().orElseThrow(() -> new RuntimeException("Could not self-attach: failed to determine our own commandline"));
            var process = new ProcessBuilder(command, "-cp", classpathItem, SelfAttach.class.getName(), DevAgent.class.getName())
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .inheritIO()
                    .start();
            process.getOutputStream().close();
            var result = process.waitFor();
            if (result != 0) {
                throw new RuntimeException("Could not self-attach agent: " + result);
            }
            return getFromOurOwnAgent();
        } catch (Exception e) {
            storedExceptions.add(e);
        }

        // If our own self-attach fails due to a user using an unexpected JVM, we support that they add ByteBuddy
        // which has a plethora of self-attach options.
        try {
            var byteBuddyAgent = Class.forName("net.bytebuddy.agent.ByteBuddyAgent", true, ClassLoader.getSystemClassLoader());
            var instrumentation = (Instrumentation) byteBuddyAgent.getMethod("install").invoke(null);
            LOG.info("Using byte-buddy fallback");
            return instrumentation;
        } catch (Exception e) {
            storedExceptions.add(e);
        }

        var e = new IllegalStateException("Failed to obtain instrumentation.");
        storedExceptions.forEach(e::addSuppressed);
        throw e;
    }

    private static Instrumentation getFromOurOwnAgent() throws Exception {
        // This code may be surprising, but the DevAgent is *always* loaded on the system classloader.
        // If we have been loaded somewhere beneath, our copy of DevAgent may not be the same. To ensure we actually
        // get the "real" agent, we specifically grab the class from the system CL.
        var devAgent = Class.forName("net.neoforged.fml.startup.DevAgent", true, ClassLoader.getSystemClassLoader());
        var instrumentation = (Instrumentation) devAgent.getMethod("getInstrumentation").invoke(null);
        if (instrumentation == null) {
            throw new IllegalStateException("Our DevAgent was not attached. Pass an appropriate -javaagent parameter.");
        }
        LOG.info("Using our own agent");
        return instrumentation;
    }
}
