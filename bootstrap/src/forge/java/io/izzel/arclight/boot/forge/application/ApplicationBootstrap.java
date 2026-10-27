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

    private static final String APPLICATION_PACKAGE = "io.izzel.arclight.boot.forge.application";
    private static final String ENTRY_POINT = "io.izzel.arclight.boot.forge.mod.ModuleBootstrap";

    public static void main(String[] args) throws Exception {
        new ApplicationBootstrap().start(args);
    }

    @Override
    protected void bootstrapMain(String[] args, List<Path[]> classpath) {
        try {
            // Default parent class loader
            var cl = Thread.currentThread().getContextClassLoader();
            // This should be the AppClassloader but doesn't quite work right, can't remember why off hand but I had it commented out for a reason
            // cl == BaseBootstrap.class.getClassLoader();
            var boot = selectBootModules(classpath);
            var arclight = findArclight(classpath);
            var jar = SecureJar.from(it -> new SimpleJarMetadata("arclight.launch", "1.0", Set.of(APPLICATION_PACKAGE), List.of()), arclight.getRootPath());
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
        var runtime = selectRuntimeModules(classpath);
        var secure = runtime.stream().filter(it -> !mergedModules.contains(it.name()) && !it.name().equals("arclight.boot"))
            .collect(Collectors.toCollection(ArrayList::new));

        // Maven's model/model-builder/repository-metadata jars have split packages.
        // Retain paths merged by BootstrapClasspathModifier instead of taking only
        // the primary path from each group.
        var maven = runtime.stream().filter(it -> mergedModules.contains(it.name())).toList();
        if (!maven.isEmpty()) {
            var first = maven.getFirst();
            // A union root has no file name from which JarMetadata can derive a
            // module name, so preserve the first module's identity explicitly.
            secure.add(SecureJar.from(it -> new SimpleJarMetadata(first.name(),
                first.moduleDataProvider().descriptor().rawVersion().orElse(null), it.getPackages(), it.getProviders()),
                maven.stream().map(SecureJar::getRootPath).toArray(Path[]::new)));
        }
        var arclight = findArclight(classpath);
        secure.add(SecureJar.from(it -> new SimpleJarMetadata(arclight.name(), arclight.moduleDataProvider().descriptor().rawVersion().orElse("1.0"),
            arclight.getPackages().stream().filter(p -> !p.equals(APPLICATION_PACKAGE)).collect(Collectors.toSet()), arclight.getProviders()), arclight.getRootPath()));

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
            var services = ServiceLoader.load(layer, BootstrapEntryPoint.class).stream().filter(it -> it.type().getName().equals(ENTRY_POINT)).toList();

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

    private SecureJar findArclight(List<Path[]> classpath) {
        // Bootstrap 2.1.8 passes first-wins path groups, while SecureJar is last-wins.
        return classpath.stream().map(this::secureJar).filter(it -> it.name().equals("arclight.boot"))
            .findFirst().orElseThrow(() -> new IllegalStateException("Could not find the arclight.boot module on the launch classpath"));
    }
}
