package io.izzel.arclight.installer;

import com.google.gson.Gson;
import io.izzel.arclight.api.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ForgeInstaller {

    @SuppressWarnings("unused")
    public static Map.Entry<String, List<String>> applicationInstall() throws Throwable {
        InstallInfo installInfo;
        try (InputStream stream = ForgeInstaller.class.getResourceAsStream("/META-INF/installer.json")) {
            if (stream == null) throw new IOException("Missing META-INF/installer.json");
            installInfo = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), InstallInfo.class);
        }
        List<Supplier<Path>> suppliers = MinecraftProvider.checkMavenNoSource(installInfo.libraries);
        Path path = Paths.get("forge-" + installInfo.installer.minecraft + "-" + installInfo.installer.forge + "-shim.jar");
        var installForge = !Files.exists(path) || forgeClasspathMissing(path);
        if (!suppliers.isEmpty() || installForge) {
            System.out.println("Downloading missing libraries ...");
            ExecutorService pool = Executors.newWorkStealingPool(8);
            try {
                CompletableFuture<?>[] array = suppliers.stream().map(MinecraftProvider.reportSupply(pool, System.out::println)).toArray(CompletableFuture[]::new);
                if (installForge) {
                    var futures = installForge(installInfo, pool, System.out::println);
                    MinecraftProvider.handleFutures(System.out::println, futures);
                    System.out.println("Forge installation is starting, please wait... ");
                    try {
                        ProcessBuilder builder = new ProcessBuilder();
                        File file = new File(System.getProperty("java.home"), "bin/java");
                        builder.command(file.getCanonicalPath(), "-Djava.net.useSystemProxies=true", "-jar", futures[0].join().toString(), "--installServer", ".", "--debug");
                        builder.inheritIO();
                        Process process = builder.start();
                        if (process.waitFor() > 0) {
                            throw new Exception("Forge installation failed");
                        }
                    } catch (IOException e) {
                        try (URLClassLoader loader = new URLClassLoader(
                            new URL[]{futures[0].join().toUri().toURL()},
                            ForgeInstaller.class.getClassLoader().getParent())) {
                            Method method = loader.loadClass("net.minecraftforge.installer.SimpleInstaller").getMethod("main", String[].class);
                            method.invoke(null, (Object) new String[]{"--installServer", ".", "--debug"});
                        }
                    }
                }
                MinecraftProvider.handleFutures(System.out::println, array);
            } finally {
                pool.shutdownNow();
            }
        }
        if (!Files.isRegularFile(path) || forgeClasspathMissing(path)) {
            throw new IOException("Forge installation is incomplete: " + path);
        }
        return classpath(path, installInfo);
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<Path>[] installForge(InstallInfo info, ExecutorService pool, Consumer<String> logger) {
        var minecraftData = MinecraftProvider.downloadMinecraftData(info, pool, logger);
        String coord = String.format("net.minecraftforge:forge:%s-%s:installer", info.installer.minecraft, info.installer.forge);
        String dist = String.format("forge-%s-%s-installer.jar", info.installer.minecraft, info.installer.forge);
        var installerFuture = ForgeLikeProvider.downloadInstaller(coord, dist, info.installer.forgeHash, minecraftData, info, pool, logger);
        var serverFuture = minecraftData.thenCompose(data -> MinecraftProvider.reportSupply(pool, logger).apply(
            () -> data.downloadServer(String.format("libraries/net/minecraft/server/%1$s/server-%1$s-bundled.jar", info.installer.minecraft))
        ));
        return new CompletableFuture[]{installerFuture, serverFuture};
    }

    private static boolean forgeClasspathMissing(Path path) throws Exception {
        try (var file = new JarFile(path.toFile())) {
            return shimLibraries(file).values().stream().anyMatch(it -> !Files.isRegularFile(it));
        } catch (IOException e) {
            return true;
        }
    }

    private static Map.Entry<String, List<String>> classpath(Path path, InstallInfo installInfo) throws Throwable {
        Properties properties = new Properties();
        Map<String, Path> libraries;
        try (var file = new JarFile(path.toFile())) {
            var entry = file.getEntry("bootstrap-shim.properties");
            if (entry == null) throw new IOException("Missing bootstrap-shim.properties in " + path);
            try (var stream = file.getInputStream(entry)) {
                properties.load(stream);
            }
            libraries = shimLibraries(file);
        }
        int javaVersion = Integer.parseInt(properties.getProperty("Java-Version", "0"));
        if (Runtime.version().feature() < javaVersion) {
            throw new IllegalStateException("Forge requires Java " + javaVersion + ", current Java is " + Runtime.version());
        }
        if (!"net.minecraftforge.bootstrap.ForgeBootstrap".equals(properties.getProperty("Main-Class"))) {
            throw new IOException("Unsupported Forge bootstrap: " + properties.getProperty("Main-Class"));
        }
        var systemLibraries = List.copyOf(libraries.values());
        // The official shim owns Forge's library versions (notably ASM for Java 25).
        // Do not introduce a second module for the same artifact from Paper's graph.
        for (var lib : installInfo.libraries.keySet()) {
            libraries.putIfAbsent(libraryKey(lib), Paths.get("libraries", Util.mavenToPath(lib)));
        }
        var paths = new ArrayList<Path>();
        paths.add(Paths.get(ForgeInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        paths.addAll(libraries.values());
        for (var library : paths) {
            if (!Files.exists(library)) throw new IOException("Missing required library: " + library);
        }
        var launchArgs = new ArrayList<String>();
        var arguments = properties.getProperty("Arguments", "").trim();
        if (!arguments.isEmpty()) launchArgs.addAll(Arrays.asList(arguments.split("\\s+")));
        int target = launchArgs.indexOf("--launchTarget");
        if (target < 0 || target + 1 >= launchArgs.size()) {
            throw new IOException("Forge shim has no --launchTarget argument");
        }
        launchArgs.set(target + 1, "arclight_server");

        // Forge's shim manifest exposes its filesystem providers to the system
        // loader. FileSystemProvider.installedProviders() does not use the TCCL,
        // so a private URLClassLoader alone would lose union/jarjar/roimfs.
        // Keep additional libraries in the module launch path, not the system loader.
        for (var library : systemLibraries) addToPath(library);
        System.setProperty("java.class.path", paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator)));
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        return Map.entry("io.izzel.arclight.boot.forge.application.ApplicationBootstrap", List.copyOf(launchArgs));
    }

    private static Map<String, Path> shimLibraries(JarFile file) throws IOException {
        var entry = file.getEntry("bootstrap-shim.list");
        if (entry == null) throw new IOException("Missing bootstrap-shim.list in " + file.getName());
        var libraries = new LinkedHashMap<String, Path>();
        try (var stream = file.getInputStream(entry)) {
            for (var line : new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().toList()) {
                var fields = line.split("\t", -1);
                if (fields.length != 3 || fields[1].isEmpty() || fields[2].isEmpty()) {
                    throw new IOException("Invalid bootstrap-shim.list entry: " + line);
                }
                libraries.putIfAbsent(libraryKey(fields[1]), Paths.get("libraries", fields[2]));
            }
        }
        if (libraries.isEmpty()) throw new IOException("Empty bootstrap-shim.list in " + file.getName());
        return libraries;
    }

    private static String libraryKey(String coordinate) throws IOException {
        var artifact = coordinate.split("@", -1);
        var parts = artifact[0].split(":", -1);
        if (artifact.length > 2 || artifact.length == 2 && artifact[1].isEmpty()
            || parts.length < 3 || parts.length > 4 || Arrays.stream(parts).anyMatch(String::isEmpty)) {
            throw new IOException("Invalid Maven coordinate: " + coordinate);
        }
        return parts[0] + ':' + parts[1] + ':' + (parts.length == 4 ? parts[3] : "") + '@' + (artifact.length == 2 ? artifact[1] : "jar");
    }

    public static void addToPath(Path path) {
        try {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            Field ucpField;
            try {
                ucpField = loader.getClass().getDeclaredField("ucp");
            } catch (NoSuchFieldException e) {
                ucpField = loader.getClass().getSuperclass().getDeclaredField("ucp");
            }
            long offset = Unsafe.objectFieldOffset(ucpField);
            Object ucp = Unsafe.getObject(loader, offset);
            if (ucp == null) {
                var cl = Class.forName("jdk.internal.loader.URLClassPath");
                var handle = Unsafe.lookup().findConstructor(cl, MethodType.methodType(void.class, URL[].class));
                ucp = handle.invoke((Object) new URL[]{});
                Unsafe.putObjectVolatile(loader, offset, ucp);
            }
            Method method = ucp.getClass().getDeclaredMethod("addURL", URL.class);
            Unsafe.lookup().unreflect(method).invoke(ucp, path.toUri().toURL());
        } catch (Throwable t) {
            throw new IllegalStateException("Could not add library to the system classpath: " + path, t);
        }
    }
}
