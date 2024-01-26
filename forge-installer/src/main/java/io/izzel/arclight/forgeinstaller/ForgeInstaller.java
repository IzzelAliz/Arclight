package io.izzel.arclight.forgeinstaller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.izzel.arclight.api.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.AccessControlContext;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class ForgeInstaller {

    public static List<Path> modInstall(Consumer<String> logger) throws Throwable {
        InputStream stream = ForgeInstaller.class.getModule().getResourceAsStream("/META-INF/installer.json");
        InstallInfo installInfo = new Gson().fromJson(new InputStreamReader(stream), InstallInfo.class);
        List<Supplier<Path>> suppliers = checkMavenNoSource(installInfo.libraries);
        if (!suppliers.isEmpty()) {
            logger.accept("Downloading missing libraries ...");
            ExecutorService pool = Executors.newFixedThreadPool(8);
            CompletableFuture<?>[] array = suppliers.stream().map(reportSupply(pool, logger)).toArray(CompletableFuture[]::new);
            handleFutures(logger, array);
            pool.shutdownNow();
        }
        return installInfo.libraries.keySet().stream().map(it -> Paths.get("libraries").resolve(Util.mavenToPath(it))).collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    public static Map.Entry<String, List<String>> applicationInstall() throws Throwable {
        InputStream stream = ForgeInstaller.class.getResourceAsStream("/META-INF/installer.json");
        InstallInfo installInfo = new Gson().fromJson(new InputStreamReader(stream), InstallInfo.class);
        List<Supplier<Path>> suppliers = checkMavenNoSource(installInfo.libraries);
        Path path = Paths.get("forge-" + installInfo.installer.minecraft + "-" + installInfo.installer.forge + "-shim.jar");
        var installForge = !Files.exists(path) || forgeClasspathMissing(path);
        if (!suppliers.isEmpty() || installForge) {
            System.out.println("Downloading missing libraries ...");
            ExecutorService pool = Executors.newWorkStealingPool(8);
            CompletableFuture<?>[] array = suppliers.stream().map(reportSupply(pool, System.out::println)).toArray(CompletableFuture[]::new);
            if (installForge) {
                var futures = installForge(installInfo, pool, System.out::println);
                handleFutures(System.out::println, futures);
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
            handleFutures(System.out::println, array);
            pool.shutdownNow();
        }
        return classpath(path, installInfo);
    }

    private static Function<Supplier<Path>, CompletableFuture<Path>> reportSupply(ExecutorService service, Consumer<String> logger) {
        return it -> CompletableFuture.supplyAsync(it, service).thenApply(path -> {
            logger.accept("Downloaded " + path);
            return path;
        });
    }

    private record MinecraftData(String mirror, String serverUrl, String serverHash, String mappingUrl,
                                 String mappingHash) {
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<Path>[] installForge(InstallInfo info, ExecutorService pool, Consumer<String> logger) {
        var minecraftData = CompletableFuture.supplyAsync(() -> {
            logger.accept("Downloading mc version manifest...");
            for (Map.Entry<String, String> entry : Mirrors.getVersionManifest()) {
                try (var stream = FileDownloader.read(entry.getValue())) {
                    var bytes = stream.readAllBytes();
                    var element = new JsonParser().parse(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                    var versions = element.getAsJsonArray("versions");
                    for (var version : versions) {
                        var id = version.getAsJsonObject().get("id").getAsString();
                        if (Objects.equals(id, info.installer.minecraft)) {
                            var url = version.getAsJsonObject().get("url").getAsString();
                            try (var versionStream = FileDownloader.read(url)) {
                                var object = new JsonParser().parse(new String(versionStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                                var downloads = object.getAsJsonObject("downloads");
                                var server = downloads.getAsJsonObject("server");
                                var serverUrl = server.get("url").getAsString();
                                var serverHash = server.get("sha1").getAsString();
                                var mapping = downloads.getAsJsonObject("server_mappings");
                                var mappingUrl = mapping.get("url").getAsString();
                                var mappingHash = mapping.get("sha1").getAsString();
                                logger.accept("Minecraft version: %s, server: %s, mappings: %s".formatted(info.installer.minecraft, serverHash, mappingHash));
                                return new MinecraftData(entry.getKey(),
                                    Mirrors.mapMojangMirror(serverUrl, entry.getKey()), serverHash,
                                    Mirrors.mapMojangMirror(mappingUrl, entry.getKey()), mappingHash);
                            }
                        }
                    }
                    logger.accept("Version %s not available in %s".formatted(info.installer.minecraft, entry.getKey()));
                } catch (Exception e) {
                    logger.accept("Failed to download manifest from " + entry.getKey() + "\n  " + e);
                }
            }
            return null;
        }, pool);
        String coord = String.format("net.minecraftforge:forge:%s-%s:installer", info.installer.minecraft, info.installer.forge);
        String dist = String.format("forge-%s-%s-installer.jar", info.installer.minecraft, info.installer.forge);
        MavenDownloader forge = new MavenDownloader(Mirrors.getMavenRepo(), coord, dist, info.installer.hash);
        var installerFuture = reportSupply(pool, logger).apply(forge).thenCombineAsync(minecraftData, (path, data) -> {
            try (var jarFile = new JarFile(path.toFile())) {
                Map<String, Map.Entry<String, String>> map = new HashMap<>();
                var profile = jarFile.getEntry("install_profile.json");
                map.putAll(profileLibraries(new InputStreamReader(jarFile.getInputStream(profile)), info.installer.minecraft, data));
                var version = jarFile.getEntry("version.json");
                map.putAll(profileLibraries(new InputStreamReader(jarFile.getInputStream(version)), info.installer.minecraft, data));
                List<Supplier<Path>> suppliers = checkMaven(map);
                CompletableFuture<?>[] array = suppliers.stream().map(reportSupply(pool, logger)).toArray(CompletableFuture[]::new);
                handleFutures(logger, array);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return stripDownloadMapping(path, logger);
        });
        var serverFuture = minecraftData.thenCompose(data -> reportSupply(pool, logger).apply(
            new FileDownloader(String.format(data.serverUrl, info.installer.minecraft),
                String.format("libraries/net/minecraft/server/%1$s/server-%1$s-bundled.jar", info.installer.minecraft), data.serverHash)
        ));
        return new CompletableFuture[]{installerFuture, serverFuture};
    }

    private static Path stripDownloadMapping(Path installer, Consumer<String> logger) {
        try {
            logger.accept("Processing forge installer...");
            var path = Paths.get(".arclight", "installer_stripped.jar");
            if (!Files.isDirectory(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            Files.deleteIfExists(path);
            try (var from = new JarFile(installer.toFile());
                 var to = new JarOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE))) {
                var entries = from.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    // strip signature
                    var name = entry.getName();
                    if (name.endsWith(".SF") || name.endsWith(".DSA") || name.endsWith(".RSA")) {
                        continue;
                    }
                    if (name.equals("install_profile.json")) {
                        var element = new JsonParser().parse(new InputStreamReader(from.getInputStream(entry)));
                        var processors = element.getAsJsonObject().getAsJsonArray("processors");
                        outer:
                        for (var i = 0; i < processors.size(); i++) {
                            var processor = processors.get(i).getAsJsonObject();
                            var args = processor.getAsJsonArray("args");
                            for (var arg : args) {
                                if (arg.getAsString().equals("DOWNLOAD_MOJMAPS")) {
                                    processors.remove(i);
                                    break outer;
                                }
                            }
                        }
                        to.putNextEntry(entry);
                        to.write(new Gson().toJson(element).getBytes(StandardCharsets.UTF_8));
                    } else {
                        to.putNextEntry(entry);
                        from.getInputStream(entry).transferTo(to);
                    }
                }
            }
            return path;
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    private static void handleFutures(Consumer<String> logger, CompletableFuture<?>... futures) {
        for (CompletableFuture<?> future : futures) {
            try {
                future.join();
            } catch (CompletionException e) {
                logger.accept(e.getCause().toString());
                Unsafe.throwException(e.getCause());
            } catch (Exception e) {
                e.printStackTrace();
                Unsafe.throwException(e);
            }
        }
    }

    private static Map<String, Map.Entry<String, String>> profileLibraries(Reader reader, String minecraft, MinecraftData minecraftData) throws IOException {
        Map<String, Map.Entry<String, String>> ret = new HashMap<>();
        var object = new JsonParser().parse(reader).getAsJsonObject();
        JsonArray array = object.getAsJsonArray("libraries");
        for (JsonElement element : array) {
            String name = element.getAsJsonObject().get("name").getAsString();
            JsonObject artifact = element.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("artifact");
            String hash = artifact.get("sha1").getAsString();
            String url = artifact.get("url").getAsString();
            if (url == null || url.trim().isEmpty()) continue;
            ret.put(name, new AbstractMap.SimpleImmutableEntry<>(hash, url));
        }
        if (object.has("data")) {
            var data = object.getAsJsonObject("data");
            if (data.has("MOJMAPS")) {
                var serverMapping = data.getAsJsonObject("MOJMAPS").get("server").getAsString();
                ret.put(serverMapping.substring(1, serverMapping.length() - 1),
                    new AbstractMap.SimpleImmutableEntry<>(minecraftData.mappingHash, minecraftData.mappingUrl));
            }
        }
        return ret;
    }

    private static List<Supplier<Path>> checkMavenNoSource(Map<String, String> map) {
        LinkedHashMap<String, Map.Entry<String, String>> hashMap = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            hashMap.put(entry.getKey(), new AbstractMap.SimpleImmutableEntry<>(entry.getValue(), null));
        }
        return checkMaven(hashMap);
    }

    private static List<Supplier<Path>> checkMaven(Map<String, Map.Entry<String, String>> map) {
        List<Supplier<Path>> incomplete = new ArrayList<>();
        for (Map.Entry<String, Map.Entry<String, String>> entry : map.entrySet()) {
            String maven = entry.getKey();
            String hash = entry.getValue().getKey();
            String url = entry.getValue().getValue();
            String path = "libraries/" + Util.mavenToPath(maven);
            if (new File(path).exists()) {
                try {
                    String fileHash = Util.hash(path);
                    if (!fileHash.equals(hash)) {
                        incomplete.add(new MavenDownloader(Mirrors.getMavenRepo(), maven, path, hash, url));
                    }
                } catch (Exception e) {
                    incomplete.add(new MavenDownloader(Mirrors.getMavenRepo(), maven, path, hash, url));
                }
            } else {
                incomplete.add(new MavenDownloader(Mirrors.getMavenRepo(), maven, path, hash, url));
            }
        }
        return incomplete;
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
        return Map.entry("io.izzel.arclight.boot.application.ApplicationBootstrap", List.of("--launchTarget", "arclight_server"));
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
