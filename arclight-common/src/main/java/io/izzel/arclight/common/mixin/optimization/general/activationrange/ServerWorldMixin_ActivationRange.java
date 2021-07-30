package io.izzel.arclight.common.mixin.optimization.general.activationrange;

import io.izzel.arclight.common.bridge.optimization.EntityBridge_ActivationRange;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spigotmc.ActivationRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public class ServerWorldMixin_ActivationRange {

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerLevel;entityTickList:Lnet/minecraft/world/level/entity/EntityTickList;"))
    private void activationRange$activateEntity(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ActivationRange.activateEntities((ServerLevel) (Object) this);
    }

    @Inject(method = "tickNonPassenger", cancellable = true, at = @At(value = "HEAD"))
    private void activationRange$inactiveTick(Entity entityIn, CallbackInfo ci) {
        if (!ActivationRange.checkIfActive(entityIn)) {
            ++entityIn.tickCount;
            if (entityIn.canUpdate()) {
                ((EntityBridge_ActivationRange) entityIn).bridge$inactiveTick();
            }
            ci.cancel();
        }
    }
}
