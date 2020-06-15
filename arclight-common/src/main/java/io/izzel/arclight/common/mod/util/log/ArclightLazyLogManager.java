package io.izzel.arclight.common.mod.util.log;

import java.util.Enumeration;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ArclightLazyLogManager extends LogManager {

    private volatile LogManager delegate;

    @Override
    public boolean addLogger(Logger logger) {
        tryGet();
        if (delegate != null) return delegate.addLogger(logger);
        return super.addLogger(logger);
    }

    @Override
    public Logger getLogger(String name) {
        tryGet();
        if (delegate != null) return delegate.getLogger(name);
        return super.getLogger(name);
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
