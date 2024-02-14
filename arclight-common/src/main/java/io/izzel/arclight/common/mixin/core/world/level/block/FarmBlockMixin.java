package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public abstract class FarmBlockMixin extends BlockMixin {

    @Inject(method = "turnToDirt", cancellable = true, at = @At("HEAD"))
    private static void arclight$blockFade(Entity entity, BlockState state, Level worldIn, BlockPos pos, CallbackInfo ci) {
        if (!DistValidate.isValid(worldIn)) return;
        if (entity != null) {
            Cancellable cancellable;
            if (entity instanceof Player) {
                cancellable = CraftEventFactory.callPlayerInteractEvent((Player) entity, Action.PHYSICAL, pos, null, null, null);
            } else {
                cancellable = new EntityInteractEvent(entity.bridge$getBukkitEntity(), CraftBlock.at(worldIn, pos));
                Bukkit.getPluginManager().callEvent((EntityInteractEvent) cancellable);
            }

            if (cancellable.isCancelled()) {
                ci.cancel();
                return;
            }

            if (!CraftEventFactory.callEntityChangeBlockEvent(entity, pos, Blocks.DIRT.defaultBlockState())) {
                ci.cancel();
                return;
            }
        }
        if (CraftEventFactory.callBlockFadeEvent(worldIn, pos, Blocks.DIRT.defaultBlockState()).isCancelled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean arclight$moistureChange(ServerLevel world, BlockPos pos, BlockState newState, int flags) {
        return CraftEventFactory.handleMoistureChangeEvent(world, pos, newState, flags);
    }
}
