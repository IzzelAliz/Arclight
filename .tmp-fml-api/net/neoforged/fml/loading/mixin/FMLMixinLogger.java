/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.mixin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;

/**
 * NOTE: This class needs to be public due to MixinExtras Logger adapter making indirect use of it:
 * <a href="https://github.com/LlamaLad7/MixinExtras/blob/b2716c2c176966c1dbfc517b9b7e403a509f6120/src/main/java/com/llamalad7/mixinextras/utils/MixinExtrasLogger.java#L38">MixinExtrasLogger.java#L38</a>
 */
public class FMLMixinLogger implements ILogger {
    /**
     * Maps from ordinal of the Mixin LEVEL enum to Log4j2 levels.
     * Since Mixin is kinda modeled around log4j2, these will match 1:1.
     */
    private static final org.apache.logging.log4j.Level[] LEVELS = {
            org.apache.logging.log4j.Level.FATAL,
            org.apache.logging.log4j.Level.ERROR,
            org.apache.logging.log4j.Level.WARN,
            org.apache.logging.log4j.Level.INFO,
            org.apache.logging.log4j.Level.DEBUG,
            org.apache.logging.log4j.Level.TRACE
    };

    private final Logger logger;

    public FMLMixinLogger(String name) {
        logger = LogManager.getLogger(name);
    }

    @Override
    public String getId() {
        return logger.getName();
    }

    @Override
    public String getType() {
        return "Log4j2 (via FML)";
    }

    @Override
    public void catching(Level level, Throwable t) {
        logger.catching(LEVELS[level.ordinal()], t);
    }

    @Override
    public void catching(Throwable t) {
        logger.catching(t);
    }

    @Override
    public void debug(String message, Object... params) {
        logger.debug(message, params);
    }

    @Override
    public void debug(String message, Throwable t) {
        logger.debug(message, t);
    }

    @Override
    public void error(String message, Object... params) {
        logger.error(message, params);
    }

    @Override
    public void error(String message, Throwable t) {
        logger.error(message, t);
    }

    @Override
    public void fatal(String message, Object... params) {
        logger.fatal(message, params);
    }

    @Override
    public void fatal(String message, Throwable t) {
        logger.fatal(message, t);
    }

    @Override
    public void info(String message, Object... params) {
        logger.info(message, params);
    }

    @Override
    public void info(String message, Throwable t) {
        logger.info(message, t);
    }

    @Override
    public void log(Level level, String message, Object... params) {
        logger.log(LEVELS[level.ordinal()], message, params);
    }

    @Override
    public void log(Level level, String message, Throwable t) {
        logger.log(LEVELS[level.ordinal()], message, t);
    }

    @Override
    public <T extends Throwable> T throwing(T t) {
        return logger.throwing(t);
    }

    @Override
    public void trace(String message, Object... params) {
        logger.trace(message, params);
    }

    @Override
    public void trace(String message, Throwable t) {
        logger.trace(message, t);
    }

    @Override
    public void warn(String message, Object... params) {
        logger.warn(message, params);
    }

    @Override
    public void warn(String message, Throwable t) {
        logger.warn(message, t);
    }
}
