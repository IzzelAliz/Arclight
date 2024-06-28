package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkSensorBlock.class)
public class SculkSensorBlockMixin {

    @Inject(method = "stepOn", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private void arclight$stepOn(Level level, BlockPos pos, BlockState p_222134_, Entity entity, CallbackInfo ci) {
        org.bukkit.event.Cancellable cancellable;
        if (entity instanceof Player) {
            cancellable = CraftEventFactory.callPlayerInteractEvent((Player) entity, org.bukkit.event.block.Action.PHYSICAL, pos, null, null, null);
        } else {
            cancellable = new org.bukkit.event.entity.EntityInteractEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(level, pos));
            Bukkit.getPluginManager().callEvent((org.bukkit.event.entity.EntityInteractEvent) cancellable);
        }
        if (cancellable.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "deactivate", cancellable = true, at = @At("HEAD"))
    private static void arclight$deactivate(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(CraftBlock.at(level, pos), state.getValue(SculkSensorBlock.POWER), 0);
        Bukkit.getPluginManager().callEvent(eventRedstone);

        if (eventRedstone.getNewCurrent() > 0) {
            level.setBlock(pos, state.setValue(SculkSensorBlock.POWER, eventRedstone.getNewCurrent()), 3);
            ci.cancel();
        }
    }

    @Decorate(method = "activate", inject = true, at = @At("HEAD"))
    private void arclight$activate(Entity p_222126_, Level level, BlockPos pos, BlockState state, int i, int j) throws Throwable {
        BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(CraftBlock.at(level, pos), state.getValue(SculkSensorBlock.POWER), i);
        Bukkit.getPluginManager().callEvent(eventRedstone);
        if (eventRedstone.getNewCurrent() <= 0) {
            DecorationOps.cancel().invoke();
            return;
        }
        i = eventRedstone.getNewCurrent();
        DecorationOps.blackhole().invoke(i);
    }
}
