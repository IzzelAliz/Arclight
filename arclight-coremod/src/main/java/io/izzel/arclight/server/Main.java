package io.izzel.arclight.server;

import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.forgeinstaller.ForgeInstaller;
import io.izzel.arclight.mod.util.BukkitOptionParser;
import io.izzel.arclight.mod.util.remapper.ArclightRemapper;
import io.izzel.arclight.util.EnumHelper;
import joptsimple.OptionSet;
import net.minecraftforge.server.ServerMain;
import org.apache.logging.log4j.LogManager;
import org.fusesource.jansi.AnsiConsole;

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
            OptionSet options = new BukkitOptionParser().parse(args);
            String jline_UnsupportedTerminal = new String(new char[]{'j', 'l', 'i', 'n', 'e', '.', 'U', 'n', 's', 'u', 'p', 'p', 'o', 'r', 't', 'e', 'd', 'T', 'e', 'r', 'm', 'i', 'n', 'a', 'l'});
            String jline_terminal = new String(new char[]{'j', 'l', 'i', 'n', 'e', '.', 't', 'e', 'r', 'm', 'i', 'n', 'a', 'l'});

            boolean useJline = !(jline_UnsupportedTerminal).equals(System.getProperty(jline_terminal));

            if (options.has("nojline")) {
                System.setProperty("user.language", "en");
                useJline = false;
            }

            if (useJline) {
                AnsiConsole.systemInstall();
            } else {
                System.setProperty(jline.TerminalFactory.JLINE_TERMINAL, jline.UnsupportedTerminal.class.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
            System.setProperty("log4j.jul.LoggerAdapter", "io.izzel.arclight.mod.util.ArclightLoggerAdapter");
            LogManager.getLogger("Arclight").info("Loading mappings ...");
            Objects.requireNonNull(ArclightRemapper.INSTANCE);
            ServerMain.main(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fail to launch Arclight.");
        }
    }
}