package io.izzel.arclight.common.mixin.core.network.play.server;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.util.UUID;

@Mixin(SChatPacket.class)
public class SChatPacketMixin {

    // @formatter:off
    @Shadow private ITextComponent chatComponent;
    @Shadow private ChatType type;
    @Shadow private UUID field_240809_c_;
    // @formatter:on

    public BaseComponent[] components;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void writePacketData(PacketBuffer buf) throws IOException {
        if (components != null) {
            buf.writeString(ComponentSerializer.toString(components));
        } else {
            buf.writeTextComponent(this.chatComponent);
        }
        buf.writeByte(this.type.getId());
        buf.writeUniqueId(this.field_240809_c_);
    }
}
