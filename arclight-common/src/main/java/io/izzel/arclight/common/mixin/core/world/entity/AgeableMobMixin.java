package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.world.entity.AgeableMobBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(AgeableMob.class)
public abstract class AgeableMobMixin extends PathfinderMobMixin implements AgeableMobBridge {

    // @formatter:off
    @Shadow public abstract boolean isBaby();
    @Shadow @Nullable public abstract AgeableMob getBreedOffspring(ServerLevel world, AgeableMob mate);
    @Shadow public abstract void setAge(int age);
    @Shadow public abstract boolean isAgeLocked();
    // @formatter:on

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isClientSide()Z"))
    private boolean arclight$tickIfNotLocked(Level world) {
        return world.isClientSide() || this.isAgeLocked();
    }

    @Override
    public boolean bridge$isAgeLocked() {
        return this.isAgeLocked();
    }
}
