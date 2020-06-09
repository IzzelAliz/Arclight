package io.izzel.arclight.common.mod.util.log;

import io.izzel.arclight.i18n.ArclightLocale;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

public class ArclightI18nLogger extends ExtendedLoggerWrapper {

    public ArclightI18nLogger(ExtendedLogger logger) {
        super(logger, logger.getName(), logger.getMessageFactory());
    }

    public static Logger getLogger(String name) {
        return new ArclightI18nLogger((ExtendedLogger) LogManager.getLogger(name));
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, CharSequence message, Throwable t) {
        this.info("", "");
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message.toString()), t);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, Object message, Throwable t) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message.toString()), t);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, MessageSupplier msgSupplier, Throwable t) {
        super.logMessage(fqcn, level, marker, msgSupplier, t);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, Supplier<?> msgSupplier, Throwable t) {
        super.logMessage(fqcn, level, marker, msgSupplier, t);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Throwable t) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), t);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message));
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Object... params) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), params);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Object p0) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), p0);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Object p0, Object p1) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), p0, p1);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), p0, p1, p2);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), p0, p1, p2, p3);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), p0, p1, p2, p3, p4);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), p0, p1, p2, p3, p4, p5);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    protected void logMessage(String fqcn, Level level, Marker marker, String message, Supplier<?>... paramSuppliers) {
        super.logMessage(fqcn, level, marker, ArclightLocale.getInstance().get(message), paramSuppliers);
    }
}
