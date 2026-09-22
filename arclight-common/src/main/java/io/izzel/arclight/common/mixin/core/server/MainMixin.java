package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.BukkitOptionParser;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin {

    @Inject(method = "main([Ljava/lang/String;)V", at = @At("HEAD"), remap = false)
    private static void arclight$captureBukkitOptions(String[] args, CallbackInfo ci) {
        ArclightCaptures.captureBukkitOptions(new BukkitOptionParser().parse(args));
    }
}