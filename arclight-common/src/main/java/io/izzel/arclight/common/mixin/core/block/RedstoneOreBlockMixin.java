package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneOreBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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

@Mixin(RedstoneOreBlock.class)
public abstract class RedstoneOreBlockMixin {

    // @formatter:off
    @Shadow private static void activate(BlockState state, World world, BlockPos pos) { }
    // @formatter:on

    private static transient Entity arclight$entity;

    @Inject(method = "onBlockClicked", at = @At(value = "HEAD"))
    public void arclight$interact1(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, CallbackInfo ci) {
        arclight$entity = player;
    }

    @Inject(method = "onEntityWalk", cancellable = true, at = @At(value = "HEAD"))
    public void arclight$entityInteract(World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        if (entityIn instanceof PlayerEntity) {
            PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(((PlayerEntity) entityIn), Action.PHYSICAL, pos, null, null, null);
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

    @Inject(method = "onBlockActivated", at = @At(value = "HEAD"))
    public void arclight$interact3(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit, CallbackInfoReturnable<Boolean> cir) {
        arclight$entity = player;
    }

    @Inject(method = "randomTick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void arclight$blockFade(BlockState state, ServerWorld worldIn, BlockPos pos, Random random, CallbackInfo ci) {
        if (CraftEventFactory.callBlockFadeEvent(worldIn, pos, state.with(RedstoneOreBlock.LIT, false)).isCancelled()) {
            ci.cancel();
        }
    }

    private static void interact(BlockState blockState, World world, BlockPos blockPos, Entity entity) {
        arclight$entity = entity;
        activate(blockState, world, blockPos);
    }

    @Inject(method = "activate", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static void arclight$entityChangeBlock(BlockState blockState, World world, BlockPos blockPos, CallbackInfo ci) {
        if (CraftEventFactory.callEntityChangeBlockEvent(arclight$entity, blockPos, blockState.with(RedstoneOreBlock.LIT, true)).isCancelled()) {
            ci.cancel();
        }
        arclight$entity = null;
    }
}
