package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CactusBlock.class)
public class CactusBlockMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    public boolean arclight$blockGrow(World world, BlockPos pos, BlockState state) {
        return CraftEventFactory.handleBlockGrowEvent(world, pos, state);
    }

    @Inject(method = "onEntityCollision", at = @At("HEAD"))
    private void arclight$cactusDamage1(BlockState state, World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        CraftEventFactory.blockDamage = CraftBlock.at(worldIn, pos);
    }

    @Inject(method = "onEntityCollision", at = @At("RETURN"))
    private void arclight$cactusDamage2(BlockState state, World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        CraftEventFactory.blockDamage = null;
    }
}
