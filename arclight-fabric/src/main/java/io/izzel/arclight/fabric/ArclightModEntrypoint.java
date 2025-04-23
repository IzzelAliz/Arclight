package io.izzel.arclight.fabric;

import io.izzel.arclight.api.Arclight;
import io.izzel.arclight.fabric.mod.FabricArclightServer;
import io.izzel.arclight.fabric.mod.event.S2CPlayNConfigChannelHandler;
import io.izzel.arclight.fabric.mod.permission.ArclightPermissionImpl;
import net.fabricmc.api.ModInitializer;

public class ArclightModEntrypoint implements ModInitializer {

    @Override
    public void onInitialize() {
        Arclight.setServer(new FabricArclightServer());
        S2CPlayNConfigChannelHandler.register();
        ArclightPermissionImpl.init();
    }
}