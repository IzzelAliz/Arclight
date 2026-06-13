/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.javafmlmod;

import static net.neoforged.fml.Logging.LOADING;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;

/**
 * Automatic eventbus subscriber - reads {@link EventBusSubscriber}
 * annotations and passes individual {@linkplain Method}s to the correct event bus
 * based on whether the event type inherits from {@link IModBusEvent}.
 */
public class AutomaticEventSubscriber {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Type AUTO_SUBSCRIBER = Type.getType(EventBusSubscriber.class);
    private static final Type MOD_TYPE = Type.getType(Mod.class);

    public static void inject(ModContainer mod, ModFileScanData scanData, Module layer) {
        if (scanData == null) return;
        LOGGER.debug(LOADING, "Attempting to inject @EventBusSubscriber classes into the eventbus for {}", mod.getModId());
        List<ModFileScanData.AnnotationData> ebsTargets = scanData.getAnnotations().stream().filter(annotationData -> AUTO_SUBSCRIBER.equals(annotationData.annotationType())).collect(Collectors.toList());
        Map<String, String> modids = scanData.getAnnotations().stream().filter(annotationData -> MOD_TYPE.equals(annotationData.annotationType())).collect(Collectors.toMap(a -> a.clazz().getClassName(), a -> (String) a.annotationData().get("value")));

        ebsTargets.forEach(ad -> {
            EnumSet<Dist> sides = getSides(ad.annotationData().get("value"));
            String modId = (String) ad.annotationData().getOrDefault("modid", modids.getOrDefault(ad.clazz().getClassName(), mod.getModId()));
            if (Objects.equals(mod.getModId(), modId) && sides.contains(FMLLoader.getCurrent().getDist())) {
                LOGGER.debug(LOADING, "Scanning class {} for @SubscribeEvent-annotated methods", ad.clazz().getClassName());

                try {
                    var clazz = Class.forName(ad.clazz().getClassName(), true, layer.getClassLoader());

                    for (Method method : clazz.getDeclaredMethods()) {
                        if (!method.isAnnotationPresent(SubscribeEvent.class)) {
                            continue;
                        }

                        if (!Modifier.isStatic(method.getModifiers())) {
                            throw new IllegalArgumentException("Method " + method + " annotated with @SubscribeEvent is not static");
                        }

                        if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                            throw new IllegalArgumentException("Method " + method + " annotated with @SubscribeEvent must have only one parameter that is an Event subtype");
                        }

                        var eventType = method.getParameterTypes()[0];

                        if (IModBusEvent.class.isAssignableFrom(eventType)) {
                            var modBus = mod.getEventBus();
                            if (modBus == null) {
                                throw new IllegalArgumentException("Method " + method + " attempted to register a mod bus event, but mod " + mod.getModId() + " has no event bus");
                            } else {
                                LOGGER.debug(LOADING, "Subscribing method {} to the event bus of mod {}", method, mod.getModId());
                                modBus.register(method);
                            }
                        } else {
                            LOGGER.debug(LOADING, "Subscribing method {} to the game event bus", method);
                            FMLLoader.getCurrent().getBindings().getGameBus().register(method);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.fatal(LOADING, "Failed to register class {} with @EventBusSubscriber annotation", ad.clazz(), e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static EnumSet<Dist> getSides(Object data) {
        if (data == null) {
            return EnumSet.allOf(Dist.class);
        } else {
            return ((List<ModAnnotation.EnumHolder>) data).stream().map(eh -> Dist.valueOf(eh.value())).collect(Collectors.toCollection(() -> EnumSet.noneOf(Dist.class)));
        }
    }
}
