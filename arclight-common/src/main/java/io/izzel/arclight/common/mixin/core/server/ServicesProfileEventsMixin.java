package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import com.destroystokyo.paper.profile.PaperServicesDiscoveryService;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.SessionService;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import net.minecraft.server.Services;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Services.class)
public abstract class ServicesProfileEventsMixin {

    // Keep the supplied factory, including custom endpoints, proxy and key set.
    // Paper's event adapters delegate authentication and texture verification to it.
    @Decorate(method = "create(Lcom/mojang/authlib/services/MinecraftServicesDiscoveryService;Ljava/io/File;)Lnet/minecraft/server/Services;", at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/services/MinecraftServicesDiscoveryService;createMinecraftSessionService()Lcom/mojang/authlib/minecraft/SessionService;"), remap = false, require = 1, expect = 1, allow = 1)
    private static SessionService arclight$profileSessionService(MinecraftServicesDiscoveryService serviceAccess) throws Throwable {
        return PaperServicesDiscoveryService.wrapMinecraftSessionService((SessionService) DecorationOps.callsite().invoke(serviceAccess));
    }

    @Decorate(method = "create(Lcom/mojang/authlib/services/MinecraftServicesDiscoveryService;Ljava/io/File;)Lnet/minecraft/server/Services;", at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/services/MinecraftServicesDiscoveryService;createProfileRepository()Lcom/mojang/authlib/GameProfileRepository;"), remap = false, require = 1, expect = 1, allow = 1)
    private static GameProfileRepository arclight$profileRepository(MinecraftServicesDiscoveryService serviceAccess) throws Throwable {
        return PaperServicesDiscoveryService.wrapProfileRepository((GameProfileRepository) DecorationOps.callsite().invoke(serviceAccess));
    }
}
