package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CactusBlock.class)
public class CactusBlockMixin {

    @SuppressWarnings({"UnresolvedMixinReference", "UnnecessaryQualifiedMemberReference"})
    @Redirect(method = "Lnet/minecraft/block/CactusBlock;func_196267_b(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/World;func_175656_a(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"), require = 0)
    public boolean arclight$blockGrow_1_14(World world, BlockPos pos, BlockState state) {
        return CraftEventFactory.handleBlockGrowEvent(world, pos, state);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"), require = 0)
    private boolean arclight$blockGrow_1_15(ServerWorld serverWorld, BlockPos pos, BlockState state) {
        return CraftEventFactory.handleBlockGrowEvent(serverWorld, pos, state);
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
