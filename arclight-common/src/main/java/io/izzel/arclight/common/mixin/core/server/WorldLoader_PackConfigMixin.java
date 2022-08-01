package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.DataPackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldLoader.PackConfig.class)
public class WorldLoader_PackConfigMixin {

    @Redirect(method = "createResourceManager", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;configurePackRepository(Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/world/level/DataPackConfig;Z)Lnet/minecraft/world/level/DataPackConfig;"))
    private DataPackConfig arclight$capturePack(PackRepository s, DataPackConfig s1, boolean pack) {
        var dataPackConfig = MinecraftServer.configurePackRepository(s, s1, pack);
        ArclightCaptures.captureDatapackConfig(dataPackConfig);
        return dataPackConfig;
    }
}
