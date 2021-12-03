package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkullBlock.class)
public class WitherSkullBlockMixin {

    private static transient BlockStateListPopulator arclight$populator;
    private static transient boolean arclight$success = false;

    @Redirect(method = "checkSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean arclight$storeUpdate1(Level world, BlockPos pos, BlockState newState, int flags) {
        if (arclight$populator == null) {
            arclight$populator = new BlockStateListPopulator(world);
        }
        return arclight$populator.setBlock(pos, newState, flags);
    }

    @Redirect(method = "checkSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private static void arclight$storeUpdate2(Level world, int type, BlockPos pos, int data) {
        // do nothing
    }

    @Inject(method = "checkSpawn", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static void arclight$returnIfFail(Level worldIn, BlockPos blockPos, SkullBlockEntity tileEntity, CallbackInfo ci) {
        if (arclight$success) {
            if (arclight$populator != null) {
                for (BlockPos pos : arclight$populator.getBlocks()) {
                    worldIn.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(worldIn.getBlockState(pos)));
                }
                arclight$populator.updateList();
            }
        } else {
            ci.cancel();
        }
        arclight$populator = null;
        arclight$success = false;
    }

    @Redirect(method = "checkSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean arclight$spawnWither(Level world, Entity entityIn) {
        ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BUILD_WITHER);
        return arclight$success = world.addFreshEntity(entityIn);
    }
}
