package io.izzel.arclight.common;

import com.google.common.collect.ImmutableMap;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.util.log.ArclightI18nLogger;
import io.izzel.arclight.common.mod.util.log.ArclightLazyLogManager;
import io.izzel.arclight.common.util.EnumTypeFactory;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.i18n.ArclightLocale;
import net.minecraftforge.server.ServerMain;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public abstract class ArclightMain {

    public void run(String[] args) throws Throwable {
        System.setProperty("java.util.logging.manager", ArclightLazyLogManager.class.getCanonicalName());
        System.setProperty("log4j.jul.LoggerAdapter", "io.izzel.arclight.common.mod.util.log.ArclightLoggerAdapter");
        ArclightLocale.info("i18n.using-language", ArclightConfig.spec().getLocale().getCurrent(), ArclightConfig.spec().getLocale().getFallback());
        this.afterSetup();
        try { // Java 9 & Java 兼容性
            int javaVersion = (int) Float.parseFloat(System.getProperty("java.class.version"));
            if (javaVersion == 53) {
                throw new Exception("Only Java 8 and Java 10+ is supported.");
            }
            Unsafe.ensureClassInitialized(EnumHelper.class);
        } catch (Throwable t) {
            System.err.println("Your Java is not compatible with Arclight.");
            t.printStackTrace();
            return;
        }
        try {
            printLogo();
            this.beforeStart();
            this.dirtyHacks();
            ServerMain.main(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fail to launch Arclight.");
        }
    }

    private void dirtyHacks() throws Exception {
        TypeAdapters.ENUM_FACTORY.create(null, TypeToken.get(ArclightMain.class));
        Field field = TypeAdapters.class.getDeclaredField("ENUM_FACTORY");
        Object base = Unsafe.staticFieldBase(field);
        long offset = Unsafe.staticFieldOffset(field);
        Unsafe.putObjectVolatile(base, offset, new EnumTypeFactory());
    }

    private void printLogo() throws Exception {
        URI uri = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
        FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + uri), ImmutableMap.of("create", "true"));
        try (InputStream stream = Files.newInputStream(fs.getPath("/META-INF/MANIFEST.MF"))) {
            Manifest manifest = new Manifest(stream);
            Attributes attributes = manifest.getMainAttributes();
            String version = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            String buildTime = attributes.getValue("Implementation-Timestamp");
            ArclightI18nLogger.getLogger("Arclight").info("logo", version, buildTime);
        }
    }

    protected void afterSetup() throws Throwable {
    }

    protected void beforeStart() throws Throwable {
    }
}
