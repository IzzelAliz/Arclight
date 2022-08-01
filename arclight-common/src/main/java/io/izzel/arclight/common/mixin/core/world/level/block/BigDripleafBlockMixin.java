package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BigDripleafBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BigDripleafBlock.class)
public class BigDripleafBlockMixin {

    @Shadow @Final private static EnumProperty<Tilt> TILT;

    @Inject(method = "onProjectileHit", cancellable = true, at = @At("HEAD"))
    private void arclight$projectileHit(Level level, BlockState state, BlockHitResult hitResult, Projectile projectile, CallbackInfo ci) {
        if (DistValidate.isValid(level) && CraftEventFactory.callEntityChangeBlockEvent(projectile, hitResult.getBlockPos(), state.setValue(TILT, Tilt.FULL)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "entityInside", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BigDripleafBlock;setTiltAndScheduleTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/properties/Tilt;Lnet/minecraft/sounds/SoundEvent;)V"))
    private void arclight$entityInteract(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!DistValidate.isValid(level)) return;
        org.bukkit.event.Cancellable cancellable;
        if (entity instanceof Player) {
            cancellable = CraftEventFactory.callPlayerInteractEvent((Player) entity, org.bukkit.event.block.Action.PHYSICAL, pos, null, null, null);
        } else {
            cancellable = new EntityInteractEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(level, pos));
            Bukkit.getPluginManager().callEvent((EntityInteractEvent) cancellable);
        }

        if (cancellable.isCancelled()) {
            ci.cancel();
            return;
        }
        if (CraftEventFactory.callEntityChangeBlockEvent(entity, pos, state.setValue(TILT, Tilt.FULL)).isCancelled()) {
            ci.cancel();
        }
    }
}
