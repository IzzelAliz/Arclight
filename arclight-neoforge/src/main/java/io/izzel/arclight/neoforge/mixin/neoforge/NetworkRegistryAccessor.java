package io.izzel.arclight.neoforge.mixin.neoforge;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = NetworkRegistry.class, remap = false)
public interface NetworkRegistryAccessor {
    @Accessor("PAYLOAD_REGISTRATIONS")
    static Map<ConnectionProtocol, Map<Identifier, PayloadRegistration<?>>> getRegistration() {
        throw new AbstractMethodError();
    }

    @Accessor("BUILTIN_PAYLOADS")
    static Map<Identifier, StreamCodec<?, ?>> getBuiltinPayload() {
        throw new AbstractMethodError();
    }

    @Accessor("SERVERBOUND_HANDLERS")
    static Map<ConnectionProtocol, Map<Identifier, IPayloadHandler<?>>> getServerboundHandlers() {
        throw new AbstractMethodError();
    }

    @Accessor("CLIENTBOUND_HANDLERS")
    static Map<ConnectionProtocol, Map<Identifier, IPayloadHandler<?>>> getClientboundHandlers() {
        throw new AbstractMethodError();
    }
}
