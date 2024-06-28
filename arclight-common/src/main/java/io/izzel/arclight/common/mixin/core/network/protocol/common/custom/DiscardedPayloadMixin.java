package io.izzel.arclight.common.mixin.core.network.protocol.common.custom;

import io.izzel.arclight.common.bridge.core.network.common.DiscardedPayloadBridge;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DiscardedPayload.class)
public class DiscardedPayloadMixin implements DiscardedPayloadBridge {

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

    @Override
    public void bridge$setData(ByteBuf data) {
        this.data = data;
    }

    @Override
    public ByteBuf bridge$getData() {
        return this.data;
    }
}
