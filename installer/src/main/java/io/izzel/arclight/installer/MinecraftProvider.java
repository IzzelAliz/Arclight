package io.izzel.arclight.installer;

import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MinecraftProvider {
    static Function<Supplier<Path>, CompletableFuture<Path>> reportSupply(ExecutorService service, Consumer<String> logger) {
        return it -> CompletableFuture.supplyAsync(it, service).thenApply(path -> {
            logger.accept("Downloaded " + path);
            return path;
        });
    }

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

    static CompletableFuture<MinecraftData> downloadMinecraftData(InstallInfo info, ExecutorService pool, Consumer<String> logger) {
        return CompletableFuture.supplyAsync(() -> {
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
                                return new MinecraftProvider.MinecraftData(entry.getKey(),
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
    }

    static void handleFutures(Consumer<String> logger, CompletableFuture<?>... futures) {
        for (CompletableFuture<?> future : futures) {
            try {
                future.join();
            } catch (CompletionException e) {
                logger.accept(e.getCause().toString());
                Util.throwException(e.getCause());
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    static List<Supplier<Path>> checkMavenNoSource(Map<String, String> map) {
        LinkedHashMap<String, Map.Entry<String, String>> hashMap = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            hashMap.put(entry.getKey(), new AbstractMap.SimpleImmutableEntry<>(entry.getValue(), null));
        }
        return checkMaven(hashMap);
    }

    static List<Supplier<Path>> checkMaven(Map<String, Map.Entry<String, String>> map) {
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

    record MinecraftData(String mirror, String serverUrl, String serverHash, String mappingUrl,
                         String mappingHash) {
    }
}
