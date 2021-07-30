package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(RedStoneOreBlock.class)
public abstract class RedstoneOreBlockMixin {

    // @formatter:off
    @Shadow private static void interact(BlockState state, Level world, BlockPos pos) { }
    // @formatter:on

    private static transient Entity arclight$entity;

    @Inject(method = "attack", at = @At(value = "HEAD"))
    public void arclight$interact1(BlockState state, Level worldIn, BlockPos pos, Player player, CallbackInfo ci) {
        arclight$entity = player;
    }

    @Inject(method = "stepOn", cancellable = true, at = @At(value = "HEAD"))
    public void arclight$entityInteract(Level worldIn, BlockPos pos, BlockState state, Entity entityIn, CallbackInfo ci) {
        if (entityIn instanceof Player) {
            PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(((Player) entityIn), Action.PHYSICAL, pos, null, null, null);
            if (event.isCancelled()) {
                ci.cancel();
                return;
            }
        } else {
            EntityInteractEvent event = new EntityInteractEvent(((EntityBridge) entityIn).bridge$getBukkitEntity(), CraftBlock.at(worldIn, pos));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
                return;
            }
        }
        arclight$entity = entityIn;
    }

    @Inject(method = "use", at = @At(value = "HEAD"))
    public void arclight$interact3(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit, CallbackInfoReturnable<Boolean> cir) {
        arclight$entity = player;
    }

    @Inject(method = "randomTick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void arclight$blockFade(BlockState state, ServerLevel worldIn, BlockPos pos, Random random, CallbackInfo ci) {
        if (CraftEventFactory.callBlockFadeEvent(worldIn, pos, state.setValue(RedStoneOreBlock.LIT, false)).isCancelled()) {
            ci.cancel();
        }
    }

    private static void interact(BlockState blockState, Level world, BlockPos blockPos, Entity entity) {
        arclight$entity = entity;
        interact(blockState, world, blockPos);
    }

    @Inject(method = "interact", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static void arclight$entityChangeBlock(BlockState blockState, Level world, BlockPos blockPos, CallbackInfo ci) {
        if (CraftEventFactory.callEntityChangeBlockEvent(arclight$entity, blockPos, blockState.setValue(RedStoneOreBlock.LIT, true)).isCancelled()) {
            ci.cancel();
        }
        arclight$entity = null;
    }
}
