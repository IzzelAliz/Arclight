package io.izzel.arclight.boot;

import io.izzel.arclight.forgeinstaller.ForgeInstaller;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Main_Forge {

    public static void main(String[] args) throws Throwable {
        try {
            Map.Entry<String, List<String>> install = ForgeInstaller.install();
            var path = new File(Main_Forge.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalPath();
            System.setProperty("arclight.selfPath", path);
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
}
