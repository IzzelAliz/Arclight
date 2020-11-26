package io.izzel.arclight.common.mod.util.log;

import java.util.Enumeration;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ArclightLazyLogManager extends LogManager {

    private static final String SECURITY_LOGGER_NAME = "jdk.event.security";
    private volatile LogManager delegate;

    @Override
    public boolean addLogger(Logger logger) {
        tryGet();
        if (SECURITY_LOGGER_NAME.equals(logger.getName())) return true;
        if (delegate != null) return delegate.addLogger(logger);
        return super.addLogger(logger);
    }

    @Override
    public Logger getLogger(String name) {
        tryGet();
        if (delegate != null && !SECURITY_LOGGER_NAME.equals(name)) return delegate.getLogger(name);
        return Logger.getGlobal();
    }

    @Override
    public Enumeration<String> getLoggerNames() {
        tryGet();
        if (delegate != null) return delegate.getLoggerNames();
        return super.getLoggerNames();
    }

    private void tryGet() {
        if (delegate != null) return;
        try {
            Class<?> name = Class.forName("org.apache.logging.log4j.jul.LogManager");
            delegate = (LogManager) name.newInstance();
        } catch (Exception ignored) {
        }
    }
}
