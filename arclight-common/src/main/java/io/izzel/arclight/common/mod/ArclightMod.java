package io.izzel.arclight.common.mod;

import io.izzel.arclight.common.mod.server.event.ArclightEventDispatcherRegistry;
import io.izzel.arclight.common.mod.util.log.ArclightI18nLogger;
import net.minecraftforge.fml.CrashReportExtender;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v.CraftCrashReport;

@Mod("arclight")
public class ArclightMod {

    public static final Logger LOGGER = ArclightI18nLogger.getLogger("Arclight");

    public ArclightMod() {
        LOGGER.info("mod-load");
        ArclightEventDispatcherRegistry.registerAllEventDispatchers();
        CrashReportExtender.registerCrashCallable("Arclight", () -> new CraftCrashReport().call().toString());
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }
}
