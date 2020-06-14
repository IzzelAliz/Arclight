package io.izzel.arclight.impl.mixin.v1_14.entity.passive;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.impl.mixin.v1_14.entity.EntityMixin_1_14;
import net.minecraft.entity.passive.OcelotEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OcelotEntity.class)
public abstract class OcelotEntityMixin_1_14 extends EntityMixin_1_14 {

    @Inject(method = "func_213525_dW", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$spawnBaby(CallbackInfo ci) {
        ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.OCELOT_BABY);
    }
}
