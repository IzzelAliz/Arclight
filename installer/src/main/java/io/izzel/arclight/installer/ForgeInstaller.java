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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.jar.JarFile;

public class ForgeInstaller {

    @SuppressWarnings("unused")
    public static Map.Entry<String, List<String>> applicationInstall() throws Throwable {
        InputStream stream = ForgeInstaller.class.getResourceAsStream("/META-INF/installer.json");
        InstallInfo installInfo = new Gson().fromJson(new InputStreamReader(stream), InstallInfo.class);
        List<Supplier<Path>> suppliers = MinecraftProvider.checkMavenNoSource(installInfo.libraries);
        Path path = Paths.get("forge-" + installInfo.installer.minecraft + "-" + installInfo.installer.forge + "-shim.jar");
        var installForge = !Files.exists(path) || forgeClasspathMissing(path);
        if (!suppliers.isEmpty() || installForge) {
            System.out.println("Downloading missing libraries ...");
            ExecutorService pool = Executors.newWorkStealingPool(8);
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
                        new URL[]{new File(String.format("forge-%s-%s-installer.jar", installInfo.installer.minecraft, installInfo.installer.forge)).toURI().toURL()},
                        ForgeInstaller.class.getClassLoader().getParent())) {
                        Method method = loader.loadClass("net.minecraftforge.installer.SimpleInstaller").getMethod("main", String[].class);
                        method.invoke(null, (Object) new String[]{"--installServer", ".", "--debug"});
                    }
                }
            }
            MinecraftProvider.handleFutures(System.out::println, array);
            pool.shutdownNow();
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
            new FileDownloader(String.format(data.serverUrl(), info.installer.minecraft),
                String.format("libraries/net/minecraft/server/%1$s/server-%1$s-bundled.jar", info.installer.minecraft), data.serverHash())
        ));
        return new CompletableFuture[]{installerFuture, serverFuture};
    }

    private static boolean forgeClasspathMissing(Path path) throws Exception {
        try (var file = new JarFile(path.toFile())) {
            var entry = file.getEntry("bootstrap-shim.list");
            try (var stream = file.getInputStream(entry)) {
                return new String(stream.readAllBytes()).lines().anyMatch(it -> !Files.exists(Paths.get("libraries", it.split("\t")[2])));
            }
        }
    }

    private static Map.Entry<String, List<String>> classpath(Path path, InstallInfo installInfo) throws Throwable {
        var libs = new ArrayList<>(installInfo.libraries.keySet());
        var classpath = new StringBuilder(System.getProperty("java.class.path"));
        try (var file = new JarFile(path.toFile())) {
            var entry = file.getEntry("bootstrap-shim.list");
            try (var stream = file.getInputStream(entry)) {
                new String(stream.readAllBytes()).lines().forEach(it -> {
                    var args = it.split("\t");
                    classpath.append(File.pathSeparator).append("libraries/").append(args[2]);
                    addToPath(Paths.get("libraries", args[2]));
                });
            }
        }
        for (var lib : libs) {
            classpath.append(File.pathSeparator).append("libraries/").append(Util.mavenToPath(lib));
        }
        System.setProperty("java.class.path", classpath.toString());
        return Map.entry("io.izzel.arclight.boot.forge.application.ApplicationBootstrap", List.of("--launchTarget", "arclight_server"));
    }

    @SuppressWarnings("removal")
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
                var handle = Unsafe.lookup().findConstructor(cl, MethodType.methodType(void.class, URL[].class, AccessControlContext.class));
                ucp = handle.invoke(new URL[]{}, (AccessControlContext) null);
                Unsafe.putObjectVolatile(loader, offset, ucp);
            }
            Method method = ucp.getClass().getDeclaredMethod("addURL", URL.class);
            Unsafe.lookup().unreflect(method).invoke(ucp, path.toUri().toURL());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
