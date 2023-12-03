package io.izzel.arclight.common.mixin.core.network.protocol.game;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSystemChatPacket.class)
public class ClientboundSystemChatPacketMixin {

    public void arclight$constructor(Component content, boolean overlay) {
        throw new RuntimeException();
    }

    public void arclight$constructor(String content, boolean overlay) {
        arclight$constructor(Component.Serializer.fromJson(content), overlay);
    }

    public void arclight$constructor(BaseComponent[] content, boolean overlay) {
        arclight$constructor(ComponentSerializer.toString(content), overlay);
    }

    @Inject(method = "<init>(Lnet/minecraft/network/chat/Component;Z)V", at = @At("RETURN"))
    private void arclight$init(Component content, boolean overlay, CallbackInfo ci) {
        this.content = Component.Serializer.toJson(content);
    }

    private String content;

    public String content() {
        return content;
    }
}
