package io.izzel.arclight.common.mod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

import java.nio.file.Paths;

public class ArclightConnector implements IMixinConnector {

    public static final Logger LOGGER = LogManager.getLogger("Arclight");

    @Override
    public void connect() {
        Mixins.addConfiguration("mixins.arclight.core.json");
        Mixins.addConfiguration("mixins.arclight.bukkit.json");
        Mixins.addConfiguration("mixins.arclight.forge.json");
        LOGGER.info("Arclight core mixin added.");
        ArclightConfig.init(Paths.get("arclight.yml"));
        Mixins.addConfiguration("mixins.arclight.optimization.json");
        if (ArclightConfig.INSTANCE.optimizations.removeStreams) {
            Mixins.addConfiguration("mixins.arclight.optimization.stream.json");
        }
        LOGGER.info("Arclight optimization mixin added.");
    }
}
