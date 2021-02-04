package io.izzel.arclight.impl;

import io.izzel.arclight.common.mod.ArclightConnector;
import io.izzel.arclight.i18n.ArclightConfig;
import org.spongepowered.asm.mixin.Mixins;

@SuppressWarnings("unused")
public class ArclightConnector_1_16 extends ArclightConnector {

    @Override
    public void connect() {
        super.connect();
        Mixins.addConfiguration("mixins.arclight.impl.optimization.1_16.json");
        if (ArclightConfig.spec().getOptimization().isRemoveStream()) {
            Mixins.addConfiguration("mixins.arclight.impl.optimization.stream.1_16.json");
        }
        LOGGER.info("mixin-load.optimization");
    }
}
