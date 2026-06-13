/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.util;

import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IOrderedProvider;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApiStatus.Internal
public final class ServiceLoaderUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLoaderUtil.class);

    private ServiceLoaderUtil() {}

    public static <T> List<T> loadServices(ILaunchContext context, Class<T> serviceClass) {
        return loadServices(context, serviceClass, List.of());
    }

    public static <T> List<T> loadServices(ILaunchContext context, Class<T> serviceClass, Predicate<Class<? extends T>> filter) {
        return loadServices(context, serviceClass, List.of(), filter);
    }

    public static <T> List<T> loadServices(ILaunchContext context, Class<T> serviceClass, Collection<T> additionalServices) {
        return loadServices(context, serviceClass, additionalServices, ignored -> true);
    }

    /**
     * Same as {@link #loadServices}, but it also marks any jar file that provided such services as located to prevent it
     * from being located again as a mod-file or library later.
     */
    public static <T> List<T> loadEarlyServices(ILaunchContext context, Class<T> serviceClass, Collection<T> additionalServices) {
        var services = loadServices(context, serviceClass, additionalServices, ignored -> true);

        for (var service : services) {
            var codeSource = service.getClass().getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                try {
                    context.addLocated(Path.of(codeSource.getLocation().toURI()));
                } catch (IllegalArgumentException | FileSystemNotFoundException | URISyntaxException ignored) {}
            }
        }

        return services;
    }

    /**
     * @param serviceClass If the service class implements {@link IOrderedProvider}, the services will automatically be sorted.
     */
    public static <T> List<T> loadServices(ILaunchContext context,
            Class<T> serviceClass,
            Collection<T> additionalServices,
            Predicate<Class<? extends T>> filter) {
        var serviceLoaderServices = context.loadServices(serviceClass)
                .filter(p -> {
                    if (!filter.test(p.type())) {
                        LOGGER.debug("Filtering out service provider {} for service class {}", p.type(), serviceClass);
                        return false;
                    }
                    return true;
                })
                .map(p -> {
                    try {
                        return p.get();
                    } catch (ServiceConfigurationError sce) {
                        LOGGER.error("Failed to load implementation for {}", serviceClass, sce);
                        return null;
                    }
                }).filter(Objects::nonNull);

        var servicesStream = Stream.concat(additionalServices.stream(), serviceLoaderServices).distinct();

        var applyPriority = IOrderedProvider.class.isAssignableFrom(serviceClass);
        if (applyPriority) {
            servicesStream = servicesStream.sorted(Comparator.comparingInt(service -> ((IOrderedProvider) service).getPriority()).reversed());
        }

        var services = servicesStream.toList();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(LogMarkers.CORE, "Found {} implementations of {}:", services.size(), serviceClass.getSimpleName());
            for (T service : services) {
                String priorityPrefix = "";
                if (applyPriority) {
                    priorityPrefix = String.format(Locale.ROOT, "%8d - ", ((IOrderedProvider) service).getPriority());
                }

                if (additionalServices.contains(service)) {
                    LOGGER.debug(LogMarkers.CORE, "\t{}[built-in] {}", priorityPrefix, identifyService(service));
                } else {
                    LOGGER.debug(LogMarkers.CORE, "\t{}{}", priorityPrefix, identifyService(service));
                }
            }
        }

        return services;
    }

    private static String identifyService(Object o) {
        var sourcePath = identifySourcePath(o);
        return o.getClass().getName() + " from " + sourcePath;
    }

    /**
     * Given any object, this method tries to build a human-readable chain of paths that identify where the
     * code implementing the given object is coming from.
     */
    public static String identifySourcePath(Object object) {
        var codeLocation = object.getClass().getProtectionDomain().getCodeSource().getLocation();
        Path fsLocation;
        try {
            fsLocation = Path.of(codeLocation.toURI());
        } catch (URISyntaxException e) {
            return codeLocation.toString();
        }
        return PathPrettyPrinting.prettyPrint(fsLocation);
    }
}
