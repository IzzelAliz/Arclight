package io.izzel.arclight.common.mod.util.log;

import io.papermc.paper.plugin.configuration.PluginMeta;
import org.apache.logging.log4j.jul.LogManager;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ArclightPluginLogger extends Logger {

    private static final LogManager JUL_MANAGER =
        java.util.logging.LogManager.getLogManager() instanceof LogManager instance ? instance : new LogManager();

    private final Logger logger;

    public ArclightPluginLogger(Plugin context) {
        // PluginLogger's constructor still depends on the legacy descriptor.
        super(context.getClass().getCanonicalName(), null);
        setParent(context.getServer().getLogger());
        setLevel(Level.ALL);
        PluginMeta meta = context.getPluginMeta();
        String prefix = meta.getLoggerPrefix();
        logger = JUL_MANAGER.getLogger(prefix == null ? meta.getName() : prefix);
    }

    @Override
    public void log(LogRecord logRecord) {
        logger.log(logRecord);
    }

    public static Logger getLogger(String name) {
        return JUL_MANAGER.getLogger(name);
    }

    public static Logger getLogger(String name, String rb) {
        return JUL_MANAGER.getLogger(name);
    }
}
