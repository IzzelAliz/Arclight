package io.izzel.arclight.forgeinstaller;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.i18n.ArclightLocale;
import io.izzel.arclight.i18n.LocalizedException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlContext;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ForgeInstaller {

    private static final MethodHandles.Lookup IMPL_LOOKUP = Unsafe.lookup();
    private static final String[] MAVEN_REPO = {
        "https://arclight.mcxk.net/"
    };
    private static final String INSTALLER_URL = "https://arclight.mcxk.net/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar";
    private static final String SERVER_URL = "https://arclight.mcxk.net/net/minecraft/server/minecraft_server.%s.jar";
    private static final Map<String, String> VERSION_HASH = ImmutableMap.of(
        "1.17.1", "A16D67E5807F57FC4E550299CF20226194497DC2"
    );

    public static Map.Entry<String, List<String>> install() throws Throwable {
        InputStream stream = ForgeInstaller.class.getResourceAsStream("/META-INF/installer.json");
        InstallInfo installInfo = new Gson().fromJson(new InputStreamReader(stream), InstallInfo.class);
        List<Supplier<Path>> suppliers = checkMavenNoSource(installInfo.libraries);
        Path path = Paths.get("libraries", "net", "minecraftforge", "forge", installInfo.installer.minecraft + "-" + installInfo.installer.forge, "win_args.txt");
        if (!suppliers.isEmpty() || !Files.exists(path)) {
            ArclightLocale.info("downloader.info2");
            ExecutorService pool = Executors.newFixedThreadPool(8);
            CompletableFuture<?>[] array = suppliers.stream().map(reportSupply(pool)).toArray(CompletableFuture[]::new);
            if (!Files.exists(path)) {
                CompletableFuture<?>[] futures = installForge(installInfo, pool);
                handleFutures(futures);
                ArclightLocale.info("downloader.forge-install");
                try {
                    ProcessBuilder builder = new ProcessBuilder();
                    File file = new File(System.getProperty("java.home"), "bin/java");
                    builder.command(file.getCanonicalPath(), "-Djava.net.useSystemProxies=true", "-jar", String.format("forge-%s-%s-installer.jar", installInfo.installer.minecraft, installInfo.installer.forge), "--installServer", ".");
                    builder.inheritIO();
                    Process process = builder.start();
                    process.waitFor();
                } catch (IOException e) {
                    URLClassLoader loader = new URLClassLoader(
                        new URL[]{new File(String.format("forge-%s-%s-installer.jar", installInfo.installer.minecraft, installInfo.installer.forge)).toURI().toURL()},
                        ForgeInstaller.class.getClassLoader().getParent());
                    Method method = loader.loadClass("net.minecraftforge.installer.SimpleInstaller").getMethod("main", String[].class);
                    method.invoke(null, (Object) new String[]{"--installServer", "."});
                }
            }
            handleFutures(array);
            pool.shutdownNow();
        }
        return classpath(path, installInfo);
    }

    private static Function<Supplier<Path>, CompletableFuture<Path>> reportSupply(ExecutorService service) {
        return it -> CompletableFuture.supplyAsync(it, service).thenApply(path -> {
            ArclightLocale.info("downloader.complete", path);
            return path;
        });
    }

    private static CompletableFuture<?>[] installForge(InstallInfo info, ExecutorService pool) throws Exception {
        String format = String.format(INSTALLER_URL, info.installer.minecraft, info.installer.forge, info.installer.minecraft, info.installer.forge);
        String dist = String.format("forge-%s-%s-installer.jar", info.installer.minecraft, info.installer.forge);
        FileDownloader fd = new FileDownloader(format, dist, info.installer.hash);
        CompletableFuture<?> installerFuture = reportSupply(pool).apply(fd).thenAccept(path -> {
            try {
                FileSystem system = FileSystems.newFileSystem(path, (ClassLoader) null);
                Map<String, Map.Entry<String, String>> map = new HashMap<>();
                Path profile = system.getPath("install_profile.json");
                map.putAll(profileLibraries(profile));
                Path version = system.getPath("version.json");
                map.putAll(profileLibraries(version));
                List<Supplier<Path>> suppliers = checkMaven(map);
                CompletableFuture<?>[] array = suppliers.stream().map(reportSupply(pool)).toArray(CompletableFuture[]::new);
                handleFutures(array);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        CompletableFuture<?> serverFuture = reportSupply(pool).apply(
            new FileDownloader(String.format(SERVER_URL, info.installer.minecraft),
                String.format("libraries/net/minecraft/server/%1$s/minecraft_server.%1$s.jar", info.installer.minecraft), VERSION_HASH.get(info.installer.minecraft))
        );
        return new CompletableFuture<?>[]{installerFuture, serverFuture};
    }

    private static void handleFutures(CompletableFuture<?>... futures) {
        for (CompletableFuture<?> future : futures) {
            try {
                future.join();
            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof LocalizedException local) {
                    ArclightLocale.error(local.node(), local.args());
                } else throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static Map<String, Map.Entry<String, String>> profileLibraries(Path path) throws IOException {
        Map<String, Map.Entry<String, String>> ret = new HashMap<>();
        JsonArray array = new JsonParser().parse(Files.newBufferedReader(path)).getAsJsonObject().getAsJsonArray("libraries");
        for (JsonElement element : array) {
            String name = element.getAsJsonObject().get("name").getAsString();
            JsonObject artifact = element.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("artifact");
            String hash = artifact.get("sha1").getAsString();
            String url = artifact.get("url").getAsString();
            if (url == null || url.trim().isEmpty()) continue;
            ret.put(name, new AbstractMap.SimpleImmutableEntry<>(hash, url));
        }
        return ret;
    }

    private static List<Supplier<Path>> checkMavenNoSource(Map<String, String> map) {
        LinkedHashMap<String, Map.Entry<String, String>> hashMap = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            hashMap.put(entry.getKey(), new AbstractMap.SimpleImmutableEntry<>(entry.getValue(), null));
        }
        return checkMaven(hashMap);
    }

    private static List<Supplier<Path>> checkMaven(Map<String, Map.Entry<String, String>> map) {
        List<Supplier<Path>> incomplete = new ArrayList<>();
        for (Map.Entry<String, Map.Entry<String, String>> entry : map.entrySet()) {
            String maven = entry.getKey();
            String hash = entry.getValue().getKey();
            String url = entry.getValue().getValue();
            String path = "libraries/" + Util.mavenToPath(maven);
            if (new File(path).exists()) {
                try {
                    String fileHash = Util.hash(path);
                    if (!fileHash.equals(hash)) {
                        incomplete.add(new MavenDownloader(MAVEN_REPO, maven, path, hash, url));
                    }
                } catch (Exception e) {
                    incomplete.add(new MavenDownloader(MAVEN_REPO, maven, path, hash, url));
                }
            } else {
                incomplete.add(new MavenDownloader(MAVEN_REPO, maven, path, hash, url));
            }
        }
        return incomplete;
    }

    private static Map.Entry<String, List<String>> classpath(Path path, InstallInfo installInfo) throws Throwable {
        boolean jvmArgs = true;
        String mainClass = null;
        List<String> userArgs = new ArrayList<>();
        List<String> opens = new ArrayList<>();
        List<String> exports = new ArrayList<>();
        List<String> ignores = new ArrayList<>();
        List<String> merges = new ArrayList<>();
        var self = new File(ForgeInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalPath();
        for (String arg : Files.lines(path).collect(Collectors.toList())) {
            if (jvmArgs && arg.startsWith("-")) {
                if (arg.startsWith("-p ")) {
                    addModules(arg.substring(2).trim());
                } else if (arg.startsWith("--add-opens ")) {
                    opens.add(arg.substring("--add-opens ".length()).trim());
                } else if (arg.startsWith("--add-exports ")) {
                    exports.add(arg.substring("--add-exports ".length()).trim());
                } else if (arg.startsWith("-D")) {
                    var split = arg.substring(2).split("=", 2);
                    if (split[0].equals("legacyClassPath")) {
                        for (String lib : split[1].split(File.pathSeparator)) {
                            addToPath(Paths.get(lib), ignores.stream().anyMatch(lib::contains));
                        }
                        split[1] =
                            Stream.concat(
                                Stream.concat(Stream.of(split[1]), installInfo.libraries.keySet().stream()
                                    .map(it -> Paths.get("libraries", Util.mavenToPath(it)))
                                    .peek(it -> {
                                        var name = it.getFileName().toString();
                                        if (name.contains("maven-model")) {
                                            merges.add(name);
                                        }
                                    })
                                    .map(Path::toString)),
                                Stream.of(self)
                            ).collect(Collectors.joining(File.pathSeparator));
                    } else if (split[0].equals("ignoreList")) {
                        ignores.addAll(Arrays.asList(split[1].split(",")));
                    }
                    System.setProperty(split[0], split[1]);
                }
            } else {
                if (jvmArgs) {
                    jvmArgs = false;
                    mainClass = arg;
                } else {
                    userArgs.addAll(Arrays.asList(arg.split(" ")));
                }
            }
        }
        var merge = String.join(",", merges);
        var mergeModules = System.getProperty("mergeModules");
        if (mergeModules != null) {
            System.setProperty("mergeModules", mergeModules + ";" + merge);
        } else {
            System.setProperty("mergeModules", merge);
        }
        addOpens(opens);
        addExports(exports);
        /*
        JarFile jarFile = new JarFile(path.toFile());
        Manifest manifest = jarFile.getManifest();
        String[] split = manifest.getMainAttributes().getValue("Class-Path").split(" ");
        for (String s : split) {
            addToPath(Paths.get(s));
        }
        for (String library : installInfo.libraries.keySet()) {
            addToPath(Paths.get("libraries", Util.mavenToPath(library)));
        }
        addToPath(path);*/
        for (String library : installInfo.libraries.keySet()) {
            addToPath(Paths.get("libraries", Util.mavenToPath(library)), false);
        }
        return Map.entry(Objects.requireNonNull(mainClass, "No main class found"), userArgs);
    }

    private static void addToPath(Path path, boolean boot) throws Throwable {
        ClassLoader loader = boot ? ClassLoader.getPlatformClassLoader() : ForgeInstaller.class.getClassLoader();
        Field ucpField;
        try {
            ucpField = loader.getClass().getDeclaredField("ucp");
        } catch (NoSuchFieldException e) {
            ucpField = loader.getClass().getSuperclass().getDeclaredField("ucp");
        }
        long offset = Unsafe.objectFieldOffset(ucpField);
        Object ucp = Unsafe.getObject(loader, offset);
        if (ucp == null) {
            var cl = Class.forName("jdk.internal.loader.URLClassPath");
            var handle = Unsafe.lookup().findConstructor(cl, MethodType.methodType(void.class, URL[].class, AccessControlContext.class));
            ucp = handle.invoke(new URL[]{}, (AccessControlContext) null);
            Unsafe.putObjectVolatile(loader, offset, ucp);
        }
        Method method = ucp.getClass().getDeclaredMethod("addURL", URL.class);
        Unsafe.lookup().unreflect(method).invoke(ucp, path.toUri().toURL());
    }

    public static void addExports(List<String> exports) throws Throwable {
        MethodHandle implAddExportsMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddExports", MethodType.methodType(void.class, String.class, Module.class));
        MethodHandle implAddExportsToAllUnnamedMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddExportsToAllUnnamed", MethodType.methodType(void.class, String.class));

        addExtra(exports, implAddExportsMH, implAddExportsToAllUnnamedMH);
    }

    public static void addOpens(List<String> opens) throws Throwable {
        MethodHandle implAddOpensMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddOpens", MethodType.methodType(void.class, String.class, Module.class));
        MethodHandle implAddOpensToAllUnnamedMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddOpensToAllUnnamed", MethodType.methodType(void.class, String.class));

        addExtra(opens, implAddOpensMH, implAddOpensToAllUnnamedMH);
    }

    private static ParserData parseModuleExtra(String extra) {
        String[] all = extra.split("=", 2);
        if (all.length < 2) {
            return null;
        }

        String[] source = all[0].split("/", 2);
        if (source.length < 2) {
            return null;
        }
        return new ParserData(source[0], source[1], all[1]);
    }

    private static class ParserData {

        final String module;
        final String packages;
        final String target;

        ParserData(String module, String packages, String target) {
            this.module = module;
            this.packages = packages;
            this.target = target;
        }
    }

    private static void addExtra(List<String> extras, MethodHandle implAddExtraMH, MethodHandle implAddExtraToAllUnnamedMH) {
        extras.forEach(extra -> {
            ParserData data = parseModuleExtra(extra);
            if (data != null) {
                ModuleLayer.boot().findModule(data.module).ifPresent(m -> {
                    try {
                        if ("ALL-UNNAMED".equals(data.target)) {
                            implAddExtraToAllUnnamedMH.invokeWithArguments(m, data.packages);
                        } else {
                            ModuleLayer.boot().findModule(data.target).ifPresent(tm -> {
                                try {
                                    implAddExtraMH.invokeWithArguments(m, data.packages, tm);
                                } catch (Throwable t) {
                                    throw new RuntimeException(t);
                                }
                            });
                        }
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void addModules(String modulePath) throws Throwable {

        // Find all extra modules
        ModuleFinder finder = ModuleFinder.of(Arrays.stream(modulePath.split(File.pathSeparator)).map(Paths::get).toArray(Path[]::new));
        MethodHandle loadModuleMH = IMPL_LOOKUP.findVirtual(Class.forName("jdk.internal.loader.BuiltinClassLoader"), "loadModule", MethodType.methodType(void.class, ModuleReference.class));

        // Resolve modules to a new config
        Configuration config = Configuration.resolveAndBind(finder, List.of(ModuleLayer.boot().configuration()), finder, finder.findAll().stream().peek(mref -> {
            try {
                // Load all extra modules in system class loader (unnamed modules for now)
                loadModuleMH.invokeWithArguments(ClassLoader.getSystemClassLoader(), mref);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }).map(ModuleReference::descriptor).map(ModuleDescriptor::name).collect(Collectors.toList()));

        // Copy the new config graph to boot module layer config
        MethodHandle graphGetter = IMPL_LOOKUP.findGetter(Configuration.class, "graph", Map.class);
        HashMap<ResolvedModule, Set<ResolvedModule>> graphMap = new HashMap<>((Map<ResolvedModule, Set<ResolvedModule>>) graphGetter.invokeWithArguments(config));
        MethodHandle cfSetter = IMPL_LOOKUP.findSetter(ResolvedModule.class, "cf", Configuration.class);
        // Reset all extra resolved modules config to boot module layer config
        graphMap.forEach((k, v) -> {
            try {
                cfSetter.invokeWithArguments(k, ModuleLayer.boot().configuration());
                v.forEach(m -> {
                    try {
                        cfSetter.invokeWithArguments(m, ModuleLayer.boot().configuration());
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                });
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
        graphMap.putAll((Map<ResolvedModule, Set<ResolvedModule>>) graphGetter.invokeWithArguments(ModuleLayer.boot().configuration()));
        IMPL_LOOKUP.findSetter(Configuration.class, "graph", Map.class).invokeWithArguments(ModuleLayer.boot().configuration(), new HashMap<>(graphMap));

        // Reset boot module layer resolved modules as new config resolved modules to prepare define modules
        Set<ResolvedModule> oldBootModules = ModuleLayer.boot().configuration().modules();
        MethodHandle modulesSetter = IMPL_LOOKUP.findSetter(Configuration.class, "modules", Set.class);
        HashSet<ResolvedModule> modulesSet = new HashSet<>(config.modules());
        modulesSetter.invokeWithArguments(ModuleLayer.boot().configuration(), new HashSet<>(modulesSet));

        // Prepare to add all of the new config "nameToModule" to boot module layer config
        MethodHandle nameToModuleGetter = IMPL_LOOKUP.findGetter(Configuration.class, "nameToModule", Map.class);
        HashMap<String, ResolvedModule> nameToModuleMap = new HashMap<>((Map<String, ResolvedModule>) nameToModuleGetter.invokeWithArguments(ModuleLayer.boot().configuration()));
        nameToModuleMap.putAll((Map<String, ResolvedModule>) nameToModuleGetter.invokeWithArguments(config));
        IMPL_LOOKUP.findSetter(Configuration.class, "nameToModule", Map.class).invokeWithArguments(ModuleLayer.boot().configuration(), new HashMap<>(nameToModuleMap));

        // Define all extra modules and add all of the new config "nameToModule" to boot module layer config
        ((Map<String, Module>) IMPL_LOOKUP.findGetter(ModuleLayer.class, "nameToModule", Map.class).invokeWithArguments(ModuleLayer.boot())).putAll((Map<String, Module>) IMPL_LOOKUP.findStatic(Module.class, "defineModules", MethodType.methodType(Map.class, Configuration.class, Function.class, ModuleLayer.class)).invokeWithArguments(ModuleLayer.boot().configuration(), (Function<String, ClassLoader>) name -> ClassLoader.getSystemClassLoader(), ModuleLayer.boot()));

        // Add all of resolved modules
        modulesSet.addAll(oldBootModules);
        modulesSetter.invokeWithArguments(ModuleLayer.boot().configuration(), new HashSet<>(modulesSet));

        // Reset cache of boot module layer
        IMPL_LOOKUP.findSetter(ModuleLayer.class, "modules", Set.class).invokeWithArguments(ModuleLayer.boot(), null);
        IMPL_LOOKUP.findSetter(ModuleLayer.class, "servicesCatalog", Class.forName("jdk.internal.module.ServicesCatalog")).invokeWithArguments(ModuleLayer.boot(), null);

        // Add reads from extra modules to jdk modules
        MethodHandle implAddReadsMH = IMPL_LOOKUP.findVirtual(Module.class, "implAddReads", MethodType.methodType(void.class, Module.class));
        config.modules().forEach(rm -> ModuleLayer.boot().findModule(rm.name()).ifPresent(m -> oldBootModules.forEach(brm -> ModuleLayer.boot().findModule(brm.name()).ifPresent(bm -> {
            try {
                implAddReadsMH.invokeWithArguments(m, bm);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }))));
    }
}
