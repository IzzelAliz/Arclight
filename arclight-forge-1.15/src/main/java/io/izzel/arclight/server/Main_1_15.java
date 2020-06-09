package io.izzel.arclight.server;

import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.common.ArclightMain;
import io.izzel.arclight.forgeinstaller.ForgeInstaller;

public class Main_1_15 extends ArclightMain {

    public static void main(String[] args) throws Throwable {
        new Main_1_15().run(args);
    }

    @Override
    protected void afterSetup() throws Throwable {
        ArclightVersion.setVersion(ArclightVersion.v1_15);
        ForgeInstaller.install();
    }
}
