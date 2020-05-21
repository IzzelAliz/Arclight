package io.izzel.arclight.forgeinstaller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.izzel.arclight.api.Unsafe;

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
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ForgeInstaller {

    private static final String INFO = "Download mirror service by BMCLAPI: https://bmclapidoc.bangbang93.com\n" +
        "Support MinecraftForge project at https://www.patreon.com/LexManos/";
    private static final String INSTALLER_URL = "https://bmclapi2.bangbang93.com/maven/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar";
    private static final String BMCL_API = "https://bmclapi2.bangbang93.com/maven/";
    private static final String SERVER_URL = "https://bmclapi2.bangbang93.com/version/%s/server";

    public static void install() throws Throwable {
        InputStream stream = ForgeInstaller.class.getResourceAsStream("/META-INF/installer.json");
        InstallInfo installInfo = new Gson().fromJson(new InputStreamReader(stream), InstallInfo.class);
        Path path = Paths.get(String.format("forge-%s-%s.jar", installInfo.installer.minecraft, installInfo.installer.forge));
        if (!Files.exists(path)) {
            System.out.println(INFO);
            Thread.sleep(5000);
            download(installInfo);
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("java", "-jar", String.format("forge-%s-%s-installer.jar", installInfo.installer.minecraft, installInfo.installer.forge), "--installServer", ".");
            builder.inheritIO();
            Process process = builder.start();
            process.waitFor();
        }
        classpath(path, installInfo);
    }

    private static void classpath(Path path, InstallInfo installInfo) throws Throwable {
        JarFile jarFile = new JarFile(path.toFile());
        Manifest manifest = jarFile.getManifest();
        String[] split = manifest.getMainAttributes().getValue("Class-Path").split(" ");
        for (String s : split) {
            if (s.contains("eventbus-1.0.0-service")) continue;
            addToPath(Paths.get(s));
        }
        for (String library : installInfo.libraries) {
            addToPath(Paths.get("libraries", mavenToPath(library)));
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

    private static void download(InstallInfo info) throws Exception {
        SimpleDownloader downloader = new SimpleDownloader();
        String format = String.format(INSTALLER_URL, info.installer.minecraft, info.installer.forge, info.installer.minecraft, info.installer.forge);
        String dist = String.format("forge-%s-%s-installer.jar", info.installer.minecraft, info.installer.forge);
        downloader.download(format, dist, null,
            path -> processInstaller(path, downloader));
        for (String library : info.libraries) {
            String path = mavenToPath(library);
            downloader.downloadMaven(path);
        }
        downloader.download(String.format(SERVER_URL, info.installer.minecraft), String.format("minecraft_server.%s.jar", info.installer.minecraft), null);
        if (!downloader.awaitTermination()) {
            Files.deleteIfExists(Paths.get(dist));
            throw new Exception();
        }
    }

    private static void processInstaller(Path path, SimpleDownloader downloader) {
        try {
            FileSystem system = FileSystems.newFileSystem(path, null);
            Set<String> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
            Path profile = system.getPath("install_profile.json");
            downloadLibraries(downloader, profile, set);
            Path version = system.getPath("version.json");
            downloadLibraries(downloader, version, set);
            system.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadLibraries(SimpleDownloader downloader, Path path, Set<String> set) throws IOException {
        JsonArray array = new JsonParser().parse(Files.newBufferedReader(path)).getAsJsonObject().getAsJsonArray("libraries");
        for (JsonElement element : array) {
            String name = element.getAsJsonObject().get("name").getAsString();
            if (!set.add(name)) continue;
            String libPath = mavenToPath(name);
            JsonObject artifact = element.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("artifact");
            String url = artifact.get("url").getAsString();
            if (url == null || url.trim().isEmpty()) continue;
            String hash = artifact.get("sha1").getAsString();
            downloader.download(BMCL_API + libPath, "libraries/" + libPath, hash);
        }
    }

    private static String mavenToPath(String maven) {
        String type;
        if (maven.matches(".*@\\w+$")) {
            int i = maven.lastIndexOf('@');
            type = maven.substring(i + 1);
            maven = maven.substring(0, i);
        } else {
            type = "jar";
        }
        String[] arr = maven.split(":");
        if (arr.length == 3) {
            String pkg = arr[0].replace('.', '/');
            return String.format("%s/%s/%s/%s-%s.%s", pkg, arr[1], arr[2], arr[1], arr[2], type);
        } else if (arr.length == 4) {
            String pkg = arr[0].replace('.', '/');
            return String.format("%s/%s/%s/%s-%s-%s.%s", pkg, arr[1], arr[2], arr[1], arr[2], arr[3], type);
        } else throw new RuntimeException("Wrong maven coordinate " + maven);
    }
}
