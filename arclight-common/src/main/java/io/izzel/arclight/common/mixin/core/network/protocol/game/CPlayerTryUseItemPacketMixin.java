package io.izzel.arclight.common.mixin.core.network.protocol.game;

import io.izzel.arclight.common.bridge.core.network.play.TimestampedPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundUseItemPacket.class)
public class CPlayerTryUseItemPacketMixin implements TimestampedPacket {

    public long timestamp;

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("RETURN"))
    private void arclight$read(FriendlyByteBuf buf, CallbackInfo ci) {
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public long bridge$timestamp() {
        return timestamp;
    }
}
