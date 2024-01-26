package io.izzel.arclight.boot.application;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class Main_Forge {

    public static void main(String[] args) throws Throwable {
        try {
            Map.Entry<String, List<String>> install = forgeInstall();
            var cl = Class.forName(install.getKey());
            var method = cl.getMethod("main", String[].class);
            var target = Stream.concat(install.getValue().stream(), Arrays.stream(args)).toArray(String[]::new);
            method.invoke(null, (Object) target);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fail to launch Arclight.");
            System.exit(-1);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map.Entry<String, List<String>> forgeInstall() throws Throwable {
        var path = Paths.get(".arclight", "gson.jar");
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.copy(Objects.requireNonNull(Main_Forge.class.getResourceAsStream("/gson.jar")), path);
        }
        try (var loader = new URLClassLoader(new URL[]{path.toUri().toURL(), Main_Forge.class.getProtectionDomain().getCodeSource().getLocation()}, ClassLoader.getPlatformClassLoader())) {
            var cl = loader.loadClass("io.izzel.arclight.forgeinstaller.ForgeInstaller");
            var handle = MethodHandles.lookup().findStatic(cl, "applicationInstall", MethodType.methodType(Map.Entry.class));
            return (Map.Entry<String, List<String>>) handle.invoke();
        }
    }
}
