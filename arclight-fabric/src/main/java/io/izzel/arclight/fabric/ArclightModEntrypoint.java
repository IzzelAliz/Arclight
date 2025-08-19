package io.izzel.arclight.fabric;

import io.izzel.arclight.api.Arclight;
import io.izzel.arclight.fabric.mod.FabricArclightServer;
import io.izzel.arclight.fabric.mod.event.EventHandlerRegistry;
import io.izzel.arclight.fabric.mod.permission.ArclightPermissionImpl;
import net.fabricmc.api.ModInitializer;

public class ArclightModEntrypoint implements ModInitializer {

    @Override
    public void onInitialize() {
        Arclight.setServer(new FabricArclightServer());
        EventHandlerRegistry.register();
        ArclightPermissionImpl.init();
    }
}