package io.izzel.arclight.server;

import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.ArclightMain;
import io.izzel.arclight.forgeinstaller.ForgeInstaller;

import java.io.File;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Main_Forge {

    public static void main(String[] args) throws Throwable {
        new ArclightMain() {
            private Map.Entry<String, List<String>> install;

            @Override
            protected void afterSetup() throws Throwable {
                ArclightVersion.setVersion(ArclightVersion.v1_17_R1);
                install = ForgeInstaller.install();
            }

            @Override
            protected void beforeStart() throws Throwable {
                var path = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalPath();
                System.setProperty("arclight.selfPath", path);
                var cl = Class.forName(install.getKey());
                var handle = Unsafe.lookup().findStatic(cl, "main", MethodType.methodType(void.class, String[].class));
                var target = Stream.concat(install.getValue().stream(), Arrays.stream(args)).toArray(String[]::new);
                handle.invoke((Object) target);
            }
        }.run(args);
    }
}
