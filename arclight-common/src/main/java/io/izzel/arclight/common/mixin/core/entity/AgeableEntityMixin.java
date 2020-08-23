package io.izzel.arclight.common.mixin.core.entity;

import net.minecraft.entity.AgeableEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(AgeableEntity.class)
public abstract class AgeableEntityMixin extends CreatureEntityMixin {

    // @formatter:off
    @Shadow public abstract boolean isChild();
    @Shadow @Nullable public abstract AgeableEntity func_241840_a(ServerWorld p_241840_1_, AgeableEntity p_241840_2_);
    @Shadow public abstract void setGrowingAge(int age);
    // @formatter:on

    public boolean ageLocked;

    @Inject(method = "writeAdditional", at = @At("RETURN"))
    private void arclight$writeAgeLocked(CompoundNBT compound, CallbackInfo ci) {
        compound.putBoolean("AgeLocked", ageLocked);
    }

    @Inject(method = "readAdditional", at = @At("RETURN"))
    private void arclight$readAgeLocked(CompoundNBT compound, CallbackInfo ci) {
        ageLocked = compound.getBoolean("AgeLocked");
    }

    @Redirect(method = "livingTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;isRemote:Z"))
    private boolean arclight$tickIfNotLocked(World world) {
        return world.isRemote || ageLocked;
    }
}
