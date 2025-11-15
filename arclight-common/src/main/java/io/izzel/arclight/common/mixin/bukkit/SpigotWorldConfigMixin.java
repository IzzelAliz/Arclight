package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.mod.server.world.ArclightWorldConfig;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SpigotWorldConfig.class, remap = false)
public class SpigotWorldConfigMixin {

    @Shadow @Final private String worldName;

    @SuppressWarnings("StringEquality")
    @Decorate(method = "log", inject = true, at = @At("HEAD"))
    private void arclight$skipLog(String content) throws Throwable {
        if (worldName == ArclightWorldConfig.DEFAULT_MARKER) {
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.blackhole().invoke();
    }
}
