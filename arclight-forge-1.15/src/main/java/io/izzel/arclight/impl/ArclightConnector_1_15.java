package io.izzel.arclight.impl;

import io.izzel.arclight.common.mod.ArclightConnector;
import io.izzel.arclight.i18n.ArclightConfig;
import org.spongepowered.asm.mixin.Mixins;

@SuppressWarnings("unused")
public class ArclightConnector_1_15 extends ArclightConnector {

    @Override
    public void connect() {
        super.connect();
        if (ArclightConfig.spec().getOptimization().isRemoveStream()) {
            Mixins.addConfiguration("mixins.arclight.impl.optimization.stream.1_15.json");
        }
        LOGGER.info("mixin-load.optimization");
    }
}
