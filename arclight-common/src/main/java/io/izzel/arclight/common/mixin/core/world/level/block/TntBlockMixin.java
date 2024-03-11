package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.TNTPrimeEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
public abstract class TntBlockMixin extends BlockMixin {

    // @formatter:off
    @Shadow private static void explode(Level arg, BlockPos arg2, @Nullable LivingEntity arg3) {}
    // @formatter:on

    @Redirect(method = "onPlace", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean arclight$redstone1(Level instance, BlockPos pos) {
        return instance.hasNeighborSignal(pos) && CraftEventFactory.callTNTPrimeEvent(instance, pos, TNTPrimeEvent.PrimeCause.REDSTONE, null, null);
    }

    @Redirect(method = "neighborChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean arclight$redstone2(Level instance, BlockPos pos, BlockState p_57457_, Level p_57458_, BlockPos p_57459_, Block p_57460_, BlockPos source) {
        return instance.hasNeighborSignal(pos) && CraftEventFactory.callTNTPrimeEvent(instance, pos, TNTPrimeEvent.PrimeCause.REDSTONE, null, source);
    }

    @Redirect(method = "playerWillDestroy", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TntBlock;explode(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void arclight$playerBreak(Level world, BlockPos pos,
                                      Level p_57445_, BlockPos blockPos, BlockState state, Player player) {
        if (CraftEventFactory.callTNTPrimeEvent(world, pos, TNTPrimeEvent.PrimeCause.BLOCK_BREAK, player, null)) {
            this.bridge$forge$onCaughtFire(state, world, pos, null, null);
        }
    }

    @Inject(method = "use", require = 0, cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TntBlock;explode(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$player(BlockState p_57450_, Level level, BlockPos pos, Player player, InteractionHand p_57454_, BlockHitResult p_57455_, CallbackInfoReturnable<InteractionResult> cir) {
        if (!CraftEventFactory.callTNTPrimeEvent(level, pos, TNTPrimeEvent.PrimeCause.PLAYER, player, null)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "onProjectileHit", require = 0, cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TntBlock;explode(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)V"))
    public void arclight$entityChangeBlock(Level worldIn, BlockState state, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        if (!CraftEventFactory.callEntityChangeBlockEvent(projectile, hit.getBlockPos(), Blocks.AIR.defaultBlockState())
            || !CraftEventFactory.callTNTPrimeEvent(worldIn, hit.getBlockPos(), TNTPrimeEvent.PrimeCause.PROJECTILE, projectile, null)) {
            ci.cancel();
        }
    }

    @Override
    public void bridge$forge$onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
        explode(level, pos, igniter);
    }
}
