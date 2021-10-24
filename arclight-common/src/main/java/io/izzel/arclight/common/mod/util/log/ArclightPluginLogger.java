package io.izzel.arclight.common.mod.util.log;

import org.apache.logging.log4j.jul.LogManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ArclightPluginLogger extends PluginLogger {

    private static final LogManager JUL_MANAGER =
        java.util.logging.LogManager.getLogManager() instanceof LogManager instance ? instance : new LogManager();

    private final Logger logger;

    public ArclightPluginLogger(Plugin context) {
        super(context);
        String prefix = context.getDescription().getPrefix();
        logger = JUL_MANAGER.getLogger(prefix == null ? context.getName() : prefix);
    }

    @Override
    public void log(LogRecord logRecord) {
        logger.log(logRecord);
    }
}
