package io.izzel.arclight.neoforge.mixin.neoforge;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = NetworkRegistry.class, remap = false)
public interface NetworkRegistryAccessor {
    @Accessor("PAYLOAD_REGISTRATIONS")
    static Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>> getRegistration() {
        throw new AbstractMethodError();
    }
}
