package io.izzel.arclight.neoforge;

import io.izzel.arclight.api.Arclight;
import io.izzel.arclight.common.mod.ArclightCommon;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.neoforge.mod.NeoForgeArclightServer;
import io.izzel.arclight.neoforge.mod.NeoForgeCommonImpl;
import io.izzel.arclight.neoforge.mod.event.ArclightEventDispatcherRegistry;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.io.PrintStream;

@Mod("arclight")
public class ArclightMod {

    public ArclightMod() {
        ArclightServer.LOGGER.info("mod-load");
        Arclight.setServer(new NeoForgeArclightServer());
        System.setOut(new LoggingPrintStream("STDOUT", System.out, Level.INFO));
        System.setErr(new LoggingPrintStream("STDERR", System.err, Level.ERROR));
        ArclightEventDispatcherRegistry.registerAllEventDispatchers();
    }

    private static class LoggingPrintStream extends PrintStream {

        private final Logger logger;
        private final Level level;

        public LoggingPrintStream(String name, @NotNull OutputStream out, Level level) {
            super(out);
            this.logger = LogManager.getLogger(name);
            this.level = level;
        }

        @Override
        public void println(@Nullable String x) {
            logger.log(level, x);
        }

        @Override
        public void println(@Nullable Object x) {
            logger.log(level, String.valueOf(x));
        }
    }
}
