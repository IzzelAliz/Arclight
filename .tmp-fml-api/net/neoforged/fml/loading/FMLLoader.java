/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import com.mojang.logging.LogUtils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.neoforged.accesstransformer.api.AccessTransformerEngine;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.FMLVersion;
import net.neoforged.fml.IBindingsProvider;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.classloading.JarContentsModule;
import net.neoforged.fml.classloading.JarContentsModuleFinder;
import net.neoforged.fml.classloading.ResourceMaskingClassLoader;
import net.neoforged.fml.classloading.transformation.ClassProcessorAuditLog;
import net.neoforged.fml.classloading.transformation.ClassProcessorAuditSource;
import net.neoforged.fml.classloading.transformation.ClassProcessorSet;
import net.neoforged.fml.classloading.transformation.TransformingClassLoader;
import net.neoforged.fml.common.asm.AccessTransformerService;
import net.neoforged.fml.common.asm.SimpleProcessorsGroup;
import net.neoforged.fml.common.asm.enumextension.RuntimeEnumExtender;
import net.neoforged.fml.i18n.FMLTranslations;
import net.neoforged.fml.jarcontents.CompositeJarContents;
import net.neoforged.fml.jarcontents.EmptyJarContents;
import net.neoforged.fml.jarcontents.FolderJarContents;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarcontents.JarFileContents;
import net.neoforged.fml.jarcontents.JarResource;
import net.neoforged.fml.loading.mixin.MixinFacade;
import net.neoforged.fml.loading.moddiscovery.ModDiscoverer;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.locators.GameLocator;
import net.neoforged.fml.loading.moddiscovery.locators.InDevFolderLocator;
import net.neoforged.fml.loading.moddiscovery.locators.InDevJarLocator;
import net.neoforged.fml.loading.moddiscovery.locators.ModsFolderLocator;
import net.neoforged.fml.loading.moddiscovery.locators.NeoForgeDevDistCleaner;
import net.neoforged.fml.loading.modscan.BackgroundScanHandler;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.fml.startup.InstrumentationHelper;
import net.neoforged.fml.startup.StartupArgs;
import net.neoforged.fml.util.ClasspathResourceUtils;
import net.neoforged.fml.util.PathPrettyPrinting;
import net.neoforged.fml.util.ServiceLoaderUtil;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.event.Level;

