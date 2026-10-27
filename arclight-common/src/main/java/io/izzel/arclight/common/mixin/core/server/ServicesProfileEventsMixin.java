package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import com.destroystokyo.paper.profile.PaperAuthenticationService;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.server.Services;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Services.class)
public abstract class ServicesProfileEventsMixin {

    // Keep the supplied factory, including custom endpoints, proxy and key set.
    // Paper's event adapters delegate authentication and texture verification to it.
    @Decorate(method = "create(Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Ljava/io/File;)Lnet/minecraft/server/Services;", at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;createMinecraftSessionService()Lcom/mojang/authlib/minecraft/MinecraftSessionService;"), remap = false, require = 1, expect = 1, allow = 1)
    private static MinecraftSessionService arclight$profileSessionService(YggdrasilAuthenticationService serviceAccess) throws Throwable {
        return PaperAuthenticationService.wrapMinecraftSessionService((MinecraftSessionService) DecorationOps.callsite().invoke(serviceAccess));
    }

    @Decorate(method = "create(Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Ljava/io/File;)Lnet/minecraft/server/Services;", at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;createProfileRepository()Lcom/mojang/authlib/GameProfileRepository;"), remap = false, require = 1, expect = 1, allow = 1)
    private static GameProfileRepository arclight$profileRepository(YggdrasilAuthenticationService serviceAccess) throws Throwable {
        return PaperAuthenticationService.wrapProfileRepository((GameProfileRepository) DecorationOps.callsite().invoke(serviceAccess));
    }
}
