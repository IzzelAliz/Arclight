package io.izzel.arclight.boot.application;

import cpw.mods.cl.ModuleClassLoader;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class Main_Forge {

    public static void main(String[] args) throws Throwable {
        try {
            // Modifying the installer with a file archiver will corrupt the jar file
            // The manifest data will be unavailable for further use, stop here
            verifyManifest();
            Map.Entry<String, List<String>> install = forgeInstall();
            var cl = new BootstrapTransformer(Main_Forge.class.getClassLoader());
            var clazz = cl.loadClass(install.getKey(), true);
            var method = clazz.getMethod("main", String[].class);
            var target = Stream.concat(install.getValue().stream(), Arrays.stream(args)).toArray(String[]::new);
            method.invoke(null, (Object) target);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fail to launch Arclight.");
            System.exit(-1);
        }
    }

    private static void verifyManifest() throws IOException, URISyntaxException {
        var location = Main_Forge.class.getProtectionDomain().getCodeSource().getLocation();
        try(JarFile baseArchive = new JarFile(new File(location.toURI()))) {
            var mf = baseArchive.getManifest();
            if (mf == null || mf.getMainAttributes().isEmpty()) {
                System.err.println("Failed to verify completeness for Arclight installer.");
                System.err.println("The manifest data is corrupted, is the jar file modified?");
                System.err.println("Cannot proceed, Arclight will exit");
                throw new IOException("The installer jar file is corrupted");
            }
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