public final class FMLLoader implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final AtomicReference<@Nullable FMLLoader> current = new AtomicReference<>();

    /**
     * The context class-loader that will be restored when the loader is closed.
     */
    @Nullable
    private final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    /**
     * The current tail of the class-loader chain. It is moved whenever a new set of Jars is loaded.
     */
    private ClassLoader currentClassLoader;
    /**
     * Resources owned by this loader, such as opened URL classloaders, which
     * will be closed alongside the loader.
     */
    private final List<AutoCloseable> ownedResources = new ArrayList<>();
    /**
     * Additional user-supplied resources that will be closed, before this loader itself is closed.
     */
    private final List<AutoCloseable> closeCallbacks = new ArrayList<>();
    private final ProgramArgs programArgs;

    private LanguageProviderLoader languageProviderLoader;
    private final Dist dist;
    private LoadingModList loadingModList;
    private final Path gameDir;
    private final Set<Path> locatedPaths = new HashSet<>();

    private VersionInfo versionInfo;
    private VersionSupportMatrix versionSupportMatrix;
    public BackgroundScanHandler backgroundScanHandler;
    private final boolean production;
    @Nullable
    private ModuleLayer gameLayer;
    private final List<ModFile> earlyServicesJars = new ArrayList<>();
    @VisibleForTesting
    DiscoveryResult discoveryResult;
    private final ClassProcessorAuditLog classTransformerAuditLog = new ClassProcessorAuditLog();
    @Nullable
    @VisibleForTesting
    volatile IBindingsProvider bindings;

    @ApiStatus.Internal
    public ClassProcessorAuditSource getClassTransformerAuditLog() {
        return classTransformerAuditLog;
    }

    @VisibleForTesting
    record DiscoveryResult(
            List<ModFile> pluginContent,
            List<ModFile> gameContent,
            List<ModFile> gameLibraryContent,
            List<ModLoadingIssue> discoveryIssues) {
        public List<ModFile> allContent() {
            var content = new ArrayList<ModFile>(
                    pluginContent.size() + gameContent.size() + gameLibraryContent.size());
            content.addAll(pluginContent);
            content.addAll(gameContent);
            content.addAll(gameLibraryContent);
            return content;
        }

        public List<ModFile> allGameContent() {
            var content = new ArrayList<ModFile>(
                    gameContent.size() + gameLibraryContent.size());
            content.addAll(gameContent);
            content.addAll(gameLibraryContent);
            return content;
        }

        public boolean hasErrors() {
            return discoveryIssues.stream().anyMatch(i -> i.severity() == ModLoadingIssue.Severity.ERROR);
        }
    }

    private FMLLoader(ClassLoader currentClassLoader, String[] programArgs, Dist dist, boolean production, Path gameDir) {
        this.currentClassLoader = currentClassLoader;
        this.programArgs = ProgramArgs.from(programArgs);
        this.dist = dist;
        this.production = production;
        this.gameDir = gameDir;

        versionInfo = new VersionInfo(
                this.programArgs.remove("fml.neoForgeVersion"),
                this.programArgs.remove("fml.mcVersion"),
                this.programArgs.remove("fml.neoFormVersion"));

        LOGGER.info(
                "Starting FancyModLoader version {} ({} in {})",
                FMLVersion.getVersion(),
                dist,
                production ? "PROD" : "DEV");

        LOGGER.info("Game directory: {}", gameDir);

        makeCurrent();
    }

    /**
     * Tries to get the mod file that a given class belongs to.
     *
     * @return Null, if the class doesn't belong to a mod file.
     */
    @Nullable
    public IModFile getModFileByClass(Class<?> clazz) {
        var packageName = clazz.getPackageName();
        if (packageName.isEmpty()) {
            return null;
        }
        return loadingModList.getPackageIndex().get(packageName);
    }

    /**
     * Adds a callback that will be called once, when this loader is about to close.
     * <p>Mod files and class-loaders will not be closed yet when the callback is called, allowing for
     * class-loading to occur normally.
     */
    public void addCloseCallback(AutoCloseable callback) {
        closeCallbacks.add(callback);
    }

    @Override
    public void close() {
        LOGGER.info("Closing FML Loader {}", Integer.toHexString(System.identityHashCode(this)));

        for (var closeCallback : closeCallbacks) {
            try {
                closeCallback.close();
            } catch (Exception e) {
                LOGGER.error("Failed to run mod-supplied close callback {}", closeCallback, e);
            }
        }
        closeCallbacks.clear();

        for (var modFile : earlyServicesJars) {
            modFile.close();
        }
        earlyServicesJars.clear();

        if (loadingModList != null) {
            for (var modFile : loadingModList.getModFiles()) {
                modFile.getFile().close();
            }
            for (var modFile : loadingModList.getPlugins()) {
                ((ModFile) modFile.getFile()).close();
            }
            for (var modFile : loadingModList.getGameLibraries()) {
                ((ModFile) modFile).close();
            }
        }
        // Clean up some further shared state
        if (this == current.compareAndExchange(this, null)) {
            ModList.clear();
            ModLoader.clear();
        }

        for (var ownedResource : ownedResources) {
            try {
                ownedResource.close();
            } catch (Exception e) {
                LOGGER.error("Failed to close resource {} owned by FMLLoader", ownedResource, e);
            }
        }
        ownedResources.clear();

        if (Thread.currentThread().getContextClassLoader() == currentClassLoader) {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private void makeCurrent() {
        FMLLoader witness = current.compareAndExchange(null, this);
        if (witness != null) {
            throw new IllegalStateException("Another FML loader is already active: " + witness);
        }
    }

    public ClassLoader getCurrentClassLoader() {
        return currentClassLoader;
    }

    public ProgramArgs getProgramArgs() {
        return programArgs;
    }

    @ApiStatus.Internal
    public IBindingsProvider getBindings() {
        if (bindings == null) {
            synchronized (this) {
                if (bindings == null) {
                    if (gameLayer == null) {
                        throw new IllegalStateException("Cannot retrieve bindings before the game layer is initialized.");
                    }
                    var providers = ServiceLoader.load(gameLayer, IBindingsProvider.class).stream().toList();
                    if (providers.isEmpty()) {
                        throw new IllegalStateException("Could not find bindings provider");
                    } else if (providers.size() > 1) {
                        String providerList = providers.stream().map(p -> p.type().getName()).collect(Collectors.joining(", "));
                        throw new IllegalStateException("Found more than one bindings provider: " + providerList);
                    }
                    bindings = providers.getFirst().get();
                }
            }
        }
        return bindings;
    }

    public static FMLLoader create(StartupArgs startupArgs) {
        var instrumentation = InstrumentationHelper.obtainInstrumentation();
        return create(instrumentation, startupArgs);
    }

    public static FMLLoader create(@Nullable Instrumentation instrumentation, StartupArgs startupArgs) {
        // If a client class is available, then it's client, otherwise DEDICATED_SERVER
        // The auto-detection never detects JOINED since it's impossible to do so
        var initialLoader = Objects.requireNonNullElse(startupArgs.parentClassLoader(), ClassLoader.getSystemClassLoader());

        PathPrettyPrinting.addRoot(startupArgs.gameDirectory());

        var loader = new FMLLoader(
                initialLoader,
                startupArgs.programArgs(),
                Objects.requireNonNullElseGet(startupArgs.dist(), () -> detectDist(initialLoader)),
                detectProduction(initialLoader),
                startupArgs.gameDirectory());

        try {
            FMLPaths.loadAbsolutePaths(startupArgs.gameDirectory());
            FMLConfig.load();

            var launchContext = loader.new LaunchContextAdapter();
            for (var claimedFile : startupArgs.claimedFiles()) {
                launchContext.addLocated(claimedFile.toPath());
            }

            loader.loadEarlyServices(startupArgs);

            ImmediateWindowHandler.load(launchContext, startupArgs.headless(), loader.programArgs);
            // Report known versions no
            if (loader.versionInfo.neoForgeVersion() != null) {
                ImmediateWindowHandler.setNeoForgeVersion(loader.versionInfo.neoForgeVersion());
            }
            if (loader.versionInfo.mcVersion() != null) {
                ImmediateWindowHandler.setMinecraftVersion(loader.versionInfo.mcVersion());
            }

            DiscoveryResult discoveryResult;
            if (startupArgs.headless()) {
                discoveryResult = loader.runDiscovery();
            } else {
                discoveryResult = runOffThread(loader::runDiscovery);
            }
            for (var issue : discoveryResult.discoveryIssues()) {
                LOGGER.atLevel(issue.severity() == ModLoadingIssue.Severity.ERROR ? Level.ERROR : Level.WARN)
                        .setCause(issue.cause())
                        .log("{}", FMLTranslations.translateIssueEnglish(issue));
            }
            if (discoveryResult.hasErrors()) {
                throw new ModLoadingException(discoveryResult.discoveryIssues);
            }

            // Build all module descriptors in parallel
            discoveryResult.allContent().stream().parallel().forEach(ModFile::getModuleDescriptor);

            ClassLoadingGuardian classLoadingGuardian = null;
            if (instrumentation != null) {
                classLoadingGuardian = new ClassLoadingGuardian(instrumentation, discoveryResult.allGameContent());
                loader.ownedResources.add(classLoadingGuardian);
            }

            var mixinFacade = new MixinFacade();
            loader.ownedResources.add(mixinFacade);

            // Load Plugins
            loader.loadPlugins(loader.loadingModList.getPlugins());

            // Now go and build the language providers and let mods discover theirs
            loader.languageProviderLoader = new LanguageProviderLoader(launchContext);
            for (var modFile : discoveryResult.gameContent) {
                modFile.identifyLanguage();
            }

            // BUILD GAME LAYER
            var gameContent = new ArrayList<JarContentsModule>();
            for (var modFile : discoveryResult.allGameContent()) {
                gameContent.add(new JarContentsModule(
                        modFile.getContents(),
                        modFile.getModuleDescriptor()));
            }

            var classProcessorSet = createClassProcessorSet(startupArgs, launchContext, discoveryResult, mixinFacade);
            if (!classProcessorSet.getGeneratedPackages().isEmpty()) {
                var descriptor = ModuleDescriptor.newAutomaticModule(ClassProcessor.GENERATED_PACKAGE_MODULE)
                        .packages(classProcessorSet.getGeneratedPackages())
                        .build();
                gameContent.add(new JarContentsModule(
                        JarContents.empty(Path.of("VirtualJar/" + descriptor.name())),
                        descriptor));
            }
            var transformingLoader = loader.buildTransformingLoader(classProcessorSet, loader.classTransformerAuditLog, gameContent);

            // From here on out, try loading through the TCL
            if (classLoadingGuardian != null) {
                classLoadingGuardian.setAllowedClassLoader(transformingLoader);
            }

            // We're adding mixins *after* setting the Thread context classloader since
            // Mixin stubbornly loads Mixin Configs via its ModLauncher environment using the TCL.
            // Adding containers beforehand will try to load Mixin configs using the app classloader and fail.
            mixinFacade.finishInitialization(loader.loadingModList, transformingLoader);

            ImmediateWindowHandler.updateProgress("Launching minecraft");
            ImmediateWindowHandler.renderTick();

            return loader;
        } catch (RuntimeException | Error e) {
            try {
                loader.close();
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            throw e;
        }
    }

    private static ClassProcessorSet createClassProcessorSet(StartupArgs startupArgs,
            LaunchContextAdapter launchContext,
            DiscoveryResult discoveryResult,
            MixinFacade mixinFacade) {
        // Add our own launch plugins explicitly.
        var builtInProcessors = new ArrayList<ClassProcessor>();
        builtInProcessors.add(createAccessTransformerService(discoveryResult));
        builtInProcessors.add(new RuntimeEnumExtender());
        builtInProcessors.add(new SimpleProcessorsGroup());

        if (startupArgs.cleanDist()) {
            var minecraftModFile = discoveryResult.gameContent().stream()
                    .filter(mf -> mf.getId().equals("minecraft"))
                    .findFirst()
                    .map(ModFile::getContents)
                    .orElse(null);
            if (minecraftModFile != null && NeoForgeDevDistCleaner.supportsDistCleaning(minecraftModFile)) {
                builtInProcessors.add(new NeoForgeDevDistCleaner(minecraftModFile, startupArgs.dist()));
            }
        }

        builtInProcessors.add(mixinFacade.getClassProcessor());

        return ClassProcessorSet.builder()
                .markMarker(ClassProcessorIds.SIMPLE_PROCESSORS_GROUP)
                .markMarker(ClassProcessorIds.COMPUTING_FRAMES)
                .addProcessors(ServiceLoaderUtil.loadServices(launchContext, ClassProcessor.class, builtInProcessors))
                .addProcessorProviders(ServiceLoaderUtil.loadServices(launchContext, ClassProcessorProvider.class))
                .build();
    }

    private static ClassProcessor createAccessTransformerService(DiscoveryResult discoveryResult) {
        var engine = AccessTransformerEngine.newEngine();
        for (var modFile : discoveryResult.gameContent()) {
            for (var atPath : modFile.getAccessTransformers()) {
                LOGGER.debug("Adding Access Transformer {} in {}", atPath, modFile);
                try (var in = modFile.getContents().openFile(atPath)) {
                    if (in == null) {
                        LOGGER.error(LogMarkers.LOADING, "Access transformer file {} provided by {} does not exist!", atPath, modFile);
                    } else {
                        engine.loadAT(new InputStreamReader(new BufferedInputStream(in)), atPath);
                    }
                } catch (IOException e) {
                    // TODO: Convert to translated issue?
                    throw new RuntimeException("Failed to load AT at " + atPath + " from " + modFile, e);
                }
            }
        }
        return new AccessTransformerService(engine);
    }

    private TransformingClassLoader buildTransformingLoader(ClassProcessorSet classProcessorSet,
            ClassProcessorAuditLog auditTrail,
            List<JarContentsModule> content) {
        maskContentAlreadyOnClasspath(content);

        long start = System.currentTimeMillis();

        var parentLayers = List.of(ModuleLayer.boot());

        var cf = Configuration.resolveAndBind(
                new JarContentsModuleFinder(content),
                parentLayers.stream().map(ModuleLayer::configuration).toList(),
                ModuleFinder.of(),
                content.stream().map(JarContentsModule::moduleName).toList());

        var moduleNames = getModuleNameList(cf, content);
        LOGGER.info("Building game content classloader:\n{}", moduleNames);
        var loader = new TransformingClassLoader(classProcessorSet, auditTrail, cf, parentLayers, currentClassLoader);

        var layer = ModuleLayer.defineModules(
                cf,
                parentLayers,
                f -> loader).layer();

        var elapsed = System.currentTimeMillis() - start;
        LOGGER.info("Built game content classloader in {}ms", elapsed);

        loader.setFallbackClassLoader(currentClassLoader);

        gameLayer = layer;
        ownedResources.add(loader);
        currentClassLoader = loader;
        Thread.currentThread().setContextClassLoader(loader);
        return loader;
    }

    /**
     * If any location being added is already on the classpath, we add a masking classloader to ensure
     * that resources are not double-reported when using getResources/getResource.
     * <p>
     * The primary purpose of this is in mod and NeoForge development environments, where IDEs put the mod
     * on the app classpath, but we also add it as content to the game layer. This method is responsible
     * for setting up a classloader that prevents getResource/getResources from reporting Jar resources
     * for both the jar on the App classpath and on the transforming classloader.
     */
    private void maskContentAlreadyOnClasspath(List<JarContentsModule> content) {
        var classpathItems = ClasspathResourceUtils.getAllClasspathItems(currentClassLoader);

        // Collect all paths that make up the game content, which are already on the classpath
        Set<Path> needsMasking = new HashSet<>();
        for (var secureJar : content) {
            for (var basePath : getBasePaths(secureJar.contents(), true)) {
                if (classpathItems.contains(basePath)) {
                    needsMasking.add(basePath);
                }
            }
        }

        if (!needsMasking.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Masking classpath elements: {}", needsMasking.stream().map(PathPrettyPrinting::prettyPrint).toList());
            }

            var maskedLoader = new ResourceMaskingClassLoader(currentClassLoader, needsMasking);
            if (Thread.currentThread().getContextClassLoader() == currentClassLoader) {
                Thread.currentThread().setContextClassLoader(maskedLoader);
            }
            currentClassLoader = maskedLoader;
        }
    }

    private static List<Path> getBasePaths(JarContents contents, boolean ignoreFilter) {
        var result = new ArrayList<Path>();
        switch (contents) {
            case CompositeJarContents compositeModContainer -> {
                if (!ignoreFilter && compositeModContainer.isFiltered()) {
                    throw new IllegalStateException("Cannot load filtered Jar content into a URL classloader");
                }
                for (var delegate : compositeModContainer.getDelegates()) {
                    result.addAll(getBasePaths(delegate, ignoreFilter));
                }
            }
            case EmptyJarContents ignored -> {}
            case FolderJarContents folderModContainer -> result.add(folderModContainer.getPrimaryPath());
            case JarFileContents jarModContainer -> result.add(jarModContainer.getPrimaryPath());
            default -> throw new IllegalStateException("Don't know how to handle " + contents);
        }
        return result;
    }

    private static String getModuleNameList(Configuration cf, List<JarContentsModule> content) {
        var jarsById = content.stream().collect(Collectors.toMap(JarContentsModule::moduleName, Function.identity()));

        return cf.modules().stream()
                .map(module -> {
                    var jar = jarsById.get(module.name());
                    var contentList = jar != null ? jar.contents() : module.reference().location().map(URI::toString).orElse("unknown");
                    return " - " + module.name() + " (" + contentList + ")";
                })
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    private static Dist detectDist(ClassLoader classLoader) {
        var clientAvailable = classLoader.getResource("net/minecraft/client/main/Main.class") != null;
        return clientAvailable ? Dist.CLIENT : Dist.DEDICATED_SERVER;
    }

    private static boolean detectProduction(ClassLoader classLoader) {
        // Since Minecraft has been shipping unobfuscated since 26.1, we can no longer identify
        // that the patched NeoForge is on the classpath by looking for a class that would be
        // obfuscated in production.
        // The new heuristic is to load a Minecraft class that we know is patched by NF,
        // and check for net/neoforged/fml in the bytecode to verify this.
        try (var resource = classLoader.getResourceAsStream("net/minecraft/SharedConstants.class")) {
            if (resource == null) {
                return true; // Likely an error but missing classes will be reported by the game locator more competently
            }

            var resourceContent = resource.readAllBytes();
            var signature = "net/neoforged/fml".getBytes(StandardCharsets.UTF_8);
            // Slow, but whatever
            for (int i = 0; i < resourceContent.length - signature.length; i++) {
                boolean match = true;
                for (int j = 0; j < signature.length; j++) {
                    if (resourceContent[i + j] != signature[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    // Found signature -> we're definitely in dev
                    return false;
                }
            }
            return true; // didn't find patched-in NeoForge reference in bytecode -> not in dev
        } catch (IOException ignored) {
            return true; // Any error here will be caught later
        }
    }

    private void loadEarlyServices(StartupArgs startupArgs) {
        // Search for early services
        this.earlyServicesJars.addAll(EarlyServiceDiscovery.findEarlyServiceJars(startupArgs, FMLPaths.MODSDIR.get()));
        if (!earlyServicesJars.isEmpty()) {
            appendLoader("FML Early Services", earlyServicesJars.stream().map(IModFile::getContents).toList());
        }
    }

    private void loadPlugins(List<IModFileInfo> plugins) {
        appendLoader("FML Plugins", plugins.stream().map(mfi -> mfi.getFile().getContents()).toList());
    }

    /**
     * Loads the given services into a URL classloader.
     */
    private void appendLoader(String loaderName, List<JarContents> jars) {
        if (jars.isEmpty()) {
            LOGGER.info("No additional classpath items for {} were found.", loaderName);
            return;
        }

        LOGGER.info("Loading {}:", loaderName);

        List<URL> rootUrls = new ArrayList<>(jars.size());
        for (var jar : jars) {
            if (jar instanceof CompositeJarContents compositeJarContents && compositeJarContents.isFiltered()) {
                throw new IllegalArgumentException("Cannot use simple URLClassLoader for filtered content " + jar);
            }

            // TODO: Order on the classpath matters, we need to double-check the content roots are in the right order here
            for (var contentRoot : jar.getContentRoots()) {
                LOGGER.info(" - {}", PathPrettyPrinting.prettyPrint(contentRoot));
                try {
                    rootUrls.add(contentRoot.toUri().toURL());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e); // This should not happen for file URLs
                }
                locatedPaths.add(contentRoot); // Prevents it from getting picked up again
            }
        }

        var loader = new URLClassLoader(loaderName, rootUrls.toArray(URL[]::new), currentClassLoader);
        ownedResources.add(loader);
        currentClassLoader = loader;
        Thread.currentThread().setContextClassLoader(loader);
    }

    private DiscoveryResult runDiscovery() {
        var progress = StartupNotificationManager.prependProgressBar("Discovering mods...", 0);

        var additionalLocators = new ArrayList<IModFileCandidateLocator>();

        additionalLocators.add(new GameLocator());
        additionalLocators.add(new InDevFolderLocator());
        additionalLocators.add(new InDevJarLocator());
        additionalLocators.add(new ModsFolderLocator());

        var modDiscoverer = new ModDiscoverer(new LaunchContextAdapter(), additionalLocators);
        var discoveryResult = modDiscoverer.discoverMods(earlyServicesJars);

        // Now we should have a mod for "minecraft" and "neoforge" allowing us to fill in the versions
        var neoForgeVersion = versionInfo.neoForgeVersion();
        var minecraftVersion = versionInfo.mcVersion();
        for (var modFile : discoveryResult.modFiles()) {
            var mods = modFile.getModFileInfo().getMods();
            if (mods.isEmpty()) {
                continue;
            }
            var mainMod = mods.getFirst();
            switch (modFile.getId()) {
                case "minecraft" -> minecraftVersion = mainMod.getVersion().toString();
                case "neoforge" -> neoForgeVersion = mainMod.getVersion().toString();
            }
        }
        versionInfo = new VersionInfo(
                neoForgeVersion,
                minecraftVersion,
                getVersionInfo().neoFormVersion());
        versionSupportMatrix = new VersionSupportMatrix(versionInfo);
        progress.complete();

        ImmediateWindowHandler.setMinecraftVersion(versionInfo.mcVersion());
        ImmediateWindowHandler.setNeoForgeVersion(versionInfo.neoForgeVersion());

        loadingModList = ModSorter.sort(discoveryResult.modFiles(), discoveryResult.discoveryIssues());

        Map<IModInfo, JarResource> enumExtensionsByMod = new HashMap<>();
        for (var modFile : loadingModList.getAllModFiles()) {
            var mods = modFile.getModInfos();

            for (var mod : mods) {
                mod.getConfig().<String>getConfigElement("enumExtensions").ifPresent(file -> {
                    var resource = mod.getOwningFile().getFile().getContents().get(file);
                    if (resource == null) {
                        ModLoader.addLoadingIssue(ModLoadingIssue.error("fml.modloadingissue.enumextender.file_not_found", file).withAffectedMod(mod));
                        return;
                    }
                    enumExtensionsByMod.put(mod, resource);
                });
            }
        }
        RuntimeEnumExtender.loadEnumPrototypes(enumExtensionsByMod);

        backgroundScanHandler = new BackgroundScanHandler(loadingModList.getAllModFiles());

        return this.discoveryResult = new DiscoveryResult(
                loadingModList.getPlugins().stream().map(mfi -> (ModFile) mfi.getFile()).toList(),
                loadingModList.getModFiles().stream().map(ModFileInfo::getFile).toList(),
                loadingModList.getGameLibraries().stream().map(mf -> (ModFile) mf).toList(),
                loadingModList.getModLoadingIssues());
    }

    private static <T> T runOffThread(Supplier<T> supplier) {
        var cl = Thread.currentThread().getContextClassLoader();
        var future = CompletableFuture.supplyAsync(() -> {
            var previousCl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(cl);
            try {
                return supplier.get();
            } catch (Throwable e) {
                LOGGER.error("Off-thread operation failed.", e);
                throw new CompletionException(e);
            } finally {
                Thread.currentThread().setContextClassLoader(previousCl);
            }
        });

        while (true) {
            ImmediateWindowHandler.renderTick();
            try {
                return future.get(10L, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw new CompletionException(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for future", e);
            } catch (TimeoutException ignored) {}
        }
    }

    public static LanguageProviderLoader getLanguageLoadingProvider() {
        return getCurrent().languageProviderLoader;
    }

    public static FMLLoader getCurrent() {
        var current = getCurrentOrNull();
        if (current == null) {
            throw new IllegalStateException("There is no current FML Loader");
        }
        return current;
    }

    @Nullable
    public static FMLLoader getCurrentOrNull() {
        return current.get();
    }

    public Dist getDist() {
        return dist;
    }

    /**
     * @throws IllegalStateException if the loading mod list hasn't been built yet.
     */
    public LoadingModList getLoadingModList() {
        if (loadingModList == null) {
            throw new IllegalStateException("The loading mod list isn't built yet.");
        }
        return loadingModList;
    }

    public Path getGameDir() {
        return gameDir;
    }

    public boolean isProduction() {
        return production;
    }

    public ModuleLayer getGameLayer() {
        if (gameLayer == null) {
            throw new IllegalStateException("This can only be called after mod discovery is completed");
        }
        return gameLayer;
    }

    /**
     * Please note that the returned version information can be incomplete until mod discovery has been completed.
     * This is only relevant for early FML services.
     */
    public VersionInfo getVersionInfo() {
        return versionInfo;
    }

    VersionSupportMatrix getVersionSupportMatrix() {
        if (versionSupportMatrix == null) {
            throw new IllegalStateException("Mod discovery has not completed yet, versions may not be known.");
        }
        return versionSupportMatrix;
    }

    private class LaunchContextAdapter implements ILaunchContext {
        @Override
        public Dist getRequiredDistribution() {
            return dist;
        }

        @Override
        public Path gameDirectory() {
            return gameDir;
        }

        @Override
        public <T> Stream<ServiceLoader.Provider<T>> loadServices(Class<T> serviceClass) {
            // We simply rely on thread context classloader to be correct
            return ServiceLoader.load(serviceClass).stream();
        }

        @Override
        public boolean isLocated(Path path) {
            return FMLLoader.this.locatedPaths.contains(path);
        }

        @Override
        public boolean addLocated(Path path) {
            return FMLLoader.this.locatedPaths.add(path);
        }

        @Override
        public VersionInfo getVersions() {
            return versionInfo;
        }
    }
}
