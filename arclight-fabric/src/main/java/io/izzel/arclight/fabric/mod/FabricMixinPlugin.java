package io.izzel.arclight.fabric.mod;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.boot.AbstractBootstrap;
import io.izzel.arclight.common.mod.ArclightCommon;
import io.izzel.arclight.common.mod.ArclightMixinPlugin;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.i18n.ArclightLocale;
import io.izzel.arclight.mixin.MixinTools;
import org.slf4j.LoggerFactory;

public class FabricMixinPlugin extends ArclightMixinPlugin implements AbstractBootstrap {

    @Override
    public void onLoad(String mixinPackage) {
        ArclightCommon.setInstance(new FabricCommonImpl());
        super.onLoad(mixinPackage);
        MixinTools.setup();
        LoggerFactory.getLogger("Arclight").info(
            ArclightLocale.getInstance().format("i18n.using-language", ArclightConfig.spec().getLocale().getCurrent(), ArclightConfig.spec().getLocale().getFallback())
        );
        try {
            this.setupMod(ArclightPlatform.FABRIC, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
