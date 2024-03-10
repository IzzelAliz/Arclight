package io.izzel.arclight.neoforge.mod;

import io.izzel.arclight.common.mod.ArclightCommon;
import io.izzel.arclight.common.mod.ArclightMixinPlugin;

public class NeoForgeMixinPlugin extends ArclightMixinPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        ArclightCommon.setInstance(new NeoForgeCommonImpl());
    }
}
