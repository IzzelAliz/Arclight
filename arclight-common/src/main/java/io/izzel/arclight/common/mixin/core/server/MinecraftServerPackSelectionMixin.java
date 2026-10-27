package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.common.bridge.core.server.packs.repository.PackRepositoryBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerPackSelectionMixin {

    // Query reloads and successful resource-adoption setSelected calls deliberately
    // remain on the native no-flag path: neither is allowed to force plugin packs.

    @Redirect(method = "configurePackRepository(Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/world/level/WorldDataConfiguration;ZZ)Lnet/minecraft/world/level/WorldDataConfiguration;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;reload()V"),
        require = 1, expect = 1, allow = 1)
    private static void arclight$discoverStartupPacks(PackRepository repository) {
        ((PackRepositoryBridge) repository).bridge$reload(true);
    }

    @Redirect(method = {
        "configureRepositoryWithSelection(Lnet/minecraft/server/packs/repository/PackRepository;Ljava/util/Collection;Lnet/minecraft/world/flag/FeatureFlagSet;Z)Lnet/minecraft/world/level/WorldDataConfiguration;",
        "enableForcedFeaturePacks(Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/world/flag/FeatureFlagSet;)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;setSelected(Ljava/util/Collection;)V"),
        require = 2, expect = 2, allow = 2)
    private static void arclight$selectStartupPacks(PackRepository repository, Collection<String> packs) {
        // Keep native/loader selection, forced features and safe-mode policy intact.
        // Only these pre-load selections may force plugin auto-enabled packs back in.
        ((PackRepositoryBridge) repository).bridge$setSelected(packs, true);
    }
}
