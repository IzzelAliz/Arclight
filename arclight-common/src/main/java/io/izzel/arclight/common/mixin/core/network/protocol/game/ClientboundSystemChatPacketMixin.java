package io.izzel.arclight.common.mixin.core.network.protocol.game;

import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientboundSystemChatPacket.class)
public class ClientboundSystemChatPacketMixin {

    @ShadowConstructor
    public void arclight$constructor(Component content, boolean overlay) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void arclight$constructor(BaseComponent[] content, boolean overlay) {
        arclight$constructor(Component.Serializer.fromJson(ComponentSerializer.toString(content)), overlay);
    }
}
