package io.izzel.arclight.boot.fabric.application;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class Main_Fabric {

    public static void main(String[] args) throws Throwable {
        System.setProperty("fabric.skipMcProvider", "true");
        try {
            var install = forgeInstall();
            var ours = Main_Fabric.class.getProtectionDomain().getCodeSource().getLocation();
            var classloader = new URLClassLoader(Stream.concat(Stream.of(ours), install.getValue().stream().map(it -> {
                try {
                    return it.toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            })).toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
            Thread.currentThread().setContextClassLoader(classloader);
            var cl = Class.forName(install.getKey(), false, classloader);
            var handle = MethodHandles.lookup().findStatic(cl, "main", MethodType.methodType(void.class, String[].class));
            handle.invoke((Object) args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fail to launch Arclight.");
            System.exit(-1);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map.Entry<String, List<Path>> forgeInstall() throws Throwable {
        var path = Paths.get(".arclight", "gson.jar");
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.copy(Objects.requireNonNull(Main_Fabric.class.getResourceAsStream("/gson.jar")), path);
        }
        try (var loader = new URLClassLoader(new URL[]{path.toUri().toURL(), Main_Fabric.class.getProtectionDomain().getCodeSource().getLocation()}, ClassLoader.getPlatformClassLoader())) {
            var cl = loader.loadClass("io.izzel.arclight.installer.FabricInstaller");
            var handle = MethodHandles.lookup().findStatic(cl, "applicationInstall", MethodType.methodType(Map.Entry.class));
            return (Map.Entry<String, List<Path>>) handle.invoke();
        }
    }
}