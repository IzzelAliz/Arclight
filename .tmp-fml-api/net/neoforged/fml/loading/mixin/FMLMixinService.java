/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.mixin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IAdviceProvider;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.ReEntranceLock;

public class FMLMixinService implements IMixinService {
    private final ConcurrentHashMap<String, FMLMixinLogger> loggers = new ConcurrentHashMap<>();
    private final ReEntranceLock lock = new ReEntranceLock(1);

    /**
     * Class provider, either uses hacky internals or provided service
     */
    private IClassProvider classProvider;

    /**
     * Bytecode provider, either uses hacky internals or provided service
     */
    @Nullable
    private IClassBytecodeProvider bytecodeProvider;

    /**
     * Class tracker, tracks class loads and registered invalid classes
     */
    private final FMLClassTracker classTracker = new FMLClassTracker();

    private final FMLAuditTrail auditTrail = new FMLAuditTrail();

    private final IFeatureValidator featureValidator = new FMLMixinFeatureValidator();

    private final IAdviceProvider adviceProvider = new FMLMixinAdviceProvider();

    private final Map<String, byte[]> mixinConfigContents = new HashMap<>();

    @Nullable
    private IMixinTransformer mixinTransformer;

    private final ContainerHandleVirtual primaryContainer = new ContainerHandleVirtual("fml");

    private final List<IContainerHandle> mixinContainers = new ArrayList<>();

    @Override
    public void prepare() {}

    @Override
    public Phase getInitialPhase() {
        return Phase.PREINIT;
    }

    @Override
    public void init() {}

    @Override
    public void beginPhase() {}

    @Override
    public void checkEnv(Object bootSource) {}

    @Override
    public ReEntranceLock getReEntranceLock() {
        return lock;
    }

    @Override
    public String getSideName() {
        return switch (FMLLoader.getCurrent().getDist()) {
            case CLIENT -> Constants.SIDE_CLIENT;
            case DEDICATED_SERVER -> Constants.SIDE_SERVER;
        };
    }

    public void setBytecodeProvider(@Nullable IClassBytecodeProvider bytecodeProvider) {
        this.bytecodeProvider = bytecodeProvider;
    }

    @Override
    public void offer(IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory mixinTransformerFactory) {
            this.mixinTransformer = mixinTransformerFactory.createTransformer();
        }
    }

    @Override
    public String getName() {
        return "FML";
    }

    @Override
    public CompatibilityLevel getMinCompatibilityLevel() {
        return CompatibilityLevel.JAVA_21;
    }

    @Override
    public CompatibilityLevel getMaxCompatibilityLevel() {
        return null;
    }

    @Override
    public ILogger getLogger(String name) {
        return loggers.computeIfAbsent(name, FMLMixinLogger::new);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        if (this.classProvider == null) {
            this.classProvider = new FMLClassProvider();
        }
        return this.classProvider;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        if (this.bytecodeProvider == null) {
            throw new IllegalStateException("Service initialisation incomplete, launch plugin was not created");
        }
        return this.bytecodeProvider;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return null;
    }

    @Override
    public IClassTracker getClassTracker() {
        return this.classTracker;
    }

    FMLClassTracker getInternalClassTracker() {
        return this.classTracker;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return this.auditTrail;
    }

    FMLAuditTrail getInternalAuditTrail() {
        return this.auditTrail;
    }

    @Override
    public IFeatureValidator getFeatureValidator() {
        return this.featureValidator;
    }

    @Override
    public IAdviceProvider getAdviceProvider() {
        return this.adviceProvider;
    }

    public IMixinTransformer getMixinTransformer() {
        return Objects.requireNonNull(this.mixinTransformer);
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return List.of("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return primaryContainer;
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return mixinContainers;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        // Mixin doesn't close this stream, so we use something that doesn't hold OS resources.
        var content = mixinConfigContents.get(name);
        if (content == null) {
            try (var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
                if (stream != null) {
                    content = stream.readAllBytes();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return content != null ? new ByteArrayInputStream(content) : null;
    }

    public void addMixinConfigContent(String config, byte[] resource) {
        mixinConfigContents.put(config, resource);
    }

    public void addMixinContainer(IContainerHandle handle) {
        this.mixinContainers.add(handle);
    }

    @VisibleForTesting
    public void clearMixinContainers() {
        mixinContainers.clear();
    }
}
