package io.izzel.arclight.impl;

import io.izzel.arclight.common.mod.ArclightConnector;

@SuppressWarnings("unused")
public class ArclightConnector_1_16 extends ArclightConnector {

    @Override
    public void connect() {
        super.connect();
        /*
        if (ArclightConfig.spec().getOptimization().isRemoveStream()) {
            Mixins.addConfiguration("mixins.arclight.impl.optimization.stream.1_15.json");
        }
        LOGGER.info("mixin-load.optimization");*/
    }
}
