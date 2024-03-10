package io.izzel.arclight.installer;

import com.google.gson.Gson;
import io.izzel.arclight.api.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlContext;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NeoforgeInstaller {

    private static final MethodHandles.Lookup IMPL_LOOKUP = Unsafe.lookup();

    @SuppressWarnings("unused")
    public static Map.Entry<String, List<String>> applicationInstall() throws Throwable {
        InputStream stream = ForgeInstaller.class.getResourceAsStream("/META-INF/installer.json");
        InstallInfo installInfo = new Gson().fromJson(new InputStreamReader(stream), InstallInfo.class);
        List<Supplier<Path>> suppliers = MinecraftProvider.checkMavenNoSource(installInfo.libraries);
        var sysType = File.pathSeparatorChar == ';' ? "win" : "unix";
        Path path = Paths.get("libraries", "net", "neoforged", "neoforge", installInfo.installer.neoforge, sysType + "_args.txt");
        var installForge = !Files.exists(path) || forgeClasspathMissing(path);
        if (!suppliers.isEmpty() || installForge) {
            System.out.println("Downloading missing libraries ...");
            ExecutorService pool = Executors.newWorkStealingPool(8);
            CompletableFuture<?>[] array = suppliers.stream().map(MinecraftProvider.reportSupply(pool, System.out::println)).toArray(CompletableFuture[]::new);
            if (installForge) {
                var futures = installForge(installInfo, pool, System.out::println);
                MinecraftProvider.handleFutures(System.out::println, futures);
                System.out.println("Forge installation is starting, please wait... ");
                try {
                    ProcessBuilder builder = new ProcessBuilder();
                    File file = new File(System.getProperty("java.home"), "bin/java");
                    builder.command(file.getCanonicalPath(), "-Djava.net.useSystemProxies=true", "-jar", futures[0].join().toString(), "--installServer", ".", "--debug");
                    builder.inheritIO();
                    Process process = builder.start();
                    if (process.waitFor() > 0) {
                        throw new Exception("Forge installation failed");
                    }
                } catch (IOException e) {
                    try (URLClassLoader loader = new URLClassLoader(
                        new URL[]{futures[0].join().toUri().toURL()},
                        ForgeInstaller.class.getClassLoader().getParent())) {
                        Method method = loader.loadClass("net.minecraftforge.installer.SimpleInstaller").getMethod("main", String[].class);
                        method.invoke(null, (Object) new String[]{"--installServer", ".", "--debug"});
                    }
                }
            }
            MinecraftProvider.handleFutures(System.out::println, array);
            pool.shutdownNow();
        }
        return classpath(path, installInfo);
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<Path>[] installForge(InstallInfo info, ExecutorService pool, Consumer<String> logger) {
        var minecraftData = MinecraftProvider.downloadMinecraftData(info, pool, logger);
        String coord = String.format("net.neoforged:neoforge:%s:installer", info.installer.neoforge);
        String dist = String.format("neoforge-%s-installer.jar", info.installer.neoforge);
        var installerFuture = ForgeLikeProvider.downloadInstaller(coord, dist, info.installer.neoforgeHash, minecraftData, info, pool, logger);
        var serverFuture = minecraftData.thenCompose(data -> MinecraftProvider.reportSupply(pool, logger).apply(
            new FileDownloader(String.format(data.serverUrl(), info.installer.minecraft),
                String.format("libraries/net/minecraft/server/%1$s/server-%1$s.jar", info.installer.minecraft), data.serverHash())
        ));
        return new CompletableFuture[]{installerFuture, serverFuture};
    }

    private static boolean forgeClasspathMissing(Path path) throws Exception {
        for (String arg : Files.lines(path).toList()) {
            if (arg.startsWith("-p ")) {
                var modules = arg.substring(2).trim();
                if (!Arrays.stream(modules.split(File.pathSeparator)).map(Paths::get).allMatch(Files::exists)) {
                    return true;
                }
            } else if (arg.startsWith("-DlegacyClassPath")) {
                var classpath = arg.substring("-DlegacyClassPath=".length()).trim();
                if (!Arrays.stream(classpath.split(File.pathSeparator)).map(Paths::get).allMatch(Files::exists)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map.Entry<String, List<String>> classpath(Path path, InstallInfo installInfo) throws Throwable {
        boolean jvmArgs = true;
        String mainClass = null;
        List<String> userArgs = new ArrayList<>();
        List<String> opens = new ArrayList<>();
        List<String> exports = new ArrayList<>();
        exports.add("cpw.mods.bootstraplauncher/cpw.mods.bootstraplauncher=ALL-UNNAMED");
        List<String> ignores = new ArrayList<>();
        List<String> merges = new ArrayList<>();
        var self = new File(ForgeInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath();
        for (String arg : Files.lines(path).toList()) {
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
                        split[1] =
                            Stream.concat(
                                Stream.concat(Stream.concat(Stream.of(self.toString()), Arrays.stream(split[1].split(File.pathSeparator))), installInfo.libraries.keySet().stream()
                                    .peek(it -> {
                                        var lib = Paths.get("libraries", Util.mavenToPath(it));
                                        var name = lib.getFileName().toString();
                                        if (name.contains("maven-model")) {
                                            merges.add(name);
                                        }
                                    })
                                    .map(it -> "libraries/" + Util.mavenToPath(it))),
                                Stream.empty()
                                //Stream.of(self)
                            ).sorted((a, b) -> {
                                // damn stupid jpms
                                if (a.contains("maven-repository-metadata")) {
                                    return -1;
                                } else if (b.contains("maven-repository-metadata")) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            }).distinct().collect(Collectors.joining(File.pathSeparator));
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
        addToPath(path);
        for (String library : installInfo.libraries.keySet()) {
            addToPath(Paths.get("libraries", Util.mavenToPath(library)), false);
        }*/
        return Map.entry(Objects.requireNonNull(mainClass, "No main class found"), userArgs);
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

    private record ParserData(String module, String packages, String target) {
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
        ModuleFinder finder = ModuleFinder.of(Arrays.stream(modulePath.split(File.pathSeparator)).map(Paths::get).peek(NeoforgeInstaller::addToPath).toArray(Path[]::new));
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

    @SuppressWarnings("removal")
    public static void addToPath(Path path) {
        try {
            ClassLoader loader = ClassLoader.getPlatformClassLoader();
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
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
