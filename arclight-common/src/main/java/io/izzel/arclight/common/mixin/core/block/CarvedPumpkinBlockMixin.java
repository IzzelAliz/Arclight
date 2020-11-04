package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.entity.Entity;
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

@Mixin(CarvedPumpkinBlock.class)
public class CarvedPumpkinBlockMixin {

    private transient BlockStateListPopulator arclight$populator;
    private transient boolean arclight$success = false;

    @Redirect(method = "trySpawnGolem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public boolean arclight$storeUpdate1(World world, BlockPos pos, BlockState newState, int flags) {
        if (arclight$populator == null) {
            arclight$populator = new BlockStateListPopulator(world);
        }
        return arclight$populator.setBlockState(pos, newState, flags);
    }

    @Redirect(method = "trySpawnGolem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playEvent(ILnet/minecraft/util/math/BlockPos;I)V"))
    public void arclight$storeUpdate2(World world, int type, BlockPos pos, int data) {
        // do nothing
    }

    @Inject(method = "trySpawnGolem", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$returnIfFail(World world, BlockPos blockPos, CallbackInfo ci) {
        if (arclight$success) {
            if (arclight$populator != null) {
                for (BlockPos pos : arclight$populator.getBlocks()) {
                    world.playEvent(Constants.WorldEvents.BREAK_BLOCK_EFFECTS, pos, Block.getStateId(world.getBlockState(pos)));
                }
                arclight$populator.updateList();
            }
        } else {
            ci.cancel();
        }
        arclight$populator = null;
        arclight$success = false;
    }

    @Redirect(method = "trySpawnGolem", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public boolean arclight$spawnSnow(World world, Entity entityIn) {
        ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN);
        return arclight$success = world.addEntity(entityIn);
    }

    @Redirect(method = "trySpawnGolem", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public boolean arclight$spawnIron(World world, Entity entityIn) {
        ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM);
        return arclight$success = world.addEntity(entityIn);
    }
}
