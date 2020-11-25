package io.izzel.arclight.common.mod;

import cpw.mods.modlauncher.api.ITransformingClassLoader;
import io.izzel.arclight.common.mod.util.log.ArclightI18nLogger;
import io.izzel.arclight.mixin.injector.EjectorInfo;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

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
        InjectionInfo.register(EjectorInfo.class);
        Mixins.addConfiguration("mixins.arclight.core.json");
        Mixins.addConfiguration("mixins.arclight.bukkit.json");
        Mixins.addConfiguration("mixins.arclight.forge.json");
        LOGGER.info("mixin-load.core");
    }
}
