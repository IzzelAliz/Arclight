/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.startup;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.status.StatusLogger;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

/**
 * Provides a convenient base-class for our entry point classes.
 */
public abstract class Entrypoint {
    protected Entrypoint() {}

    protected static StartupResult startup(String[] args, boolean headless, Dist dist, boolean cleanDist) {
        // Wait to log this until Log4j2 is initialized properly.
        long startupUptime = ManagementFactory.getRuntimeMXBean().getUptime();

        // In dev, do not overwrite the logging configuration if the user explicitly set another one.
        // In production, always overwrite the vanilla configuration.
        // TODO: Update this comment and coordinate with launchers to determine how to use THEIR logging config
        if (!hasCustomLoggingConfiguration()) {
            overwriteLoggingConfiguration();
        }

        // Try to avoid accessing this class before initializing Log4j2 to avoid reconfiguration
        var logger = LoggerFactory.getLogger(Entrypoint.class);

        logger.info("JVM Uptime at startup: {}ms", startupUptime);

        var startupArgs = new StartupArgs(
                getGameDir(args),
                headless,
                dist,
                cleanDist,
                args,
                new HashSet<>(),
                listClasspathEntries(),
                Thread.currentThread().getContextClassLoader());

        try {
            return new StartupResult(
                    FMLLoader.create(DevAgent.getInstrumentation(), startupArgs),
                    startupArgs);
        } catch (Exception e) {
            var sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.error("Failed to start FML: {}", sw);
            throw new FatalStartupException("Failed to start FML: " + e, startupArgs, e);
        }
    }

    private static List<File> listClasspathEntries() {
        // Find entries on the classpath
        var classPathEntries = System.getProperty("java.class.path").split(File.pathSeparator);
        var result = new ArrayList<File>(classPathEntries.length);
        for (var classPathEntry : classPathEntries) {
            var file = new File(classPathEntry);
            result.add(file);
        }

        return result;
    }

    private static Path getGameDir(String[] args) {
        var gameDir = new File(getArg(args, "gameDir", "")).getAbsoluteFile();
        if (!gameDir.isDirectory()) {
            throw new RuntimeException("The game directory passed on the command-line is not a directory: " + gameDir);
        }
        return gameDir.toPath();
    }

    private static String getArg(String[] args, String name, String defaultValue) {
        var argName = "--" + name;
        for (int i = 0; i + 1 < args.length; i++) {
            if (argName.equals(args[i])) {
                return args[i + 1];
            }
        }

        return defaultValue;
    }

    /**
     * If a user (or launcher) supplies an explicit Log4j2 configuration file, it should be used over the one
     * we ship with FML. We try to detect whether one is given using the usual Log4j2 configuration properties.
     * <p>See <a href="https://logging.apache.org/log4j/2.x/manual/systemproperties.html#log4j2.configurationFile">Log4j2 documentation</a>.
     */
    private static boolean hasCustomLoggingConfiguration() {
        return System.getProperty("log4j2.configurationFile") != null
                || System.getProperty("log4j.configurationFile") != null
                || System.getenv("LOG4J_CONFIGURATION_FILE") != null;
    }

    /**
     * Forces the log4j2 logging context to use the configuration shipped with fml_loader.
     */
    static void overwriteLoggingConfiguration() {
        // Disabling JMX for Log4j2 improves startup time
        if (System.getProperty("log4j2.disable.jmx") == null && System.getenv("LOG4J_DISABLE_JMX") == null) {
            System.setProperty("log4j2.disable.jmx", "true");
        }

        var loggingConfigUrl = Entrypoint.class.getResource("log4j2.xml");
        if (loggingConfigUrl != null) {
            URI loggingConfigUri;
            try {
                loggingConfigUri = loggingConfigUrl.toURI();
            } catch (URISyntaxException e) {
                StatusLogger.getLogger().error("Failed to read FML logging configuration: {}", loggingConfigUrl, e);
                return;
            }
            var configSource = ConfigurationSource.fromUri(loggingConfigUri);
            Configurator.reconfigure(ConfigurationFactory.getInstance().getConfiguration(LoggerContext.getContext(), configSource));
            StatusLogger.getLogger().debug("Reconfiguring logging with configuration from {}", loggingConfigUri);
        }
    }

    /**
     * The only point of this is to get a neater stacktrace in all crash reports, since this
     * will replace three levels of Java reflection with one generated lambda method.
     */
    protected static MethodHandle createMainMethodCallable(StartupResult startupResult, String mainClassName) {
        try {
            var currentClassLoader = startupResult.loader.getCurrentClassLoader();
            var mainClass = Class.forName(mainClassName, true, currentClassLoader);
            if (mainClass.getClassLoader() != currentClassLoader) {
                throw new FatalStartupException("Missing main class " + mainClassName + " from the game content loader (but available on " + mainClass.getClassLoader() + ").", startupResult.startupArgs);
            }
            var lookup = MethodHandles.publicLookup();
            var methodType = MethodType.methodType(void.class, String[].class);
            return lookup.findStatic(mainClass, "main", methodType);
        } catch (ClassNotFoundException e) {
            throw new FatalStartupException("Missing main class " + mainClassName + " on the classpath.", startupResult.startupArgs);
        } catch (NoSuchMethodException e) {
            throw new FatalStartupException(mainClassName + " is missing a static 'main' method.", startupResult.startupArgs);
        } catch (Throwable e) {
            throw new FatalStartupException("Failed to create entrypoint object.", startupResult.startupArgs, e);
        }
    }

    protected static @Nullable Thread findThread(String threadName) {
        Thread serverThread = null;
        for (var thread : Thread.getAllStackTraces().keySet()) {
            if (threadName.equals(thread.getName())) {
                // While there's no guarantee for thread ids to be monotonically increasing
                // if there's ever a conflict between threads named "Server thread" because a mod spawned one
                // with that name, we'll pick the lower.
                if (serverThread == null || thread.threadId() <= serverThread.threadId()) {
                    serverThread = thread;
                }
            }
        }
        return serverThread;
    }

    protected record StartupResult(FMLLoader loader, StartupArgs startupArgs) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            loader.close();
        }
    }
}
