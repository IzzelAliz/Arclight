package io.izzel.arclight.installer;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FabricInstaller {

    public static Map.Entry<String, List<Path>> applicationInstall() throws Exception {
        InputStream stream = ForgeInstaller.class.getResourceAsStream("/META-INF/installer.json");
        InstallInfo installInfo = new Gson().fromJson(new InputStreamReader(stream), InstallInfo.class);
        var deps = new HashMap<>(installInfo.libraries);
        deps.putAll(installInfo.fabricExtra);
        List<Supplier<Path>> suppliers = MinecraftProvider.checkMavenNoSource(deps);
        Path path = Paths.get("libraries/net/fabricmc/fabric-loader", installInfo.installer.fabricLoader, "fabric-loader-" + installInfo.installer.fabricLoader + ".jar");
        var installFabric = !Files.exists(path) || fabricClasspathMissing(path);
        if (!suppliers.isEmpty() || installFabric) {
            System.out.println("Downloading missing libraries ...");
            ExecutorService pool = Executors.newWorkStealingPool(8);
            CompletableFuture<?>[] array = suppliers.stream().map(MinecraftProvider.reportSupply(pool, System.out::println)).toArray(CompletableFuture[]::new);
            if (installFabric) {
                var futures = installFabric(installInfo, pool, System.out::println);
                array = Stream.concat(Arrays.stream(futures), Arrays.stream(array)).toArray(CompletableFuture[]::new);
            }
            MinecraftProvider.handleFutures(System.out::println, array);
            pool.shutdownNow();
        }
        return classpath(installInfo, path);
    }

    private static Map.Entry<String, List<Path>> classpath(InstallInfo info, Path path) throws Exception {
        var mcPath = String.format("libraries/net/minecraft/server/%1$s/server-%1$s.jar", info.installer.minecraft);
        System.setProperty("fabric.gameJarPath", Paths.get(mcPath).toAbsolutePath().toString());
        var deps = new HashMap<>(info.libraries);
        deps.putAll(info.fabricExtra);
        System.setProperty("arclight.fabric.classpath", deps.keySet().stream().map(it ->
                "libraries/" + Util.mavenToPath(it)).collect(Collectors.joining(File.pathSeparator)));
        var libs = new ArrayList<Path>();
        fabricDeps(path).keySet().stream().map(it-> Paths.get("libraries", Util.mavenToPath(it))).forEach(libs::add);
        libs.add(path);
        try (var file = new JarFile(path.toFile())) {
            var mainClass = file.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            return Map.entry(mainClass, libs);
        }
    }

    private static boolean fabricClasspathMissing(Path fabricLoader) throws Exception {
        return fabricDeps(fabricLoader).keySet().stream().anyMatch(it -> !Files.exists(Paths.get("libraries", Util.mavenToPath(it))));
    }

    private static Map<String, Map.Entry<String, String>> fabricDeps(Path fabricLoader) throws Exception {
        var ret = new HashMap<String, Map.Entry<String, String>>();
        try (var file = new JarFile(fabricLoader.toFile())) {
            var entry = file.getEntry("fabric-installer.json");
            try (var stream = file.getInputStream(entry)) {
                var libraries = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject().getAsJsonObject("libraries");
                for (var block : List.of(libraries.getAsJsonArray("common"), libraries.getAsJsonArray("server"))) {
                    for (var element : block) {
                        var obj = element.getAsJsonObject();
                        var name = obj.get("name").getAsString();
                        var url = obj.get("url").getAsString() + Util.mavenToPath(name);
                        var hash = obj.get("sha1").getAsString();
                        ret.put(name, new AbstractMap.SimpleImmutableEntry<>(hash, url));
                    }
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<Path>[] installFabric(InstallInfo info, ExecutorService pool, Consumer<String> logger) {
        var minecraftData = MinecraftProvider.downloadMinecraftData(info, pool, logger);
        String coord = String.format("net.fabricmc:fabric-loader:%s", info.installer.fabricLoader);
        String dist = "libraries/" + Util.mavenToPath(coord);
        var installerFuture = MinecraftProvider.reportSupply(pool, logger).apply(new MavenDownloader(Mirrors.getMavenRepo(), coord, dist, info.installer.fabricLoaderHash))
                .thenAccept(path -> {
                    try {
                        var deps = fabricDeps(path);
                        var suppliers = MinecraftProvider.checkMaven(deps);
                        var array = suppliers.stream().map(MinecraftProvider.reportSupply(pool, System.out::println)).toArray(CompletableFuture[]::new);
                        MinecraftProvider.handleFutures(logger, array);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        var serverFuture = minecraftData.thenCompose(data -> MinecraftProvider.reportSupply(pool, logger).apply(
                new FileDownloader(String.format(data.serverUrl(), info.installer.minecraft),
                        String.format("libraries/net/minecraft/server/%1$s/server-%1$s.jar", info.installer.minecraft), data.serverHash())
        ));
        return new CompletableFuture[]{installerFuture, serverFuture};
    }
}