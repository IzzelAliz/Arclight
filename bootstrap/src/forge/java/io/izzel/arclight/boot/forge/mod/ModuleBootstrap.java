package io.izzel.arclight.boot.forge.mod;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.boot.AbstractBootstrap;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.i18n.ArclightLocale;
import net.minecraftforge.bootstrap.api.BootstrapEntryPoint;

import java.util.Arrays;
import java.util.ServiceLoader;

public class ModuleBootstrap implements BootstrapEntryPoint, AbstractBootstrap {

    private static final int MIN_DEPRECATED_VERSION = 60;
    private static final int MIN_DEPRECATED_JAVA_VERSION = 16;

    @Override
    public void main(String[] args) {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setProperty("log4j.jul.LoggerAdapter", "io.izzel.arclight.boot.log.ArclightLoggerAdapter");
        System.setProperty("log4j.configurationFile", "arclight-log4j2.xml");
        ArclightLocale.info("i18n.using-language", ArclightConfig.spec().getLocale().getCurrent(), ArclightConfig.spec().getLocale().getFallback());
        try {
            int javaVersion = (int) Float.parseFloat(System.getProperty("java.class.version"));
            if (javaVersion < MIN_DEPRECATED_VERSION) {
                ArclightLocale.error("java.deprecated", System.getProperty("java.version"), MIN_DEPRECATED_JAVA_VERSION);
                Thread.sleep(3000);
            }
            Unsafe.ensureClassInitialized(EnumHelper.class);
        } catch (Throwable t) {
            System.err.println("Your Java is not compatible with Arclight.");
            t.printStackTrace();
            return;
        }
        try {
            this.setupMod(ArclightPlatform.FORGE);
            this.dirtyHacks();
            int targetIndex = Arrays.asList(args).indexOf("--launchTarget");
            if (targetIndex >= 0 && targetIndex < args.length - 1) {
                args[targetIndex + 1] = "arclight_server";
            }
            ServiceLoader.load(getClass().getModule().getLayer(), BootstrapEntryPoint.class).stream()
                .filter(it -> !it.type().getName().contains("arclight"))
                .findFirst().orElseThrow().get().main(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fail to launch Arclight.");
        }
    }
}
