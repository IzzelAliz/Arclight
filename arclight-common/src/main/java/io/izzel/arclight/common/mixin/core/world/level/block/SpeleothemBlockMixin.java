package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.world.level.block.SpeleothemBlock.THICKNESS;
import static net.minecraft.world.level.block.SpeleothemBlock.TIP_DIRECTION;
import static net.minecraft.world.level.block.SpeleothemBlock.WATERLOGGED;

@Mixin(SpeleothemBlock.class)
public class SpeleothemBlockMixin {

    @Inject(method = "onProjectileHit", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private void arclight$projectile(Level p_154042_, BlockState p_154043_, BlockHitResult hitResult, Projectile projectile, CallbackInfo ci) {
        if (!CraftEventFactory.callEntityChangeBlockEvent(projectile, hitResult.getBlockPos(), Blocks.AIR.defaultBlockState())) {
            ci.cancel();
        }
    }

    @Redirect(method = "createMergedTips", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SpeleothemBlock;createSpeleothem(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/properties/SpeleothemThickness;)V"))
    private void arclight$changeBlock1(SpeleothemBlock receiver, LevelAccessor level, BlockPos pos, Direction direction, SpeleothemThickness thickness,
                                       BlockState p_154231_, LevelAccessor p_154232_, BlockPos source) {
        var state = receiver.defaultBlockState().setValue(TIP_DIRECTION, direction).setValue(THICKNESS, thickness).setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
        CraftEventFactory.handleBlockSpreadEvent(level, source, pos, state, 3);
    }

    @Redirect(method = "grow", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SpeleothemBlock;createSpeleothem(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/properties/SpeleothemThickness;)V"))
    private void arclight$changeBlock2(SpeleothemBlock receiver, LevelAccessor level, BlockPos pos, Direction direction, SpeleothemThickness thickness,
                                       ServerLevel p_154036_, BlockPos source, Direction p_154038_) {
        var state = receiver.defaultBlockState().setValue(TIP_DIRECTION, direction).setValue(THICKNESS, thickness).setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
        CraftEventFactory.handleBlockSpreadEvent(level, source, pos, state, 3);
    }
}
