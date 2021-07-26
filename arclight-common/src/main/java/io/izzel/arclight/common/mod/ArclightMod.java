package io.izzel.arclight.common.mod;

import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.common.mod.server.ArclightPermissionHandler;
import io.izzel.arclight.common.mod.server.event.ArclightEventDispatcherRegistry;
import io.izzel.arclight.common.mod.util.log.ArclightI18nLogger;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmllegacy.network.FMLNetworkConstants;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.Logger;

@Mod("arclight")
public class ArclightMod {

    public static final Logger LOGGER = ArclightI18nLogger.getLogger("Arclight");

    public ArclightMod() {
        LOGGER.info("mod-load");
        ArclightVersion.setVersion(ArclightVersion.v1_16_4);
        ArclightEventDispatcherRegistry.registerAllEventDispatchers();
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
            () -> new IExtensionPoint.DisplayTest(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        PermissionAPI.setPermissionHandler(ArclightPermissionHandler.INSTANCE);
    }
}
