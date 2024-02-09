package io.izzel.arclight.neoforge.mixin.forge;

import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.neoforge.mod.permission.ArclightPermissionHandler;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.handler.IPermissionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PermissionAPI.class, remap = false)
public abstract class PermissionAPIMixin {

    @Shadow private static IPermissionHandler activeHandler;

    @Inject(method = "initializePermissionAPI", at = @At("RETURN"))
    private static void arclight$init(CallbackInfo ci) {
        if (!ArclightConfig.spec().getCompat().isForwardPermission()) {
            return;
        }
        var handler = new ArclightPermissionHandler(activeHandler);
        ArclightServer.LOGGER.info("Forwarding forge permission[{}] to bukkit", activeHandler.getIdentifier());
        activeHandler = handler;
    }
}
