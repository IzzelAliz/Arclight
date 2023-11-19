package io.izzel.arclight.boot.application;

import cpw.mods.jarhandling.SecureJar;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LibraryFinder;
import net.minecraftforge.fml.loading.VersionInfo;
import net.minecraftforge.fml.loading.targets.CommonLaunchHandler;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiPredicate;

// duplicate of ForgeProdLaunchHandler, change on update
public class ArclightLaunchHandler extends CommonLaunchHandler {

    public ArclightLaunchHandler() {
        super(SERVER, "arclight_");
    }

    @Override
    public String getNaming() {
        return "srg";
    }

    @Override
    public boolean isProduction() {
        return true;
    }

    protected Path mvn(VersionInfo info, String artifact) {
        return mvn(info, artifact, "");
    }

    protected Path mvn(VersionInfo info, String artifact, String classifier) {
        return mvn(info.forgeGroup(), artifact, classifier, info.mcAndForgeVersion());
    }

    protected Path mvn(String group, String artifact, String classifier, String version) {
        return LibraryFinder.findPathForMaven(group, artifact, "", classifier, version);
    }

    @Override
    public CommonLaunchHandler.LocatedPaths getMinecraftPaths() {
        var vers = FMLLoader.versionInfo();

        // Minecraft
        var mc = getMCPaths(vers);

        // mods (Forge)
        var forgejar = mvn(vers, "forge", "universal");

        // Libraries
        var fmlcore = mvn(vers, "fmlcore");
        var javafmllang = mvn(vers, "javafmllanguage");
        var lowcodelang = mvn(vers, "lowcodelanguage");
        var mclang = mvn(vers, "mclanguage");

        return new CommonLaunchHandler.LocatedPaths(
            mc,
            null,
            List.of(List.of(forgejar)),
            List.of(fmlcore, javafmllang, lowcodelang, mclang)
        );
    }

    @Override
    protected String[] preLaunch(String[] arguments, ModuleLayer layer) {
        // skip the log4j configuration reloading
        return arguments;
    }

    protected List<Path> getMCPaths(VersionInfo vers) {
        var mc = mvn("net.minecraft", "server", "srg", vers.mcAndMCPVersion());
        var mcextra = mvn("net.minecraft", "server", "extra", vers.mcAndMCPVersion());
        var patches = mvn(vers, "forge", "server");

        // TODO: Get rid of this by filtering the file at install time.
        // We only want it for it's resources. So filter everything else out.
        BiPredicate<String, String> filter = (path, base) -> {
            return path.equals("META-INF/versions/") || // This is required because it bypasses our filter for the manifest, and it's a multi-release jar.
                (!path.endsWith(".class") && !path.startsWith("META-INF/"));
        };
        mcextra = SecureJar.from(filter, mcextra).getRootPath();

        return List.of(mc, mcextra, patches);
    }
}
