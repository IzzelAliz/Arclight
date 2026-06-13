package io.izzel.arclight.common.mixin.vanilla.world.level.block;

import io.izzel.arclight.common.mixin.core.world.level.block.BlockMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.TNTPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
public abstract class TntBlockMixin_Vanilla extends BlockMixin {

    @Redirect(method = "playerWillDestroy", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TntBlock;explode(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void arclight$playerBreak(Level world, BlockPos pos,
                                      Level p_57445_, BlockPos blockPos, BlockState state, Player player) {
        if (CraftEventFactory.callTNTPrimeEvent(world, pos, TNTPrimeEvent.PrimeCause.BLOCK_BREAK, player, null)) {
            this.bridge$forge$onCaughtFire(state, world, pos, null, null);
        }
    }

    @Inject(method = "useItemOn", require = 0, cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TntBlock;explode(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$player(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!CraftEventFactory.callTNTPrimeEvent(level, blockPos, TNTPrimeEvent.PrimeCause.PLAYER, player, null)) {
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
}
