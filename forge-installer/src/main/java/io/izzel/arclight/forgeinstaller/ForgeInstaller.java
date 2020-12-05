package io.izzel.arclight.forgeinstaller;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.i18n.ArclightLocale;
import io.izzel.arclight.i18n.LocalizedException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ForgeInstaller {

    private static final String[] MAVEN_REPO = {
        "https://arclight.mcxk.net/"
    };
    private static final String INSTALLER_URL = "https://arclight.mcxk.net/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar";
    private static final String SERVER_URL = "https://arclight.mcxk.net/net/minecraft/server/minecraft_server.%s.jar";
    private static final Map<String, String> VERSION_HASH = ImmutableMap.of(
        "1.14.4", "3dc3d84a581f14691199cf6831b71ed1296a9fdf",
        "1.15.2", "bb2b6b1aefcd70dfd1892149ac3a215f6c636b07",
        "1.16.3", "f02f4473dbf152c23d7d484952121db0b36698cb",
        "1.16.4", "35139deedbd5182953cf1caa23835da59ca3d7cd"
    );

    public static void install() throws Throwable {
        InputStream stream = ForgeInstaller.class.getResourceAsStream("/META-INF/installer.json");
        InstallInfo installInfo = new Gson().fromJson(new InputStreamReader(stream), InstallInfo.class);
        List<Supplier<Path>> suppliers = checkMavenNoSource(installInfo.libraries);
        Path path = Paths.get(String.format("forge-%s-%s.jar", installInfo.installer.minecraft, installInfo.installer.forge));
        if (!suppliers.isEmpty() || !Files.exists(path)) {
            ArclightLocale.info("downloader.info2");
            ExecutorService pool = Executors.newFixedThreadPool(8);
            CompletableFuture<?>[] array = suppliers.stream().map(reportSupply(pool)).toArray(CompletableFuture[]::new);
            if (!Files.exists(path)) {
                CompletableFuture<?>[] futures = installForge(installInfo, pool);
                handleFutures(futures);
                ArclightLocale.info("downloader.forge-install");
                ProcessBuilder builder = new ProcessBuilder();
                builder.command("java", "-Djava.net.useSystemProxies=true", "-jar", String.format("forge-%s-%s-installer.jar", installInfo.installer.minecraft, installInfo.installer.forge), "--installServer", ".");
                builder.inheritIO();
                Process process = builder.start();
                process.waitFor();
            }
            handleFutures(array);
            pool.shutdownNow();
        }
        classpath(path, installInfo);
    }

    private static Function<Supplier<Path>, CompletableFuture<Path>> reportSupply(ExecutorService service) {
        return it -> CompletableFuture.supplyAsync(it, service).thenApply(path -> {
            ArclightLocale.info("downloader.complete", path);
            return path;
        });
    }

    private static CompletableFuture<?>[] installForge(InstallInfo info, ExecutorService pool) throws Exception {
        String format = String.format(INSTALLER_URL, info.installer.minecraft, info.installer.forge, info.installer.minecraft, info.installer.forge);
        String dist = String.format("forge-%s-%s-installer.jar", info.installer.minecraft, info.installer.forge);
        FileDownloader fd = new FileDownloader(format, dist, info.installer.hash);
        CompletableFuture<?> installerFuture = reportSupply(pool).apply(fd).thenAccept(path -> {
            try {
                FileSystem system = FileSystems.newFileSystem(path, null);
                Map<String, Map.Entry<String, String>> map = new HashMap<>();
                Path profile = system.getPath("install_profile.json");
                map.putAll(profileLibraries(profile));
                Path version = system.getPath("version.json");
                map.putAll(profileLibraries(version));
                List<Supplier<Path>> suppliers = checkMaven(map);
                CompletableFuture<?>[] array = suppliers.stream().map(reportSupply(pool)).toArray(CompletableFuture[]::new);
                handleFutures(array);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        CompletableFuture<?> serverFuture = reportSupply(pool).apply(
            new FileDownloader(String.format(SERVER_URL, info.installer.minecraft),
                String.format("minecraft_server.%s.jar", info.installer.minecraft), VERSION_HASH.get(info.installer.minecraft))
        );
        return new CompletableFuture<?>[]{installerFuture, serverFuture};
    }

    private static void handleFutures(CompletableFuture<?>... futures) {
        for (CompletableFuture<?> future : futures) {
            try {
                future.join();
            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof LocalizedException) {
                    LocalizedException local = (LocalizedException) cause;
                    ArclightLocale.error(local.node(), local.args());
                } else throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static Map<String, Map.Entry<String, String>> profileLibraries(Path path) throws IOException {
        Map<String, Map.Entry<String, String>> ret = new HashMap<>();
        JsonArray array = new JsonParser().parse(Files.newBufferedReader(path)).getAsJsonObject().getAsJsonArray("libraries");
        for (JsonElement element : array) {
            String name = element.getAsJsonObject().get("name").getAsString();
            JsonObject artifact = element.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("artifact");
            String hash = artifact.get("sha1").getAsString();
            String url = artifact.get("url").getAsString();
            if (url == null || url.trim().isEmpty()) continue;
            ret.put(name, new AbstractMap.SimpleImmutableEntry<>(hash, url));
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
                        incomplete.add(new MavenDownloader(MAVEN_REPO, maven, path, hash, url));
                    }
                } catch (Exception e) {
                    incomplete.add(new MavenDownloader(MAVEN_REPO, maven, path, hash, url));
                }
            } else {
                incomplete.add(new MavenDownloader(MAVEN_REPO, maven, path, hash, url));
            }
        }
        return incomplete;
    }

    private static void classpath(Path path, InstallInfo installInfo) throws Throwable {
        JarFile jarFile = new JarFile(path.toFile());
        Manifest manifest = jarFile.getManifest();
        String[] split = manifest.getMainAttributes().getValue("Class-Path").split(" ");
        for (String s : split) {
            addToPath(Paths.get(s));
        }
        for (String library : installInfo.libraries.keySet()) {
            addToPath(Paths.get("libraries", Util.mavenToPath(library)));
        }
        addToPath(path);
    }

    private static void addToPath(Path path) throws Throwable {
        ClassLoader loader = ForgeInstaller.class.getClassLoader();
        Field ucpField = loader.getClass().getDeclaredField("ucp");
        long offset = Unsafe.objectFieldOffset(ucpField);
        Object ucp = Unsafe.getObject(loader, offset);
        Method method = ucp.getClass().getDeclaredMethod("addURL", URL.class);
        Unsafe.lookup().unreflect(method).invoke(ucp, path.toUri().toURL());
    }
}
