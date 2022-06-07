package io.izzel.arclight.impl.mixin.optimization.stream;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.network.play.server.SSetPassengersPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(SSetPassengersPacket.class)
public class SSetPassengersPacketMixin_Optimize {

    @Redirect(method = "Lnet/minecraft/network/play/server/SSetPassengersPacket;<init>(Lnet/minecraft/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPassengers()Ljava/util/List;"))
    public List<Entity> arclight$getPassengers$updatePassenger(Entity instance) {
        return instance.passengers;
    }
}
