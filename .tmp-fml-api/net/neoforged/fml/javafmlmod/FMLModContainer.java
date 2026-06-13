/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.javafmlmod;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.EventBusErrorMessage;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class FMLModContainer extends ModContainer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker LOADING = MarkerManager.getMarker("LOADING");
    private final ModFileScanData scanResults;
    private final IEventBus eventBus;
    private final List<Class<?>> modClasses;
    private final Module layer;

    public FMLModContainer(IModInfo info, List<String> entrypoints, ModFileScanData modFileScanResults, ModuleLayer gameLayer) {
        super(info);
        LOGGER.debug(LOADING, "Creating FMLModContainer instance for {} with entrypoints {}", info.getModId(), entrypoints);
        this.scanResults = modFileScanResults;
        this.eventBus = BusBuilder.builder()
                .setExceptionHandler(this::onEventFailed)
                .markerType(IModBusEvent.class)
                .allowPerPhasePost()
                .build();
        this.layer = gameLayer.findModule(info.getOwningFile().getFile().getId()).orElseThrow();

        var context = ModLoadingContext.get();
        try {
            context.setActiveContainer(this);

            modClasses = new ArrayList<>();

            for (var entrypoint : entrypoints) {
                try {
                    var cls = Class.forName(layer, entrypoint);
                    if (cls == null) {
                        throw new ClassNotFoundException("Class '" + entrypoint + "' could not be found");
                    }
                    modClasses.add(cls);
                    LOGGER.trace(LOADING, "Loaded modclass {} with {}", cls.getName(), cls.getClassLoader());
                } catch (Throwable e) {
                    LOGGER.error(LOADING, "Failed to load class {}", entrypoint, e);
                    throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.failedtoloadmodclass").withCause(e).withAffectedMod(info));
                }
            }
        } finally {
            context.setActiveContainer(null);
        }
    }

    private void onEventFailed(IEventBus iEventBus, Event event, EventListener[] iEventListeners, int i, Throwable throwable) {
        LOGGER.error(new EventBusErrorMessage(event, i, iEventListeners, throwable));
    }

    @Override
    protected void constructMod() {
        for (var modClass : modClasses) {
            try {
                var constructors = modClass.getConstructors();
                if (constructors.length != 1) {
                    throw new RuntimeException("Mod class " + modClass + " must have exactly 1 public constructor, found " + constructors.length);
                }
                var constructor = constructors[0];

                // Allowed arguments for injection via constructor
                Map<Class<?>, Object> allowedConstructorArgs = Map.of(
                        IEventBus.class, eventBus,
                        ModContainer.class, this,
                        FMLModContainer.class, this,
                        Dist.class, FMLLoader.getCurrent().getDist());

                var parameterTypes = constructor.getParameterTypes();
                Object[] constructorArgs = new Object[parameterTypes.length];
                Set<Class<?>> foundArgs = new HashSet<>();

                for (int i = 0; i < parameterTypes.length; i++) {
                    Object argInstance = allowedConstructorArgs.get(parameterTypes[i]);
                    if (argInstance == null) {
                        throw new RuntimeException("Mod constructor has unsupported argument " + parameterTypes[i] + ". Allowed optional argument classes: " +
                                allowedConstructorArgs.keySet().stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));
                    }

                    if (foundArgs.contains(parameterTypes[i])) {
                        throw new RuntimeException("Duplicate mod constructor argument type: " + parameterTypes[i]);
                    }

                    foundArgs.add(parameterTypes[i]);
                    constructorArgs[i] = argInstance;
                }

                // All arguments are found
                constructor.newInstance(constructorArgs);

                LOGGER.trace(LOADING, "Loaded mod instance {} of type {}", getModId(), modClass.getName());
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException) e = e.getCause(); // exceptions thrown when a reflected method call throws are wrapped in an InvocationTargetException. However, this isn't useful for the end user who has to dig through the logs to find the actual cause.
                LOGGER.error(LOADING, "Failed to create mod instance. ModID: {}, class {}", getModId(), modClass.getName(), e);
                throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.failedtoloadmod").withCause(e).withAffectedMod(modInfo));
            }
        }
        try {
            LOGGER.trace(LOADING, "Injecting Automatic event subscribers for {}", getModId());
            AutomaticEventSubscriber.inject(this, this.scanResults, layer);
            LOGGER.trace(LOADING, "Completed Automatic event subscribers for {}", getModId());
        } catch (Throwable e) {
            LOGGER.error(LOADING, "Failed to register automatic subscribers. ModID: {}", getModId(), e);
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.failedtoloadmod").withCause(e).withAffectedMod(modInfo));
        }
    }

    @Override
    public IEventBus getEventBus() {
        return this.eventBus;
    }
}
