package io.izzel.arclight.server;

import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.util.remapper.ArclightRemapper;
import io.izzel.arclight.forgeinstaller.ForgeInstaller;
import net.minecraftforge.server.ServerMain;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;

public class Main {

    public static void main(String[] args) throws Throwable {
        ForgeInstaller.install();
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
            System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
            System.setProperty("log4j.jul.LoggerAdapter", "io.izzel.arclight.common.mod.util.ArclightLoggerAdapter");
            ArclightVersion.setVersion(ArclightVersion.v1_15);
            LogManager.getLogger("Arclight").info("Loading mappings ...");
            Objects.requireNonNull(ArclightRemapper.INSTANCE);
            ServerMain.main(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fail to launch Arclight.");
        }
    }
}
