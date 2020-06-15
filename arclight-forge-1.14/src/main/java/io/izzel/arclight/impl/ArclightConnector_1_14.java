package io.izzel.arclight.impl;

import io.izzel.arclight.common.mod.ArclightConfig;
import io.izzel.arclight.common.mod.ArclightConnector;
import org.spongepowered.asm.mixin.Mixins;

public class ArclightConnector_1_14 extends ArclightConnector {

    @Override
    public void connect() {
        super.connect();
        Mixins.addConfiguration("mixins.arclight.impl.core.1_14.json");
        Mixins.addConfiguration("mixins.arclight.optimization.1_14.json");
        if (ArclightConfig.INSTANCE.optimizations.removeStreams) {
            Mixins.addConfiguration("mixins.arclight.optimization.stream.1_14.json");
        }
        LOGGER.info("Arclight optimization mixin added.");
    }
}
