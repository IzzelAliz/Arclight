package io.izzel.arclight.impl;

import io.izzel.arclight.common.mod.ArclightConnector;
import io.izzel.arclight.i18n.ArclightConfig;
import org.spongepowered.asm.mixin.Mixins;

@SuppressWarnings("unused")
public class ArclightConnector_1_14 extends ArclightConnector {

    @Override
    public void connect() {
        super.connect();
        Mixins.addConfiguration("mixins.arclight.impl.core.1_14.json");
        Mixins.addConfiguration("mixins.arclight.optimization.1_14.json");
        if (ArclightConfig.spec().getOptimization().isRemoveStream()) {
            Mixins.addConfiguration("mixins.arclight.optimization.stream.1_14.json");
        }
        LOGGER.info("mixin-load.optimization");
    }
}
