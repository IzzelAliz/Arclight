package io.izzel.arclight.boot.forge.application;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import io.izzel.arclight.api.Unsafe;
import net.minecraftforge.bootstrap.ForgeBootstrap;
import net.minecraftforge.bootstrap.api.BootstrapEntryPoint;
import net.minecraftforge.securemodules.SecureModuleClassLoader;
import net.minecraftforge.securemodules.SecureModuleFinder;

import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationBootstrap extends ForgeBootstrap {

    public static void main(String[] args) throws Exception {
        new ApplicationBootstrap().start(args);
    }

    protected void start(String... args) throws Exception {
        var classPath = getClassPath();
        var boot = selectBootModules(classPath);
        var arclight = classPath.stream().filter(it -> it.moduleDataProvider().name().equals("arclight.boot")).findAny().orElseThrow();
        var jar = SecureJar.from(it -> new SimpleJarMetadata("arclight.launch", "1.0", Set.of("io.izzel.arclight.boot.forge.application"), List.of()), arclight.getPrimaryPath());
        boot.add(jar);

        // First we need to get ourselves onto a module layer, so that we can be the parent of the actual runtime layer
        var finder = SecureModuleFinder.of(boot.toArray(SecureJar[]::new));
        var targets = boot.stream().map(SecureJar::name).toList();
        var cfg = ModuleLayer.boot().configuration().resolve(finder, ModuleFinder.ofSystem(), targets);
        var cl = Thread.currentThread().getContextClassLoader(); //BaseBootstrap.class.getClassLoader();
        var layer = ModuleLayer.boot().defineModulesWithOneLoader(cfg, cl);

        // Find ourselves in the new fancy module environment.
        var bootstrap = layer.findModule("arclight.launch").get();
        var moduleCl = bootstrap.getClassLoader();
        var self = Class.forName(this.getClass().getName(), false, moduleCl);
        var inst = self.getDeclaredConstructor().newInstance();

        // And now invoke main as if we had done all the command line arguments to specify modules!
        var moduleMain = self.getDeclaredMethod("moduleMain", String[].class);
        moduleMain.invoke(inst, (Object) args);
    }

    @Override
    public void moduleMain(String... args) throws Exception {
        var bootlayer = getClass().getModule().getLayer();
        var classPath = getClassPath();
        var mergedModules = Set.of("maven.model", "maven.model.builder", "maven.repository.metadata", "maven.artifact");
        var secure = selectRuntimeModules(classPath.stream().filter(it -> !mergedModules.contains(it.moduleDataProvider().name()) && !it.moduleDataProvider().name().equals("arclight.boot")).toList());
        var mavenMerged = SecureJar.from(classPath.stream().filter(it -> mergedModules.contains(it.moduleDataProvider().name()))
            .map(SecureJar::getPrimaryPath).toArray(Path[]::new));
        secure.add(mavenMerged);
        var arclight = classPath.stream().filter(it -> it.moduleDataProvider().name().equals("arclight.boot")).findAny().orElseThrow();
        secure.add(SecureJar.from(it -> new SimpleJarMetadata(arclight.name(), arclight.moduleDataProvider().descriptor().rawVersion().orElse("1.0"),
            arclight.getPackages().stream().filter(p -> !p.equals("io.izzel.arclight.boot.forge.application")).collect(Collectors.toSet()), arclight.getProviders()), arclight.getPrimaryPath()));

        // Now lets build a layer that has all the non-Bootstrap/SecureModule libraries on it.
        var finder = SecureModuleFinder.of(secure.toArray(SecureJar[]::new));
        var targets = secure.stream().map(SecureJar::name).toList();
        var cfg = bootlayer.configuration().resolveAndBind(finder, ModuleFinder.ofSystem(), targets);
        var parent = List.of(ModuleLayer.boot(), bootlayer);
        var cl = new SecureModuleClassLoader("SECURE-BOOTSTRAP", null, cfg, parent);
        var layer = bootlayer.defineModules(cfg, module -> cl);

        var oldcl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            var services = ServiceLoader.load(layer, BootstrapEntryPoint.class).stream().filter(it -> it.type().getName().contains("arclight")).toList();

            if (services.isEmpty())
                throw new IllegalStateException("Could not find any " + BootstrapEntryPoint.class.getName() + " service providers");

            if (services.size() > 1) {
                throw new IllegalStateException("Found multiple " + BootstrapEntryPoint.class.getName() + " service providers: " +
                    services.stream().map(p -> p.get().name()).collect(Collectors.joining(", ")));
            }

            var loader = services.get(0).get();
            loader.main(args);
        } finally {
            Thread.currentThread().setContextClassLoader(oldcl);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<SecureJar> getClassPath() throws Exception {
        Class<?> cl = Class.forName("net.minecraftforge.bootstrap.ClassPathHelper");
        var method = cl.getDeclaredMethod("getCleanedClassPath");
        try {
            return (List<SecureJar>) Unsafe.lookup().unreflect(method).invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
