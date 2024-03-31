package io.izzel.arclight.boot.forge.application;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import net.minecraftforge.bootstrap.ForgeBootstrap;
import net.minecraftforge.bootstrap.api.BootstrapEntryPoint;
import net.minecraftforge.securemodules.SecureModuleClassLoader;
import net.minecraftforge.securemodules.SecureModuleFinder;

import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationBootstrap extends ForgeBootstrap {

    public static void main(String[] args) throws Exception {
        new ApplicationBootstrap().start(args);
    }

    protected void bootstrapMain(String[] args, List<Path[]> classpath) {
        try {
            // Default parent class loader
            var cl = Thread.currentThread().getContextClassLoader();
            // This should be the AppClassloader but doesn't quite work right, can't remember why off hand but I had it commented out for a reason
            // cl == BaseBootstrap.class.getClassLoader();
            var boot = selectBootModules(classpath);
            var arclight = classpath.stream().map(SecureJar::from).filter(it -> it.moduleDataProvider().name().equals("arclight.boot")).findAny().orElseThrow();
            var jar = SecureJar.from(it -> new SimpleJarMetadata("arclight.launch", "1.0", Set.of("io.izzel.arclight.boot.forge.application"), List.of()), arclight.getPrimaryPath());
            boot.add(jar);

            // First we need to get ourselves onto a module layer, so that we can be the parent of the actual runtime layer
            var finder = SecureModuleFinder.of(boot.toArray(SecureJar[]::new));
            var targets = boot.stream().map(SecureJar::name).toList();
            var cfg = ModuleLayer.boot().configuration().resolve(finder, ModuleFinder.ofSystem(), targets);
            var layer = ModuleLayer.boot().defineModulesWithOneLoader(cfg, cl);

            // Find ourselves in the new fancy module environment.
            var bootstrap = layer.findModule("arclight.launch").orElseThrow();
            var moduleCl = bootstrap.getClassLoader();
            var self = Class.forName(this.getClass().getName(), false, moduleCl);
            var inst = self.getDeclaredConstructor().newInstance();

            // And now invoke main as if we had done all the command line arguments to specify modules!
            var moduleMain = self.getDeclaredMethod("moduleMain", String[].class, List.class);
            moduleMain.invoke(inst, args, classpath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void moduleMain(String[] args, List<Path[]> classpath) throws Exception {
        var bootlayer = getClass().getModule().getLayer();

        var mergedModules = Set.of("maven.model", "maven.model.builder", "maven.repository.metadata", "maven.artifact");
        var secure = selectRuntimeModules(classpath).stream().filter(it -> !mergedModules.contains(it.moduleDataProvider().name()) && !it.moduleDataProvider().name().equals("arclight.boot"))
            .collect(Collectors.toCollection(ArrayList::new));

        var mavenMerged = SecureJar.from(classpath.stream().map(SecureJar::from).filter(it -> mergedModules.contains(it.moduleDataProvider().name()))
            .map(SecureJar::getPrimaryPath).toArray(Path[]::new));
        secure.add(mavenMerged);
        var arclight = classpath.stream().map(SecureJar::from).filter(it -> it.moduleDataProvider().name().equals("arclight.boot")).findAny().orElseThrow();
        secure.add(SecureJar.from(it -> new SimpleJarMetadata(arclight.name(), arclight.moduleDataProvider().descriptor().rawVersion().orElse("1.0"),
            arclight.getPackages().stream().filter(p -> !p.equals("io.izzel.arclight.boot.forge.application")).collect(Collectors.toSet()), arclight.getProviders()), arclight.getPrimaryPath()));

        // Now lets build a layer that has all the non-Bootstrap/SecureModule libraries on it.
        var finder = SecureModuleFinder.of(secure.toArray(SecureJar[]::new));
        var targets = secure.stream().map(SecureJar::name).toList();
        var cfg = bootlayer.configuration().resolveAndBind(finder, ModuleFinder.ofSystem(), targets);
        var parent = List.of(ModuleLayer.boot(), bootlayer);

        // Use the current classloader as the parent, if set, so that we don't get things from the bootstrap loader.
        var oldcl = Thread.currentThread().getContextClassLoader();
        var cl = new SecureModuleClassLoader("SECURE-BOOTSTRAP", null, cfg, parent, oldcl == null ? List.of() : List.of(oldcl));
        var layer = bootlayer.defineModules(cfg, module -> cl);

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
}
