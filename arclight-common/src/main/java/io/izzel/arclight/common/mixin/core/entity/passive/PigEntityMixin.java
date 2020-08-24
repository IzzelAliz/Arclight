package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.monster.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PigEntity.class)
public abstract class PigEntityMixin extends AnimalEntityMixin {

    @Inject(method = "func_241841_a", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$pigZap(ServerWorld p_241841_1_, LightningBoltEntity lightningBolt, CallbackInfo ci, ZombifiedPiglinEntity piglin) {
        if (CraftEventFactory.callPigZapEvent((PigEntity) (Object) this, lightningBolt, piglin).isCancelled()) {
            ci.cancel();
        } else {
            ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.LIGHTNING);
        }
    }
}
