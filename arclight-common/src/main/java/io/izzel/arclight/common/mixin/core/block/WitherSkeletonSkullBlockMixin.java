package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WitherSkeletonSkullBlock;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.SkullTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkeletonSkullBlock.class)
public class WitherSkeletonSkullBlockMixin {

    private static transient BlockStateListPopulator arclight$populator;
    private static transient boolean arclight$success = false;

    @Redirect(method = "checkWitherSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static boolean arclight$storeUpdate1(World world, BlockPos pos, BlockState newState, int flags) {
        if (arclight$populator == null) {
            arclight$populator = new BlockStateListPopulator(world);
        }
        return arclight$populator.setBlockState(pos, newState, flags);
    }

    @Redirect(method = "checkWitherSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playEvent(ILnet/minecraft/util/math/BlockPos;I)V"))
    private static void arclight$storeUpdate2(World world, int type, BlockPos pos, int data) {
        // do nothing
    }

    @Inject(method = "checkWitherSpawn", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private static void arclight$returnIfFail(World worldIn, BlockPos blockPos, SkullTileEntity tileEntity, CallbackInfo ci) {
        if (arclight$success) {
            if (arclight$populator != null) {
                for (BlockPos pos : arclight$populator.getBlocks()) {
                    worldIn.playEvent(Constants.WorldEvents.BREAK_BLOCK_EFFECTS, pos, Block.getStateId(worldIn.getBlockState(pos)));
                }
                arclight$populator.updateList();
            }
        } else {
            ci.cancel();
        }
        arclight$populator = null;
        arclight$success = false;
    }

    @Redirect(method = "checkWitherSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private static boolean arclight$spawnWither(World world, Entity entityIn) {
        ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BUILD_WITHER);
        return arclight$success = world.addEntity(entityIn);
    }
}
