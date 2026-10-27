package io.izzel.arclight.boot.forge.mod;

import net.minecraftforge.fml.loading.targets.CommonLaunchHandler;

import java.nio.file.Path;
import java.util.List;

// Forge 64.1.5's production server contract, retaining Arclight's logging configuration.
public class ArclightLaunchHandler extends CommonLaunchHandler {

    public ArclightLaunchHandler() {
        super(CommonLaunchHandler.SERVER, "arclight_");
        // Also runs before discovery when Arclight is installed as a mod, without ModuleBootstrap.
        try {
            ModBootstrap.patchModDiscoverer(getClass());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize Arclight's Forge dependency locator", e);
        }
    }

    @Override
    public String getNaming() {
        // Forge calls the unobfuscated production namespace "mcp".
        return "mcp";
    }

    @Override
    public boolean isProduction() {
        return true;
    }

    @Override
    public List<Path> getMinecraftPaths() {
        return List.of(CommonLaunchHandler.getPathFromResource("net/minecraft/server/MinecraftServer.class"));
    }

    @Override
    protected String[] preLaunch(String[] arguments, ModuleLayer layer) {
        // skip the log4j configuration reloading
        return arguments;
    }
}
