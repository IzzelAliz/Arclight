package io.izzel.arclight.common.mixin.core.server.packs.repository;

import io.izzel.arclight.common.bridge.core.server.packs.repository.PackRepositoryBridge;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Mixin(ServerPacksSource.class)
public abstract class ServerPacksSourceMixin {

    @Inject(method = "createPackRepository(Ljava/nio/file/Path;Lnet/minecraft/world/level/validation/DirectoryValidator;)Lnet/minecraft/server/packs/repository/PackRepository;",
        at = @At("RETURN"), require = 1)
    private static void arclight$bindDatapackValidator(Path datapackDir, DirectoryValidator validator,
            CallbackInfoReturnable<PackRepository> cir) {
        // RETURN preserves NeoForge's populatePackRepository and all mod sources.
        ((PackRepositoryBridge) cir.getReturnValue()).bridge$setDatapackValidator(validator);
    }

    @Inject(method = "createVanillaTrustedRepository()Lnet/minecraft/server/packs/repository/PackRepository;",
        at = @At("RETURN"), require = 1)
    private static void arclight$bindTrustedDatapackValidator(CallbackInfoReturnable<PackRepository> cir) {
        // This factory intentionally uses the same allow-all policy as native code.
        ((PackRepositoryBridge) cir.getReturnValue()).bridge$setDatapackValidator(new DirectoryValidator(path -> true));
    }
}
