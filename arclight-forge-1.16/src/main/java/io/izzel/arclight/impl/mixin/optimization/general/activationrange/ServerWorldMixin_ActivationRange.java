package io.izzel.arclight.impl.mixin.optimization.general.activationrange;

import io.izzel.arclight.impl.bridge.EntityBridge_ActivationRange;
import net.minecraft.entity.Entity;
import net.minecraft.world.server.ServerWorld;
import org.spigotmc.ActivationRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public class ServerWorldMixin_ActivationRange {

    @Inject(method = "tick", at = @At(value = "INVOKE", remap = false, target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;int2ObjectEntrySet()Lit/unimi/dsi/fastutil/objects/ObjectSet;"))
    private void activationRange$activateEntity(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ActivationRange.activateEntities((ServerWorld) (Object) this);
    }

    @Inject(method = "updateEntity", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;forceSetPosition(DDD)V"))
    private void activationRange$inactiveTick(Entity entityIn, CallbackInfo ci) {
        if (!ActivationRange.checkIfActive(entityIn)) {
            if (entityIn.addedToChunk) {
                ++entityIn.ticksExisted;
                if (entityIn.canUpdate()) {
                    ((EntityBridge_ActivationRange) entityIn).bridge$inactiveTick();
                }
            }
            ci.cancel();
        }
    }
}
