package io.izzel.arclight.common.mod.util.log;

import org.apache.logging.log4j.jul.ApiLogger;
import org.apache.logging.log4j.jul.CoreLoggerAdapter;
import org.apache.logging.log4j.spi.LoggerContext;
import sun.reflect.CallerSensitive;

import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ArclightLoggerAdapter extends CoreLoggerAdapter {

    @Override
    protected Logger newLogger(final String name, final LoggerContext context) {
        Logger logger = super.newLogger(name, context);
        if (logger instanceof ApiLogger) {
            return new ArclightJulLogger(logger);
        } else {
            return logger;
        }
    }

    public static class ArclightJulLogger extends Logger {

        private final Logger logger;

        protected ArclightJulLogger(Logger logger) {
            super(logger.getName(), null);
            this.logger = logger;
        }

        @CallerSensitive
        public static Logger getLogger(String name) {
            return Logger.getLogger(name);
        }

        @CallerSensitive
        public static Logger getLogger(String name, String resourceBundleName) {
            return Logger.getLogger(name, resourceBundleName);
        }

        public static Logger getAnonymousLogger() {
            return Logger.getAnonymousLogger();
        }

        @CallerSensitive
        public static Logger getAnonymousLogger(String resourceBundleName) {
            return Logger.getAnonymousLogger(resourceBundleName);
        }

        @Override
        public ResourceBundle getResourceBundle() {
            return logger.getResourceBundle();
        }

        @Override
        public String getResourceBundleName() {
            return logger.getResourceBundleName();
        }

        @Override
        public void setFilter(Filter newFilter) throws SecurityException {
            logger.setFilter(newFilter);
        }

        @Override
        public Filter getFilter() {
            return logger.getFilter();
        }

        @Override
        public void log(LogRecord record) {
            logger.log(record);
        }

        @Override
        public void log(Level level, String msg) {
            logger.log(level, msg);
        }

        @Override
        public void log(Level level, Supplier<String> msgSupplier) {
            logger.log(level, msgSupplier);
        }

        @Override
        public void log(Level level, String msg, Object param1) {
            logger.log(level, msg, param1);
        }

        @Override
        public void log(Level level, String msg, Object[] params) {
            logger.log(level, msg, params);
        }

        @Override
        public void log(Level level, String msg, Throwable thrown) {
            logger.log(level, msg, thrown);
        }

        @Override
        public void log(Level level, Throwable thrown, Supplier<String> msgSupplier) {
            logger.log(level, thrown, msgSupplier);
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
            logger.logp(level, sourceClass, sourceMethod, msg);
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod, Supplier<String> msgSupplier) {
            logger.logp(level, sourceClass, sourceMethod, msgSupplier);
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object param1) {
            logger.logp(level, sourceClass, sourceMethod, msg, param1);
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object[] params) {
            logger.logp(level, sourceClass, sourceMethod, msg, params);
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod, String msg, Throwable thrown) {
            logger.logp(level, sourceClass, sourceMethod, msg, thrown);
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod, Throwable thrown, Supplier<String> msgSupplier) {
            logger.logp(level, sourceClass, sourceMethod, thrown, msgSupplier);
        }

        @Override
        @Deprecated
        public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg) {
            logger.logrb(level, sourceClass, sourceMethod, bundleName, msg);
        }

        @Override
        @Deprecated
        public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object param1) {
            logger.logrb(level, sourceClass, sourceMethod, bundleName, msg, param1);
        }

        @Override
        @Deprecated
        public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object[] params) {
            logger.logrb(level, sourceClass, sourceMethod, bundleName, msg, params);
        }

        @Override
        public void logrb(Level level, String sourceClass, String sourceMethod, ResourceBundle bundle, String msg, Object... params) {
            logger.logrb(level, sourceClass, sourceMethod, bundle, msg, params);
        }

        @Override
        @Deprecated
        public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Throwable thrown) {
            logger.logrb(level, sourceClass, sourceMethod, bundleName, msg, thrown);
        }

        @Override
        public void logrb(Level level, String sourceClass, String sourceMethod, ResourceBundle bundle, String msg, Throwable thrown) {
            logger.logrb(level, sourceClass, sourceMethod, bundle, msg, thrown);
        }

        @Override
        public void entering(String sourceClass, String sourceMethod) {
            logger.entering(sourceClass, sourceMethod);
        }

        @Override
        public void entering(String sourceClass, String sourceMethod, Object param1) {
            logger.entering(sourceClass, sourceMethod, param1);
        }

        @Override
        public void entering(String sourceClass, String sourceMethod, Object[] params) {
            logger.entering(sourceClass, sourceMethod, params);
        }

        @Override
        public void exiting(String sourceClass, String sourceMethod) {
            logger.exiting(sourceClass, sourceMethod);
        }

        @Override
        public void exiting(String sourceClass, String sourceMethod, Object result) {
            logger.exiting(sourceClass, sourceMethod, result);
        }

        @Override
        public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
            logger.throwing(sourceClass, sourceMethod, thrown);
        }

        @Override
        public void severe(String msg) {
            logger.severe(msg);
        }

        @Override
        public void warning(String msg) {
            logger.warning(msg);
        }

        @Override
        public void info(String msg) {
            logger.info(msg);
        }

        @Override
        public void config(String msg) {
            logger.config(msg);
        }

        @Override
        public void fine(String msg) {
            logger.fine(msg);
        }

        @Override
        public void finer(String msg) {
            logger.finer(msg);
        }

        @Override
        public void finest(String msg) {
            logger.finest(msg);
        }

        @Override
        public void severe(Supplier<String> msgSupplier) {
            logger.severe(msgSupplier);
        }

        @Override
        public void warning(Supplier<String> msgSupplier) {
            logger.warning(msgSupplier);
        }

        @Override
        public void info(Supplier<String> msgSupplier) {
            logger.info(msgSupplier);
        }

        @Override
        public void config(Supplier<String> msgSupplier) {
            logger.config(msgSupplier);
        }

        @Override
        public void fine(Supplier<String> msgSupplier) {
            logger.fine(msgSupplier);
        }

        @Override
        public void finer(Supplier<String> msgSupplier) {
            logger.finer(msgSupplier);
        }

        @Override
        public void finest(Supplier<String> msgSupplier) {
            logger.finest(msgSupplier);
        }

        @Override
        public void setLevel(Level newLevel) throws SecurityException {
            try {
                logger.setLevel(newLevel);
            } catch (Throwable ignored) { }
        }

        @Override
        public Level getLevel() {
            return logger.getLevel();
        }

        @Override
        public boolean isLoggable(Level level) {
            return logger.isLoggable(level);
        }

        @Override
        public String getName() {
            return logger.getName();
        }

        @Override
        public void addHandler(Handler handler) throws SecurityException {
            logger.addHandler(handler);
        }

        @Override
        public void removeHandler(Handler handler) throws SecurityException {
            logger.removeHandler(handler);
        }

        @Override
        public Handler[] getHandlers() {
            return logger.getHandlers();
        }

        @Override
        public void setUseParentHandlers(boolean useParentHandlers) {
            logger.setUseParentHandlers(useParentHandlers);
        }

        @Override
        public boolean getUseParentHandlers() {
            return logger.getUseParentHandlers();
        }

        @Override
        public void setResourceBundle(ResourceBundle bundle) {
            logger.setResourceBundle(bundle);
        }

        @Override
        public Logger getParent() {
            return logger.getParent();
        }

        @Override
        public void setParent(Logger parent) {
            try {
                logger.setParent(parent);
            } catch (Throwable ignored) { }
        }
    }
}
