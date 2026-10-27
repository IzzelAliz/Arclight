package io.izzel.arclight.common.mixin.core.world.entity;

import net.minecraft.world.entity.PathfinderMob;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathfinderMob.class)
public abstract class PathfinderMobMixin extends MobMixin implements io.izzel.arclight.common.bridge.core.world.entity.PathfinderMobBridge {

    public net.minecraft.core.BlockPos movingTarget;
    @Override public net.minecraft.core.BlockPos bridge$getMovingTarget() { return this.movingTarget; }
    @Override public void bridge$setMovingTarget(net.minecraft.core.BlockPos pos) { this.movingTarget = pos; }

}
