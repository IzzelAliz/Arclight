package io.izzel.arclight.installer;

import com.google.gson.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class ForgeLikeProvider {

    static CompletableFuture<Path> downloadInstaller(String coord, String dist, String hash, CompletableFuture<MinecraftProvider.MinecraftData> minecraftData, InstallInfo info, ExecutorService pool, Consumer<String> logger) {
        String officialRepo = coord.startsWith("net.neoforged:")
            ? "https://maven.neoforged.net/releases/" : "https://maven.minecraftforge.net/";
        MavenDownloader forge = new MavenDownloader(Mirrors.getMavenRepo(), coord, dist, hash,
            officialRepo + Util.mavenToPath(coord));
        return MinecraftProvider.reportSupply(pool, logger).apply(forge).thenCombineAsync(minecraftData, (path, data) -> {
            try (var jarFile = new JarFile(path.toFile())) {
                Map<String, Map.Entry<String, String>> map = new HashMap<>();
                var profile = jarFile.getEntry("install_profile.json");
                map.putAll(profileLibraries(new InputStreamReader(jarFile.getInputStream(profile)), info.installer.minecraft, data));
                var version = jarFile.getEntry("version.json");
                map.putAll(profileLibraries(new InputStreamReader(jarFile.getInputStream(version)), info.installer.minecraft, data));
                List<Supplier<Path>> suppliers = MinecraftProvider.checkMaven(map);
                CompletableFuture<?>[] array = suppliers.stream().map(MinecraftProvider.reportSupply(pool, logger)).toArray(CompletableFuture[]::new);
                MinecraftProvider.handleFutures(logger, array);
            } catch (IOException e) {
                throw new CompletionException("Cannot inspect installer profile " + path, e);
            }
            return stripDownloadMapping(path, logger);
        });
    }

    static Path stripDownloadMapping(Path installer, Consumer<String> logger) {
        try {
            // Keep NeoForge 26.1.2's signed installer intact: its profile has no
            // mapping-download task, only EXTRACT_FILES/PROCESS_MINECRAFT_JAR.
            try (var source = new JarFile(installer.toFile())) {
                var profile = source.getJarEntry("install_profile.json");
                if (profile == null) throw new IOException("Installer has no install_profile.json: " + installer);
                var json = JsonParser.parseReader(new InputStreamReader(source.getInputStream(profile))).getAsJsonObject();
                var processors = json.getAsJsonArray("processors");
                boolean downloadsMappings = false;
                if (processors != null) for (var processor : processors) {
                    for (var arg : processor.getAsJsonObject().getAsJsonArray("args")) {
                        if ("DOWNLOAD_MOJMAPS".equals(arg.getAsString())) downloadsMappings = true;
                    }
                }
                if (!downloadsMappings) return installer;
            }
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
                        // NeoForge 26.1.2 uses PROCESS_MINECRAFT_JAR on the bundled
                        // server and has no DOWNLOAD_MOJMAPS processor. Preserve it.
                        if (processors == null) {
                            to.putNextEntry(entry);
                            to.write(new Gson().toJson(element).getBytes(StandardCharsets.UTF_8));
                            continue;
                        }
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

    static Map<String, Map.Entry<String, String>> profileLibraries(Reader reader, String minecraft, MinecraftProvider.MinecraftData minecraftData) throws IOException {
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
                if (minecraftData.mappingHash() == null || minecraftData.officialMappingUrl() == null) {
                    throw new IOException("Installer requires MOJMAPS but Minecraft " + minecraft + " publishes no server mappings");
                }
                var serverMapping = data.getAsJsonObject("MOJMAPS").get("server").getAsString();
                ret.put(serverMapping.substring(1, serverMapping.length() - 1),
                        new AbstractMap.SimpleImmutableEntry<>(minecraftData.mappingHash(), minecraftData.officialMappingUrl()));
            }
        }
        return ret;
    }
}
