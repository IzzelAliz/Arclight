package io.izzel.arclight.server;

import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.common.ArclightMain;
import io.izzel.arclight.forgeinstaller.ForgeInstaller;

public class Main_1_16 {

    public static void main(String[] args) throws Throwable {
        new ArclightMain() {
            @Override
            protected void afterSetup() throws Throwable {
                ArclightVersion.setVersion(ArclightVersion.v1_16_4);
                ForgeInstaller.install();
            }
        }.run(args);
    }
}
