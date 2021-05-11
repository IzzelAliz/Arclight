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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(ArgumentTypes.class)
public abstract class ArgumentTypesMixin {

    // @formatter:off
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Nullable private static ArgumentTypes.Entry<?> get(ArgumentType<?> type) { return null; }
    @Shadow @Final private static Map<ResourceLocation, ArgumentTypes.Entry<?>> ID_TYPE_MAP;
    // @formatter:on

    private static final Set<ResourceLocation> INTERNAL_TYPES = new HashSet<>();

    @Inject(method = "registerArgumentTypes", at = @At("HEAD"))
    private static void arclight$beginRegister(CallbackInfo ci) {
        INTERNAL_TYPES.addAll(ID_TYPE_MAP.keySet());
    }

    @Inject(method = "registerArgumentTypes", at = @At("RETURN"))
    private static void arclight$endRegister(CallbackInfo ci) {
        HashSet<ResourceLocation> set = new HashSet<>(ID_TYPE_MAP.keySet());
        set.removeAll(INTERNAL_TYPES);
        INTERNAL_TYPES.clear();
        INTERNAL_TYPES.addAll(set);
        INTERNAL_TYPES.add(new ResourceLocation("forge", "enum"));
        INTERNAL_TYPES.add(new ResourceLocation("forge", "modid"));
    }

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
            boolean wrap = SpigotConfig.bungee && !INTERNAL_TYPES.contains(entry.id);
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
