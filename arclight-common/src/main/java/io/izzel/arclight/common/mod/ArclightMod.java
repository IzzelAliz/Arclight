package io.izzel.arclight.common.mod;

import io.izzel.arclight.common.mod.server.ArclightPermissionHandler;
import io.izzel.arclight.common.mod.server.event.ArclightEventDispatcherRegistry;
import io.izzel.arclight.common.mod.util.log.ArclightI18nLogger;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

@Mod("arclight")
public class ArclightMod {

    public static final Logger LOGGER = ArclightI18nLogger.getLogger("Arclight");

    public ArclightMod() {
        LOGGER.info("mod-load");
        ArclightEventDispatcherRegistry.registerAllEventDispatchers();
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        PermissionAPI.setPermissionHandler(ArclightPermissionHandler.INSTANCE);
    }
}
