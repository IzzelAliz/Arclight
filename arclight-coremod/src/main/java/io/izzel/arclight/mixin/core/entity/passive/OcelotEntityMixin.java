package io.izzel.arclight.mixin.core.entity.passive;

import io.izzel.arclight.bridge.world.WorldBridge;
import net.minecraft.entity.passive.OcelotEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OcelotEntity.class)
public abstract class OcelotEntityMixin extends AnimalEntityMixin {

    // @formatter:off
    @Shadow protected abstract boolean isTrusting();
    // @formatter:on

    public boolean spawnBonus = true;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean canDespawn(double distanceToClosestPlayer) {
        return !this.isTrusting() /*&& this.ticksExisted > 2400*/;
    }

    @Inject(method = "func_213525_dW", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$spawnBaby(CallbackInfo ci) {
        ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.OCELOT_BABY);
    }
}
