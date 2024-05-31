package io.izzel.arclight.common.mod;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.common.mod.util.log.ArclightI18nLogger;
import io.izzel.arclight.mixin.MixinTools;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class ArclightConnector implements IMixinConnector {

    public static final Logger LOGGER = ArclightI18nLogger.getLogger("Arclight");

    @Override
    public void connect() {
        MixinTools.setup();
        Mixins.addConfiguration("mixins.arclight.core.json");
        Mixins.addConfiguration("mixins.arclight.bukkit.json");
        switch (ArclightPlatform.current()) {
            case VANILLA -> Mixins.addConfiguration("mixins.arclight.vanilla.json");
            case FORGE -> Mixins.addConfiguration("mixins.arclight.forge.json");
            case NEOFORGE -> Mixins.addConfiguration("mixins.arclight.neoforge.json");
        }
        LOGGER.info("mixin-load.core");
        Mixins.addConfiguration("mixins.arclight.impl.optimization.json");
        LOGGER.info("mixin-load.optimization");
    }
}
