package io.izzel.arclight.common.mixin.optimization.dfu;

import com.mojang.datafixers.DataFixer;
import io.izzel.arclight.i18n.ArclightLocale;
import net.minecraft.server.Main;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(Main.class)
public abstract class MainMixin_DFU {

    @Inject(method = "forceUpgrade", at = @At("HEAD"))
    private static void arclight$skipConvert(LevelStorageSource.LevelStorageAccess levelSave, DataFixer dataFixer, boolean flag, BooleanSupplier b, WorldGenSettings settings, CallbackInfo ci) {
        throw new RuntimeException(ArclightLocale.getInstance().get("dfu-disable.map-convert"));
    }
}
