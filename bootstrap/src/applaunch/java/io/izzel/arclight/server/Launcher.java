package io.izzel.arclight.server;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.jar.Manifest;

public class Launcher {

    private static final int MIN_CLASS_VERSION = 61;
    private static final int MIN_JAVA_VERSION = 17;

    public static void main(String[] args) throws Throwable {
        int javaVersion = (int) Float.parseFloat(System.getProperty("java.class.version"));
        if (javaVersion < MIN_CLASS_VERSION) {
            System.err.println("Arclight requires Java " + MIN_JAVA_VERSION);
            System.err.println("Current: " + System.getProperty("java.version"));
            System.exit(-1);
            return;
        }

        try (InputStream input = Launcher.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            Manifest manifest = new Manifest(input);
            String target = manifest.getMainAttributes().getValue("Arclight-Target");
            MethodHandle main = MethodHandles.lookup().findStatic(Class.forName(target), "main", MethodType.methodType(void.class, String[].class));
            main.invoke((Object) args);
        }
    }
}
