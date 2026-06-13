package io.izzel.arclight.boot.neoforge.mod;

import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.ServiceRunner;
import net.neoforged.fml.startup.Server;

/**
 * Legacy modlauncher launch target kept for {@code --launchTarget arclightserver} compatibility.
 * NeoForge 26.1+ defaults to {@link Server#main} on the classpath; this handler is used when
 * ApplicationBootstrap rewrites the launch target.
 */
public class ArclightLaunchHandler implements ILaunchHandlerService {

    @Override
    public String name() {
        return "arclightserver";
    }

    @Override
    public ServiceRunner launchService(String[] arguments, ModuleLayer gameLayer) {
        return () -> Server.main(arguments);
    }
}
