package io.izzel.arclight.common.mod;

import cpw.mods.modlauncher.api.ITransformingClassLoader;
import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.common.mod.util.log.ArclightI18nLogger;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

import java.util.Arrays;
import java.util.List;

public class ArclightConnector implements IMixinConnector {

    public static final Logger LOGGER = ArclightI18nLogger.getLogger("Arclight");
    private static final List<String> FILTER_PACKAGE = Arrays.asList("com.google.common", "com.google.gson", "ninja.leaping.configurate",
        "io.izzel.arclight.api", "io.izzel.arclight.i18n");

    @Override
    public void connect() {
        ((ITransformingClassLoader) Thread.currentThread().getContextClassLoader()).addTargetPackageFilter(
            s -> FILTER_PACKAGE.stream().noneMatch(s::startsWith)
        );
        Mixins.addConfiguration("mixins.arclight.core.json");
        Mixins.addConfiguration("mixins.arclight.bukkit.json");
        Mixins.addConfiguration("mixins.arclight.forge.json");
        if (ArclightVersion.atLeast(ArclightVersion.v1_15)) {
            Mixins.addConfiguration("mixins.arclight.core.1_15.json");
        }
        LOGGER.info("mixin-load.core");
    }
}
