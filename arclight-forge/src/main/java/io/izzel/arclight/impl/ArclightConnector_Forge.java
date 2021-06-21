package io.izzel.arclight.impl;

import io.izzel.arclight.common.mod.ArclightConnector;
import io.izzel.arclight.i18n.ArclightConfig;
import org.spongepowered.asm.mixin.Mixins;

@SuppressWarnings("unused")
public class ArclightConnector_Forge extends ArclightConnector {

    @Override
    public void connect() {
        super.connect();
        Mixins.addConfiguration("mixins.arclight.impl.forge.optimization.json");
        if (ArclightConfig.spec().getOptimization().isRemoveStream()) {
            Mixins.addConfiguration("mixins.arclight.impl.forge.optimization.stream.json");
        }
        if (ArclightConfig.spec().getOptimization().isDisableDFU()) {
            Mixins.addConfiguration("mixins.arclight.impl.forge.optimization.dfu.json");
        }
        LOGGER.info("mixin-load.optimization");
    }
}
