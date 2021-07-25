package io.izzel.arclight.common.mixin.core.network.protocol.game;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.util.UUID;

@Mixin(ClientboundChatPacket.class)
public class SChatPacketMixin {

    // @formatter:off
    @Shadow private Component message;
    @Shadow private ChatType type;
    @Shadow private UUID sender;
    // @formatter:on

    public BaseComponent[] components;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void write(FriendlyByteBuf buf) throws IOException {
        if (components != null) {
            buf.writeUtf(ComponentSerializer.toString(components));
        } else {
            buf.writeComponent(this.message);
        }
        buf.writeByte(this.type.getIndex());
        buf.writeUUID(this.sender);
    }
}
