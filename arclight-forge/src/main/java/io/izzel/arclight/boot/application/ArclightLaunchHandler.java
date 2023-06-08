package io.izzel.arclight.boot.application;

import net.minecraftforge.fml.loading.targets.ForgeServerLaunchHandler;

public class ArclightLaunchHandler extends ForgeServerLaunchHandler {

    @Override
    public String name() {
        return "arclightserver";
    }

    @Override
    protected String[] preLaunch(String[] arguments, ModuleLayer layer) {
        // skip the log4j configuration reloading
        return arguments;
    }
}
