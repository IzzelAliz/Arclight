package io.izzel.arclight.common.mixin.core.network.protocol.game;

import com.mojang.brigadier.arguments.ArgumentType;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.util.VelocitySupport;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundCommandsPacket$ArgumentNodeStub")
public class ClientboundCommandsPacket_ArgumentNodeStubMixin {

    private static final int ARCLIGHT_WRAP_INDEX = -256;

    @Inject(method = "serializeCap(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo$Template;)V",
        cancellable = true, at = @At("HEAD"))
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void arclight$wrapArgument(FriendlyByteBuf buf, ArgumentTypeInfo<A, T> type, ArgumentTypeInfo.Template<A> node, CallbackInfo ci) {
        if (!(SpigotConfig.bungee || VelocitySupport.isEnabled())) {
            return;
        }
        var key = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(type);
        if ((key != null) && (key.getNamespace().equals("minecraft") || key.getNamespace().equals("brigadier"))) {
            return;
        }
        ci.cancel();
        buf.writeVarInt(ARCLIGHT_WRAP_INDEX);
        var id = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(type);
        if (id == -1) {
            ArclightServer.LOGGER.debug("Command argument type {} is not registered", type);
        }
        buf.writeVarInt(id);
        var payload = new FriendlyByteBuf(Unpooled.buffer());
        type.serializeToNetwork((T) node, payload);
        buf.writeVarInt(payload.readableBytes());
        buf.writeBytes(payload);
    }
}
