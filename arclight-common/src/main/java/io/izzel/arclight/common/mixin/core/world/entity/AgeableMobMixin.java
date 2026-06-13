package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.world.entity.AgeableMobBridge;
import io.izzel.arclight.common.mod.util.ArclightNbtHelper;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(AgeableMob.class)
public abstract class AgeableMobMixin extends PathfinderMobMixin implements AgeableMobBridge {

    // @formatter:off
    @Shadow public abstract boolean isBaby();
    @Shadow @Nullable public abstract AgeableMob getBreedOffspring(ServerLevel world, AgeableMob mate);
    @Shadow public abstract void setAge(int age);
    // @formatter:on

    public boolean ageLocked;

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void arclight$writeAgeLocked(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("AgeLocked", ageLocked);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void arclight$readAgeLocked(ValueInput input, CallbackInfo ci) {
        ageLocked = ArclightNbtHelper.getBoolean(input, "AgeLocked");
    }

    @Redirect(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/Level;isClientSide:Z"))
    private boolean arclight$tickIfNotLocked(Level world) {
        return !(world instanceof net.minecraft.server.level.ServerLevel) || ageLocked;
    }

    @Override
    public boolean bridge$isAgeLocked() {
        return this.ageLocked;
    }
}
