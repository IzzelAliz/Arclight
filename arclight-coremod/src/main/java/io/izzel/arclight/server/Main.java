package io.izzel.arclight.server;

import com.google.common.io.ByteStreams;
import io.izzel.arclight.mod.util.BukkitOptionParser;
import io.izzel.arclight.mod.util.remapper.ArclightRemapper;
import io.izzel.arclight.util.EnumHelper;
import io.izzel.arclight.util.Unsafe;
import joptsimple.OptionSet;
import net.minecraftforge.server.ServerMain;
import org.apache.logging.log4j.LogManager;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class Main {

    public static void main(String[] args) throws Throwable {
        if (Files.notExists(Paths.get("forge-1.14.4-28.2.0.jar"))) {
            System.err.println("Install forge 1.14.4-28.2.0 before launching Arclight.");
            return;
        }
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
            if (Files.notExists(Paths.get("./libraries/net/minecraftforge/eventbus/2.0.0-milestone.1/eventbus-2.0.0-milestone.1-service.jar"))) {
                Path folder = Paths.get("./libraries/net/minecraftforge/eventbus");
                Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                throw new Exception();
            }
            Class.forName("org.spongepowered.asm.mixin.Mixins", false, ClassLoader.getSystemClassLoader());
            Class.forName("org.objectweb.asm.util.CheckClassAdapter", false, ClassLoader.getSystemClassLoader());
            Class.forName("org.objectweb.asm.tree.analysis.AnalyzerException", false, ClassLoader.getSystemClassLoader());
            Class.forName("net.md_5.bungee.api.ChatColor", false, ClassLoader.getSystemClassLoader());
            Class.forName("org.yaml.snakeyaml.Yaml", false, ClassLoader.getSystemClassLoader());
            Class.forName("org.sqlite.JDBC", false, ClassLoader.getSystemClassLoader());
            Class.forName("com.mysql.jdbc.Driver", false, ClassLoader.getSystemClassLoader());
            Class.forName("org.apache.commons.lang.Validate", false, ClassLoader.getSystemClassLoader());
            Class.forName("jline.Terminal", false, ClassLoader.getSystemClassLoader());
            Class.forName("org.json.simple.JSONObject", false, ClassLoader.getSystemClassLoader());
            Class.forName("org.apache.logging.log4j.jul.LogManager", false, ClassLoader.getSystemClassLoader());
            Class.forName("net.minecraftforge.eventbus.EventBus", false, ClassLoader.getSystemClassLoader());
            Class.forName("net.md_5.specialsource.JarRemapper", false, ClassLoader.getSystemClassLoader());
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
        } catch (Exception e) {
            System.err.println("FATAL ERROR: The libraries required to launch Arclight are missing, extracting...");
            extract("org.spongepowered:mixin:0.8");
            extract("org.ow2.asm:asm-util:6.2");
            extract("org.ow2.asm:asm-analysis:6.2");
            extract("org.yaml:snakeyaml:1.23");
            extract("net.md-5:bungeecord-chat:1.13-SNAPSHOT");
            extract("org.xerial:sqlite-jdbc:3.28.0");
            extract("mysql:mysql-connector-java:5.1.47");
            extract("commons-lang:commons-lang:2.6");
            extract("jline:jline:2.12.1");
            extract("com.googlecode.json-simple:json-simple:1.1.1");
            extract("org.apache.logging.log4j:log4j-jul:2.11.2");
            extract("net.md-5:SpecialSource:1.8.6");
            extract("net.minecraftforge:eventbus:2.0.0-milestone.1:service");
            System.out.println("Please RESTART the server.");
        }
    }

    private static void extract(String artifact) throws Throwable {
        String[] split = artifact.split(":");
        if (split.length == 3) {
            String jar = String.format("/%s-%s.jar", split[1], split[2]);
            String path = split[0].replace('.', '/') + "/" +
                split[1] + "/" + split[2] + jar;
            extract("libs" + jar, "./libraries/" + path);
            System.out.println("Extracted " + artifact);
        } else if (split.length == 4) {
            String jar = String.format("/%s-%s-%s.jar", split[1], split[2], split[3]);
            String path = split[0].replace('.', '/') + "/" +
                split[1] + "/" + split[2] + jar;
            extract("libs" + jar, "./libraries/" + path);
            System.out.println("Extracted " + artifact);
        }
    }

    private static void extract(String name, String target) throws Throwable {
        Path path = Paths.get(target);
        if (Files.notExists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            InputStream stream = Main.class.getResourceAsStream("/" + name);
            if (stream != null) {
                OutputStream outputStream = Files.newOutputStream(Paths.get(target));
                ByteStreams.copy(stream, outputStream);
                stream.close();
                outputStream.close();
            }
        }
    }
}