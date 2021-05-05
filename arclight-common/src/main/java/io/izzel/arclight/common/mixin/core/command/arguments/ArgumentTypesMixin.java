package io.izzel.arclight.common.mixin.core.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import io.netty.buffer.Unpooled;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Logger;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(ArgumentTypes.class)
public abstract class ArgumentTypesMixin {

    // @formatter:off
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Nullable private static ArgumentTypes.Entry<?> get(ArgumentType<?> type) { return null; }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static <T extends ArgumentType<?>> void serialize(PacketBuffer buffer, T type) {
        ArgumentTypes.Entry<T> entry = (ArgumentTypes.Entry<T>) get(type);
        if (entry == null) {
            LOGGER.error("Could not serialize {} ({}) - will not be sent to client!", type, type.getClass());
            buffer.writeResourceLocation(new ResourceLocation(""));
        } else {
            String namespace = entry.id.getNamespace();
            boolean wrap = SpigotConfig.bungee && !(namespace.equals("minecraft") || namespace.equals("forge") || namespace.equals("brigadier"));
            if (wrap) {
                buffer.writeString("arclight:wrapped");
            }
            buffer.writeResourceLocation(entry.id);
            PacketBuffer buf = wrap ? new PacketBuffer(Unpooled.buffer()) : buffer;
            entry.serializer.write(type, buf);
            if (wrap) {
                buffer.writeVarInt(buf.writerIndex());
                buffer.writeBytes(buf);
            }
        }
    }
}
