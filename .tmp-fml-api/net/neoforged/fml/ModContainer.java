/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import static net.neoforged.fml.Logging.LOADING;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The container that wraps around mods in the system.
 * <p>
 * The philosophy is that individual mod implementation technologies should not
 * impact the actual loading and management of mod code. This class provides
 * a mechanism by which we can wrap actual mod code so that the loader and other
 * facilities can treat mods at arms length.
 * </p>
 *
 * @author cpw
 *
 */

public abstract class ModContainer {
    private static final Logger LOGGER = LogManager.getLogger();

    protected final String modId;
    protected final String namespace;
    protected final IModInfo modInfo;
    protected final Map<Class<? extends IExtensionPoint>, Supplier<?>> extensionPoints = new IdentityHashMap<>();

    public ModContainer(IModInfo info) {
        this.modId = info.getModId();
        // TODO: Currently not reading namespace from configuration..
        this.namespace = this.modId;
        this.modInfo = info;
    }

    /**
     * @return the modid for this mod
     */
    public final String getModId() {
        return modId;
    }

    /**
     * @return the resource prefix for the mod
     */
    public final String getNamespace() {
        return namespace;
    }

    public IModInfo getModInfo() {
        return modInfo;
    }

    @SuppressWarnings("unchecked")
    public <T extends IExtensionPoint> Optional<T> getCustomExtension(Class<T> point) {
        return Optional.ofNullable((T) extensionPoints.getOrDefault(point, () -> null).get());
    }

    /**
     * Registers an {@link IExtensionPoint} with the mod container.
     */
    public <T extends IExtensionPoint> void registerExtensionPoint(Class<T> point, T extension) {
        extensionPoints.put(point, () -> extension);
    }

    /**
     * Registers an {@link IExtensionPoint} with the mod container.
     * This overload allows passing a supplier that will only be evaluated when the extension is requested.
     */
    public <T extends IExtensionPoint> void registerExtensionPoint(Class<T> point, Supplier<T> extension) {
        extensionPoints.put(point, extension);
    }

    /**
     * Adds a {@link ModConfig} with the given type and spec. An empty config spec will be ignored and a debug line will
     * be logged.
     * 
     * @param type       The type of config
     * @param configSpec A config spec
     */
    public void registerConfig(ModConfig.Type type, IConfigSpec configSpec) {
        if (configSpec.isEmpty()) {
            // This handles the case where a mod tries to register a config, without any options configured inside it.
            LOGGER.debug("Attempted to register an empty config for type {} on mod {}", type, modId);
            return;
        }

        ConfigTracker.INSTANCE.registerConfig(type, configSpec, this);
    }

    /**
     * Adds a {@link ModConfig} with the given type, spec, and overridden file name. An empty config spec will be
     * ignored and a debug line will be logged.
     * 
     * @param type       The type of config
     * @param configSpec A config spec
     */
    public void registerConfig(ModConfig.Type type, IConfigSpec configSpec, String fileName) {
        if (configSpec.isEmpty()) {
            // This handles the case where a mod tries to register a config, without any options configured inside it.
            LOGGER.debug("Attempted to register an empty config for type {} on mod {} using file name {}", type, modId, fileName);
            return;
        }

        ConfigTracker.INSTANCE.registerConfig(type, configSpec, this, fileName);
    }

    /**
     * Function invoked by FML to construct the mod,
     * right before the dispatch of {@link FMLConstructModEvent}.
     */
    @ApiStatus.OverrideOnly
    protected void constructMod() {}

    /**
     * {@return the event bus for this mod, if available}
     *
     * <p>Not all mods have an event bus!
     *
     * @implNote For custom mod container implementations, the event bus must be built with
     *           {@link BusBuilder#allowPerPhasePost()} or posting via {@link #acceptEvent(EventPriority, Event)} will throw!
     */
    @Nullable
    public abstract IEventBus getEventBus();

    /**
     * Accept an arbitrary event for processing by the mod. Posted to {@link #getEventBus()}.
     * 
     * @param e Event to accept
     */
    public final <T extends Event & IModBusEvent> void acceptEvent(T e) {
        IEventBus bus = getEventBus();
        if (bus == null) return;

        try {
            LOGGER.trace(LOADING, "Firing event for modid {} : {}", this.getModId(), e);
            bus.post(e);
            LOGGER.trace(LOADING, "Fired event for modid {} : {}", this.getModId(), e);
        } catch (Throwable t) {
            LOGGER.error(LOADING, "Caught exception during event {} dispatch for modid {}", e, this.getModId(), t);
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.errorduringevent", e.getClass().getName()).withAffectedMod(modInfo).withCause(t));
        }
    }

    /**
     * Accept an arbitrary event for processing by the mod. Posted to {@link #getEventBus()}.
     * 
     * @param e Event to accept
     */
    public final <T extends Event & IModBusEvent> void acceptEvent(EventPriority phase, T e) {
        IEventBus bus = getEventBus();
        if (bus == null) return;

        try {
            LOGGER.trace(LOADING, "Firing event for phase {} for modid {} : {}", phase, this.getModId(), e);
            bus.post(phase, e);
            LOGGER.trace(LOADING, "Fired event for phase {} for modid {} : {}", phase, this.getModId(), e);
        } catch (Throwable t) {
            LOGGER.error(LOADING, "Caught exception during event {} dispatch for modid {}", e, this.getModId(), t);
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.errorduringevent", e.getClass().getName()).withAffectedMod(modInfo).withCause(t));
        }
    }
}
