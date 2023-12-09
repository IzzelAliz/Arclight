package io.izzel.arclight.boot.mod;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LibraryFinder;
import net.minecraftforge.fml.loading.targets.CommonLaunchHandler;

import java.nio.file.Path;
import java.util.List;

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

    @Override
    public List<Path> getMinecraftPaths() {
        var vers = FMLLoader.versionInfo();
        var mc = LibraryFinder.findPathForMaven(vers.forgeGroup(), "forge", "", this.type.name(), vers.mcAndForgeVersion());
        return List.of(mc);
    }

    @Override
    protected String[] preLaunch(String[] arguments, ModuleLayer layer) {
        // skip the log4j configuration reloading
        return arguments;
    }
}
