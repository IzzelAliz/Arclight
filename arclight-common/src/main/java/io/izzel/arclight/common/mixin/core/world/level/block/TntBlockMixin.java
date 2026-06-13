package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
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

    @Override
    public void bridge$forge$onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
        explode(level, pos, igniter);
    }
}
