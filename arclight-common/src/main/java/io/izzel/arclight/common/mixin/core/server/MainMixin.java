package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.server.Main;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.codec.DatapackCodec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Main.class)
public class MainMixin {

    @Redirect(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;func_240772_a_(Lnet/minecraft/resources/ResourcePackList;Lnet/minecraft/util/datafix/codec/DatapackCodec;Z)Lnet/minecraft/util/datafix/codec/DatapackCodec;"))
    private static DatapackCodec arclight$captureDatapackCodec(ResourcePackList p_240772_0_, DatapackCodec p_240772_1_, boolean p_240772_2_) {
        DatapackCodec datapackCodec = MinecraftServer.func_240772_a_(p_240772_0_, p_240772_1_, p_240772_2_);
        ArclightCaptures.captureDatapackConfig(datapackCodec);
        return datapackCodec;
    }
}
