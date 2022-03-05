package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.DataPackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldStem.class)
public class WorldStemMixin {

    @Redirect(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;configurePackRepository(Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/world/level/DataPackConfig;Z)Lnet/minecraft/world/level/DataPackConfig;"))
    private static DataPackConfig arclight$captureDatapackCodec(PackRepository p_240772_0_, DataPackConfig p_240772_1_, boolean p_240772_2_) {
        DataPackConfig datapackCodec = MinecraftServer.configurePackRepository(p_240772_0_, p_240772_1_, p_240772_2_);
        ArclightCaptures.captureDatapackConfig(datapackCodec);
        return datapackCodec;
    }
}
