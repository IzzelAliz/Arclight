package io.izzel.arclight.forge.mod;

import io.izzel.arclight.common.mod.ArclightCommon;
import io.izzel.arclight.common.mod.ArclightMixinPlugin;

public class ForgeMixinPlugin extends ArclightMixinPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        ArclightCommon.setInstance(new ForgeCommonImpl());
    }
}
