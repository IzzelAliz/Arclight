/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.mixin;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.classloading.transformation.TransformingClassLoader;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileParser;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.service.MixinService;

/**
 * Encapsulates the code required to interact with Mixin.
 */
public final class MixinFacade implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(MixinFacade.class);

    private final FMLMixinClassProcessor classProcessor;
    private final FMLMixinService service;

    public MixinFacade() {
        if (FMLLoader.getCurrent().getDist() == null) {
            throw new IllegalStateException("The dist must be set before initializing Mixin");
        }

        System.setProperty("mixin.service", FMLMixinService.class.getName());
        System.setProperty("mixin.bootstrapService", FMLMixinServiceBootstrap.class.getName());

        MixinBootstrap.init();

        service = (FMLMixinService) MixinService.getService();
        this.classProcessor = new FMLMixinClassProcessor(service);
    }

    public FMLMixinClassProcessor getClassProcessor() {
        return classProcessor;
    }

    public void finishInitialization(LoadingModList loadingModList, TransformingClassLoader classLoader) {
        if (Thread.currentThread().getContextClassLoader() != classLoader) {
            throw new IllegalStateException("The class loader must be the context classloader in order to find the Mixin configurations.");
        }
        addMixins(loadingModList);

        // We must transition to DEFAULT phase for normal Mixins to be applied at all
        gotoPhase(MixinEnvironment.Phase.INIT);
        gotoPhase(MixinEnvironment.Phase.DEFAULT);

        MixinBootstrap.init();
        MixinBootstrap.getPlatform().inject();
    }

    private void gotoPhase(MixinEnvironment.Phase phase) {
        try {
            var m = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
            m.setAccessible(true);
            m.invoke(null, phase);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Increment to break compatibility; during a BC window, this should be set to the latest version. This is _not_ set
    // to COMPATIBILITY_LATEST, so that if mixin is bumped past a BC it does not break mods.
    @VisibleForTesting
    public static final int DEFAULT_BEHAVIOUR_VERSION = FabricUtil.COMPATIBILITY_0_17_1;
    @VisibleForTesting
    public static final ArtifactVersion HIGHEST_MIXIN_VERSION;
    @VisibleForTesting
    public static final ArtifactVersion LOWEST_MIXIN_VERSION;

    static {
        HIGHEST_MIXIN_VERSION = new DefaultArtifactVersion(Optional.ofNullable(FabricUtil.class.getModule().getDescriptor())
                .flatMap(ModuleDescriptor::version).map(ModuleDescriptor.Version::toString)
                .or(() -> Optional.ofNullable(FabricUtil.class.getPackage().getImplementationVersion()))
                .orElseThrow(() -> new IllegalStateException("Cannot determine version of currently running mixin")));
        int defaultMixinVersion = DEFAULT_BEHAVIOUR_VERSION;
        int patch = defaultMixinVersion % 1000;
        defaultMixinVersion /= 1000;
        int minor = defaultMixinVersion % 1000;
        defaultMixinVersion /= 1000;
        int major = defaultMixinVersion;
        LOWEST_MIXIN_VERSION = new DefaultArtifactVersion(major + "." + minor + "." + patch);
    }

    private void addMixins(LoadingModList modList) {
        record AnnotationInfo(IModFile modFile, int behaviorVersion) {}

        Map<String, AnnotationInfo> configAnnotationInfo = new HashMap<>();

        for (var modFileInfo : modList.getModFiles()) {
            var modFile = modFileInfo.getFile();
            for (var mixinConfig : modFile.getMixinConfigs()) {
                if (!areRequiredModsPresent(modFile, mixinConfig, modList)) {
                    continue;
                }

                // Validate the mixin version is supported
                if (!validateMixinBehavior(modFile, mixinConfig)) {
                    continue;
                }

                var currentInfo = new AnnotationInfo(modFile, calculateBehaviorVersion(mixinConfig.behaviorVersion()));
                var existingInfo = configAnnotationInfo.putIfAbsent(mixinConfig.config(), currentInfo);
                if (existingInfo != null && existingInfo.modFile() != modFile) {
                    ModLoader.addLoadingIssue(ModLoadingIssue.error(
                            "fml.modloadingissue.mixin.duplicate_config",
                            mixinConfig.config(),
                            existingInfo.modFile)
                            .withAffectedModFile(modFile));
                    continue;
                }

                // Grab the actual mixin config content from the Jar, which also allows us to print a nicer error if it's missing
                byte[] configContent;
                try {
                    configContent = modFile.getContents().readFile(mixinConfig.config());
                } catch (IOException ignored) {
                    configContent = null;
                }
                // NOTE: Archloom does not put common modules into the same MOD_CLASSES/modFolders group as the
                //       NeoForge module, which leads to our restriction on it coming from the same modfile failing.
                if (configContent == null) {
                    for (var otherModFile : modList.getAllModFiles()) {
                        if (otherModFile != modFile) {
                            try {
                                configContent = otherModFile.getContents().readFile(mixinConfig.config());
                            } catch (IOException ignored) {}
                            if (configContent != null) {
                                LOG.warn("Mod file {} declares mixin config {}, but it actually comes from {}",
                                        modFile, mixinConfig.config(), otherModFile);
                                break;
                            }
                        }
                    }
                }
                if (configContent == null) {
                    ModLoader.addLoadingIssue(ModLoadingIssue.error(
                            "fml.modloadingissue.mixin.missing_config",
                            mixinConfig.config())
                            .withAffectedModFile(modFile));
                    continue;
                }

                service.addMixinConfigContent(mixinConfig.config(), configContent);
                Mixins.addConfiguration(mixinConfig.config());
            }
        }

        // Besides mods, also add plugins and game libraries so Mixin can scan their manifest attributes
        for (var plugin : modList.getPlugins()) {
            service.addMixinContainer(new FMLModFileContainerHandle(plugin.getFile()));
        }
        for (var gameLibrary : modList.getGameLibraries()) {
            service.addMixinContainer(new FMLModFileContainerHandle(gameLibrary));
        }

        var configMap = Mixins.getConfigs().stream().collect(Collectors.toMap(Config::getName, Config::getConfig));
        for (var entry : configAnnotationInfo.entrySet()) {
            var mixinConfigName = entry.getKey();
            var annotationInfo = entry.getValue();

            var config = configMap.get(mixinConfigName);
            if (config == null) {
                LOG.error("Mixin config {} from {} was not registered!", mixinConfigName, annotationInfo.modFile());
            } else {
                config.decorate(FabricUtil.KEY_MOD_ID, annotationInfo.modFile().getId());
                config.decorate(FabricUtil.KEY_COMPATIBILITY, annotationInfo.behaviorVersion());
            }
        }
    }

    private static boolean areRequiredModsPresent(ModFile modFile, ModFileParser.MixinConfig mixinConfig, LoadingModList modList) {
        for (var requiredModId : mixinConfig.requiredMods()) {
            if (modList.getModFileById(requiredModId) == null) {
                LOG.info("Mixin config {} from {} not applied as required mod '{}' is missing", mixinConfig.config(), modFile, requiredModId);
                return false;
            }
        }
        return true;
    }

    private static boolean validateMixinBehavior(ModFile modFile, ModFileParser.MixinConfig mixinConfig) {
        var behaviorVersion = mixinConfig.behaviorVersion();
        if (behaviorVersion == null) {
            return true; // Just uses default
        }

        if (behaviorVersion.compareTo(HIGHEST_MIXIN_VERSION) > 0) {
            ModLoader.addLoadingIssue(ModLoadingIssue.error(
                    "fml.modloadingissue.mixin.requested_behavior_too_new",
                    mixinConfig.config(),
                    behaviorVersion,
                    HIGHEST_MIXIN_VERSION).withAffectedModFile(modFile));
            return false;
        } else if (behaviorVersion.compareTo(LOWEST_MIXIN_VERSION) < 0) {
            ModLoader.addLoadingIssue(ModLoadingIssue.error(
                    "fml.modloadingissue.mixin.requested_behavior_too_old",
                    mixinConfig.config(),
                    behaviorVersion,
                    LOWEST_MIXIN_VERSION).withAffectedModFile(modFile));
            return false;
        }
        return true;
    }

    private static int calculateBehaviorVersion(@Nullable ArtifactVersion behaviorVersion) {
        if (behaviorVersion == null) {
            return DEFAULT_BEHAVIOUR_VERSION;
        }
        return behaviorVersion.getMajorVersion() * (1000 * 1000) +
                behaviorVersion.getMinorVersion() * 1000 +
                behaviorVersion.getIncrementalVersion();
    }

    @Override
    public void close() {
        // The bytecode provider holds a static global strong reference to the entire class-loader chain
        // which will keep JAR files opened.
        service.setBytecodeProvider(null);
    }
}
