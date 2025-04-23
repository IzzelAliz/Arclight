package io.izzel.arclight.common.mixin.core.network.protocol.common.custom;

import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import io.izzel.arclight.common.mod.plugin.messaging.RawPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DiscardedPayload.class)
public abstract class DiscardedPayloadMixin implements RawPayload {

    @ShadowConstructor
    public void arclight$constructor(ResourceLocation rl) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void arclight$constructor(ResourceLocation rl, ByteBuf data) {
        this.arclight$constructor(rl);
        this.data = data;
    }

    @Unique private ByteBuf data;

    @Unique
    @Override
    public ByteBuf data() {
        return data.copy();
    }

    @Unique
    @Override
    public void setData(ByteBuf data) {
        this.data = data;
    }

    @Inject(method = "codec", at = @At("HEAD"), cancellable = true)
    private static<T extends FriendlyByteBuf> void arclight$interceptCodec(ResourceLocation location, int i, CallbackInfoReturnable<StreamCodec<T, CustomPacketPayload>> cir) {
        cir.setReturnValue(RawPayload.discardedCodec(location, i));
    }
}
