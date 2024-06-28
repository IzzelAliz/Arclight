package io.izzel.arclight.common.mixin.core.world.entity.animal.frog;

import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.Tadpole;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Tadpole.class)
public abstract class TadpoleMixin extends PathfinderMobMixin {

    // @formatter:off
    @Shadow protected abstract void setAge(int i);
    // @formatter:on

    @Inject(method = "ageUp()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/frog/Tadpole;discard()V"))
    private void arclight$ageUp(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.TRANSFORMATION);
    }

    @Decorate(method = "ageUp()V", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/frog/Tadpole;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private void arclight$transform(@Local(ordinal = -1) Frog frog) throws Throwable {
        if (CraftEventFactory.callEntityTransformEvent((Tadpole) (Object) this, frog, org.bukkit.event.entity.EntityTransformEvent.TransformReason.METAMORPHOSIS).isCancelled()) {
            this.setAge(0); // Sets the age to 0 for avoid a loop if the event is canceled
            DecorationOps.cancel().invoke();
            return;
        } else {
            ((ServerWorldBridge) this.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.METAMORPHOSIS);
        }
        DecorationOps.blackhole().invoke();
    }
}
