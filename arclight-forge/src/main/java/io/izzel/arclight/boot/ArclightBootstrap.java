package io.izzel.arclight.boot;

import com.google.common.collect.ImmutableMap;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.i18n.ArclightLocale;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class ArclightBootstrap implements Consumer<String[]> {

    private static final int MIN_DEPRECATED_VERSION = 60;
    private static final int MIN_DEPRECATED_JAVA_VERSION = 16;

    @Override
    @SuppressWarnings("unchecked")
    public void accept(String[] args) {
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
            this.setupMod();
            this.dirtyHacks();
            ServiceLoader.load(getClass().getModule().getLayer(), Consumer.class).stream()
                .filter(it -> !it.type().getName().contains("Arclight"))
                .findFirst().orElseThrow().get().accept(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fail to launch Arclight.");
        }
    }

    private void dirtyHacks() throws Exception {
        TypeAdapters.ENUM_FACTORY.create(null, TypeToken.get(Object.class));
        Field field = TypeAdapters.class.getDeclaredField("ENUM_FACTORY");
        Object base = Unsafe.staticFieldBase(field);
        long offset = Unsafe.staticFieldOffset(field);
        Unsafe.putObjectVolatile(base, offset, new EnumTypeFactory());
    }

    private void setupMod() throws Exception {
        ArclightVersion.setVersion(ArclightVersion.v1_17_R1);
        URI uri = new File(System.getProperty("arclight.selfPath")).toURI();
        FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + uri), ImmutableMap.of("create", "true"));
        try (InputStream stream = Files.newInputStream(fs.getPath("/META-INF/MANIFEST.MF"))) {
            Manifest manifest = new Manifest(stream);
            Attributes attributes = manifest.getMainAttributes();
            String version = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            extract(fs.getPath("/common.jar"), version);
            String buildTime = attributes.getValue("Implementation-Timestamp");
            LogManager.getLogger("Arclight").info(ArclightLocale.getInstance().get("logo"), version, buildTime);
        }
    }

    private void extract(Path path, String version) throws Exception {
        System.setProperty("arclight.version", version);
        var dir = Paths.get(".arclight", "mod_file");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        var mod = dir.resolve(version + ".jar");
        if (!Files.exists(mod) || Boolean.getBoolean("arclight.alwaysExtract")) {
            for (Path old : Files.list(dir).collect(Collectors.toList())) {
                Files.delete(old);
            }
            Files.copy(path, mod);
        }
    }
}
