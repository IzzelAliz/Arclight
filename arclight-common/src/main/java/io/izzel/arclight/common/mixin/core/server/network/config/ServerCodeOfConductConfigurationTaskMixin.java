package io.izzel.arclight.common.mixin.core.server.network.config;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket;
import net.minecraft.server.network.config.ServerCodeOfConductConfigurationTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerCodeOfConductConfigurationTask.class)
public abstract class ServerCodeOfConductConfigurationTaskMixin {

    @Decorate(method = "start(Ljava/util/function/Consumer;)V", require = 1, expect = 1, allow = 1,
        at = @At(value = "NEW", target = "(Ljava/lang/String;)Lnet/minecraft/network/protocol/configuration/ClientboundCodeOfConductPacket;"))
    private ClientboundCodeOfConductPacket arclight$skipEmptyCodeOfConduct(String codeOfConduct) throws Throwable {
        if (codeOfConduct == null) {
            return (ClientboundCodeOfConductPacket) DecorationOps.cancel().invoke();
        }
        return (ClientboundCodeOfConductPacket) DecorationOps.callsite().invoke(codeOfConduct);
    }
}
