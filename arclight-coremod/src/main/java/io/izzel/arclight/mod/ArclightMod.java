package io.izzel.arclight.mod;

import io.izzel.arclight.mod.server.event.ArclightEventDispatcherRegistry;
import net.minecraftforge.fml.CrashReportExtender;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v1_14_R1.CraftCrashReport;

@Mod("arclight")
public class ArclightMod {

    public static final Logger LOGGER = LogManager.getLogger("Arclight");

    public ArclightMod() {
        LOGGER.info("Arclight Mod loaded.");
        ArclightEventDispatcherRegistry.registerAllEventDispatchers();
        CrashReportExtender.registerCrashCallable("Arclight", () -> new CraftCrashReport().call().toString());
    }
}
