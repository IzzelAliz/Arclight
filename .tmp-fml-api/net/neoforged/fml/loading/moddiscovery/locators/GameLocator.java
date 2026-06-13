/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.locators;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.LibraryFinder;
import net.neoforged.fml.loading.MavenCoordinate;
import net.neoforged.fml.loading.moddiscovery.ModJarMetadata;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.fml.util.ClasspathResourceUtils;
import net.neoforged.fml.util.PathPrettyPrinting;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameLocator implements IModFileCandidateLocator {
    private static final Logger LOG = LoggerFactory.getLogger(GameLocator.class);
    public static final String LIBRARIES_DIRECTORY_PROPERTY = "libraryDirectory";
    public static final String[] NEOFORGE_SPECIFIC_PATH_PREFIXES = { "net/neoforged/neoforge/", "META-INF/services/", JarModsDotTomlModFileReader.MODS_TOML };

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        var ourCl = Thread.currentThread().getContextClassLoader();

        // 0) Vanilla Launcher puts the obfuscated jar on the classpath. We mark it as claimed to prevent it from
        // being hoisted into a module, occupying the entrypoint packages.
        preventLoadingOfObfuscatedClientJar(context, ourCl);

        // We look for a class present in both Minecraft distributions on the classpath, which would be obfuscated in production (DetectedVersion)
        // If that is present, we assume we're launching in dev (NeoDev or ModDev).
        try (var systemFiles = RequiredSystemFiles.find(context, ourCl)) {
            if (!systemFiles.isEmpty()) {
                // If we've only been able to find some of the required files, we need to error
                systemFiles.checkForMissingMinecraftFiles(context.getRequiredDistribution() == Dist.CLIENT);

                handleMergedMinecraftAndNeoForgeJar(context, pipeline, systemFiles);
                return;
            } else {
                LOG.info("Failed to find common Minecraft classes and resources on the classpath. Assuming we're launching production.");
            }
        }

        // In production, it's in the libraries directory, and we're passed the version to look for on the commandline
        locateProductionMinecraft(context, pipeline);
    }

    private static void handleMergedMinecraftAndNeoForgeJar(ILaunchContext context, IDiscoveryPipeline pipeline, RequiredSystemFiles systemFiles) {
        LOG.info("Detected a joined NeoForge and Minecraft configuration. Applying filtering...");

        var mcJarContents = getCombinedMinecraftJar(context, systemFiles);
        IModFile minecraftModFile;
        if (mcJarContents.containsFile("META-INF/neoforged.mods.toml")) {
            // In this branch, the jar already has a neoforge.mods.toml
            minecraftModFile = pipeline.addJarContent(mcJarContents, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.IGNORE).orElse(null);
            if (minecraftModFile == null) {
                throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_minecraft_jar"));
            }
        } else {
            var minecraftVersion = detectMinecraftVersion(mcJarContents);
            var mcJarMetadata = new ModJarMetadata();
            minecraftModFile = IModFile.create(mcJarContents, mcJarMetadata, new MinecraftModInfo(minecraftVersion)::buildMinecraftModInfo);
            mcJarMetadata.setModFile(minecraftModFile);
            if (!pipeline.addModFile(minecraftModFile)) {
                throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_minecraft_jar"));
            }
        }
        if (!minecraftModFile.getId().equals("minecraft")) {
            LOG.error("The mod id for the Minecraft jar is not 'minecraft': {}", minecraftModFile.getId());
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_minecraft_jar"));
        }

        // We need to separate out our resources/code so that we can show up as a different data pack.
        JarContents.PathFilter nfJarFilter = relativePath -> {
            if (!relativePath.endsWith(".class")) {
                return true;
            }
            for (var includedPrefix : NEOFORGE_SPECIFIC_PATH_PREFIXES) {
                if (relativePath.startsWith(includedPrefix)) {
                    return true;
                }
            }
            return false;
        };
        var nfJarRoots = new ArrayList<JarContents.FilteredPath>();
        for (var nfJarRoot : systemFiles.getNeoForgeJarComponents()) {
            nfJarRoots.add(new JarContents.FilteredPath(nfJarRoot.getPrimaryPath(), nfJarFilter));
        }
        JarContents nfJarContents;
        try {
            nfJarContents = JarContents.ofFilteredPaths(nfJarRoots);
        } catch (IOException e) {
            LOG.error("Failed to construct filtered NeoForge jar from {}", nfJarRoots);
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_neoforge_jar").withCause(e));
        }

        var modFile = JarModsDotTomlModFileReader.createModFile(nfJarContents, ModFileDiscoveryAttributes.DEFAULT);
        if (modFile == null) {
            LOG.error("Failed to construct NeoForge mod file from {}", nfJarContents);
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_neoforge_jar"));
        }
        pipeline.addModFile(modFile);

        systemFiles.getAll().stream().map(JarContents::getPrimaryPath).forEach(context::addLocated);
    }

    private static JarContents getCombinedMinecraftJar(ILaunchContext context, RequiredSystemFiles systemFiles) {
        if (systemFiles.getCommonResources() == systemFiles.getNeoForgeResources()) {
            throw new IllegalStateException("The Minecraft and NeoForge resources cannot come from the same jar: "
                    + systemFiles.getCommonResources() + " and " + systemFiles.getNeoForgeResources());
        }

        var mcJarRoots = new ArrayList<JarContents.FilteredPath>();
        mcJarRoots.addAll(getMinecraftResourcesRoots(context, systemFiles));

        JarContents.PathFilter mcClassesFilter = relativePath -> {
            if (relativePath.endsWith(".class")) {
                for (var pkg : NEOFORGE_SPECIFIC_PATH_PREFIXES) {
                    if (relativePath.startsWith(pkg)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        };
        addContentRoot(mcJarRoots, systemFiles.getCommonClasses(), mcClassesFilter);
        addContentRoot(mcJarRoots, systemFiles.getClientClasses(), mcClassesFilter);

        JarContents mcJarContents;
        try {
            mcJarContents = JarContents.ofFilteredPaths(mcJarRoots);
        } catch (IOException e) {
            LOG.error("Failed to construct filtered Minecraft jar from {}", mcJarRoots);
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_minecraft_jar").withCause(e));
        }
        return mcJarContents;
    }

    /**
     * Reads the version.json found in Minecraft jars (both server and client have it) to detect which version of
     * Minecraft is in the given Jar.
     */
    private static String detectMinecraftVersion(JarContents mcJarContents) {
        String minecraftVersion;
        try (var in = mcJarContents.openFile("version.json")) {
            if (in == null) {
                LOG.error("Minecraft version.json not found in {}.", mcJarContents);
                throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_minecraft_jar"));
            }
            var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            var versionElement = new Gson().fromJson(reader, JsonObject.class);
            var idPrimitive = versionElement.getAsJsonPrimitive("id");
            if (idPrimitive == null) {
                LOG.error("Minecraft version.json found in {} is missing 'id' field. Available fields are: {}", mcJarContents, versionElement.keySet());
                throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_minecraft_jar"));
            }
            minecraftVersion = idPrimitive.getAsString();
        } catch (IOException e) {
            LOG.error("Failed to read Minecraft version.json from {}", mcJarContents);
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_minecraft_jar").withCause(e));
        }
        return minecraftVersion;
    }

    private static List<JarContents.FilteredPath> getMinecraftResourcesRoots(ILaunchContext context, RequiredSystemFiles systemFiles) {
        JarContents commonResources = systemFiles.getCommonResources();
        JarContents clientResources = systemFiles.getClientResources();

        // If the resource roots are separate from classes, no filter needs to be applied.
        List<JarContents.FilteredPath> result = new ArrayList<>();
        result.add(buildFilteredMinecraftResourcesFilteredPath(commonResources, context.getRequiredDistribution(), systemFiles));

        if (clientResources != null && clientResources != commonResources) {
            result.add(buildFilteredMinecraftResourcesFilteredPath(clientResources, context.getRequiredDistribution(), systemFiles));
        }

        return result;
    }

    private static JarContents.FilteredPath buildFilteredMinecraftResourcesFilteredPath(JarContents container, Dist requiredDist, RequiredSystemFiles systemFiles) {
        JarContents.PathFilter pathFilter = null;

        // If the resources container is also used for classes, we filter out the .class files
        if (systemFiles.getClassesRoots().contains(container)) {
            pathFilter = relativePath -> !relativePath.endsWith(".class");
        }

        // If there are masked, non-class resources, we also need to filter these out
        pathFilter = JarContents.PathFilter.and(pathFilter, getMaskedResourceFilter(container, requiredDist));

        return new JarContents.FilteredPath(container.getPrimaryPath(), pathFilter);
    }

    private static JarContents.@Nullable PathFilter getMaskedResourceFilter(JarContents jar, Dist requiredDist) {
        var maskedResources = NeoForgeDevDistCleaner.getMaskedFiles(jar, requiredDist)
                .filter(path -> !path.endsWith(".class"))
                .collect(Collectors.toSet());
        if (!maskedResources.isEmpty()) {
            return relativePath -> {
                if (maskedResources.contains(relativePath)) {
                    LOG.debug("Masking access to {} since it's from a different Minecraft distribution.", relativePath);
                    return false;
                }
                return true;
            };
        }
        return null;
    }

    private static void addContentRoot(List<JarContents.FilteredPath> roots, JarContents jarContents, JarContents.PathFilter filter) {
        if (jarContents == null) {
            return;
        }
        for (var root : roots) {
            if (root.path().equals(jarContents.getPrimaryPath()) && root.filter() == filter) {
                return;
            }
        }
        roots.add(new JarContents.FilteredPath(jarContents.getPrimaryPath(), filter));
    }

    /**
     * In production, the client and neoforge jars are assembled from partial jars in the libraries folder.
     */
    private static void locateProductionMinecraft(ILaunchContext context, IDiscoveryPipeline pipeline) {
        // 2) It's neither, but a libraries directory and desired versions are given on the commandline
        var librariesDirectory = System.getProperty(LIBRARIES_DIRECTORY_PROPERTY);
        if (librariesDirectory == null) {
            LOG.error("When launching in production, the system property {} must point to the libraries directory.", LIBRARIES_DIRECTORY_PROPERTY);
            pipeline.addIssue(ModLoadingIssue.error("fml.modloadingissue.corrupted_installation"));
            return;
        }

        var librariesRoot = Path.of(librariesDirectory);
        if (!Files.isDirectory(librariesRoot)) {
            LOG.error("Libraries directory is not readable: {}", librariesRoot);
            pipeline.addIssue(ModLoadingIssue.error("fml.modloadingissue.corrupted_installation"));
            return;
        }

        PathPrettyPrinting.addSubstitution(librariesRoot, "~libraries/", "");

        // The versions for Minecraft and NeoForge, etc. must be given on the CLI
        var versions = context.getVersions();
        var minecraftVersion = versions.mcVersion();
        if (minecraftVersion == null) {
            LOG.error("When launching in production, --fml.mcVersion must be present as a command-line argument");
            pipeline.addIssue(ModLoadingIssue.error("fml.modloadingissue.corrupted_installation"));
            return;
        }
        var neoForgeVersion = versions.neoForgeVersion();
        if (neoForgeVersion == null) {
            LOG.error("When launching in production, --fml.neoForgeVersion must be present as a command-line argument");
            pipeline.addIssue(ModLoadingIssue.error("fml.modloadingissue.corrupted_installation"));
            return;
        }
        var neoFormVersion = versions.neoFormVersion();
        if (neoFormVersion == null) {
            LOG.error("When launching in production, --fml.neoFormVersion must be present as a command-line argument");
            pipeline.addIssue(ModLoadingIssue.error("fml.modloadingissue.corrupted_installation"));
            return;
        }

        // Detect if the newer Minecraft installation method is available. If not, we assume the old method.
        var patchedMinecraftPath = librariesRoot.resolve((switch (context.getRequiredDistribution()) {
            case CLIENT -> new MavenCoordinate("net.neoforged", "minecraft-client-patched", "", "", versions.neoForgeVersion());
            case DEDICATED_SERVER -> new MavenCoordinate("net.neoforged", "minecraft-server-patched", "", "", versions.neoForgeVersion());
        }).toRelativeRepositoryPath());

        if (Files.isRegularFile(patchedMinecraftPath)) {
            if (pipeline.addPath(patchedMinecraftPath, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.IGNORE).isEmpty()) {
                throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_minecraft_jar").withAffectedPath(patchedMinecraftPath));
            }
        } else {
            var content = new ArrayList<Path>();
            switch (context.getRequiredDistribution()) {
                case CLIENT -> {
                    addRequiredLibrary(new MavenCoordinate("net.minecraft", "client", "", "srg", versions.mcAndNeoFormVersion()), content);
                    addRequiredLibrary(new MavenCoordinate("net.minecraft", "client", "", "extra", versions.mcAndNeoFormVersion()), content);
                    addRequiredLibrary(new MavenCoordinate("net.neoforged", "neoforge", "", "client", versions.neoForgeVersion()), content);
                }
                case DEDICATED_SERVER -> {
                    addRequiredLibrary(new MavenCoordinate("net.minecraft", "server", "", "srg", versions.mcAndNeoFormVersion()), content);
                    addRequiredLibrary(new MavenCoordinate("net.minecraft", "server", "", "extra", versions.mcAndNeoFormVersion()), content);
                    addRequiredLibrary(new MavenCoordinate("net.neoforged", "neoforge", "", "server", versions.neoForgeVersion()), content);
                }
            }

            try {
                var mcJarContents = JarContents.ofPaths(content);

                var mcJarMetadata = new ModJarMetadata();
                var mcjar = IModFile.create(mcJarContents, mcJarMetadata, new MinecraftModInfo(minecraftVersion)::buildMinecraftModInfo);
                mcJarMetadata.setModFile(mcjar);

                pipeline.addModFile(mcjar);
            } catch (Exception e) {
                pipeline.addIssue(ModLoadingIssue.error("fml.modloadingissue.corrupted_installation").withCause(e));
            }
        }

        var neoforgeCoordinate = new MavenCoordinate("net.neoforged", "neoforge", "", "universal", versions.neoForgeVersion());
        var neoforgeJar = librariesRoot.resolve(neoforgeCoordinate.toRelativeRepositoryPath());
        if (!Files.exists(neoforgeJar)) {
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.missing_neoforge_jar").withAffectedPath(neoforgeJar));
        }
        if (pipeline.addPath(neoforgeJar, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.IGNORE).isEmpty()) {
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_neoforge_jar").withAffectedPath(neoforgeJar));
        }
    }

    private void preventLoadingOfObfuscatedClientJar(ILaunchContext context, ClassLoader ourCl) {
        try {
            var resources = ourCl.getResources(RequiredSystemFiles.COMMON_CLASS);
            while (resources.hasMoreElements()) {
                Path jarPath = ClasspathResourceUtils.findJarPathFor(RequiredSystemFiles.COMMON_CLASS, "minecraft jar", resources.nextElement());
                try (var zip = new ZipFile(jarPath.toFile())) {
                    // An unmodified Minecraft Jar must have the resources too
                    if (zip.getEntry(RequiredSystemFiles.COMMON_RESOURCE_ROOT) == null) {
                        continue;
                    }
                    // If the client class is in it, also make sure it has the client resources and vice-versa
                    boolean hasClientClasses = zip.getEntry(RequiredSystemFiles.CLIENT_CLASS) != null;
                    boolean hasClientResources = zip.getEntry(RequiredSystemFiles.CLIENT_RESOURCE_ROOT) != null;
                    if (hasClientClasses != hasClientResources) {
                        continue;
                    }

                    // If it has a neoforge.mods.toml, it's likely a patched Minecraft
                    if (zip.getEntry("META-INF/neoforge.mods.toml") != null) {
                        continue;
                    }

                    LOG.info("Marking unmodified client jar as claimed to prevent loading: {}", jarPath);
                    context.addLocated(jarPath);
                    return;
                } catch (IOException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    private static void addRequiredLibrary(MavenCoordinate coordinate, List<Path> content) {
        var path = LibraryFinder.findPathForMaven(coordinate);
        if (!Files.exists(path)) {
            LOG.error("Failed to find required file {}", path);
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_installation").withAffectedPath(path));
        } else {
            content.add(path);
        }
    }

    @Override
    public int getPriority() {
        return HIGHEST_SYSTEM_PRIORITY;
    }

    @Override
    public String toString() {
        return "game locator";
    }
}
