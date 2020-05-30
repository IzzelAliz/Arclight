package io.izzel.arclight.common.mixin.core.entity;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AgeableEntity.class)
public abstract class AgeableEntityMixin extends CreatureEntityMixin {

    // @formatter:off
    @Shadow public abstract boolean isChild();
    // @formatter:on

    public boolean ageLocked;

    @Inject(method = "processInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$spawnReason(PlayerEntity player, Hand hand, CallbackInfoReturnable<Boolean> cir) {
        ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
    }

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
