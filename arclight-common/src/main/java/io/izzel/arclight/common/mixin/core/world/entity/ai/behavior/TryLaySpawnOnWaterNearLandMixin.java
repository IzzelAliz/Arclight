package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.TryLaySpawnOnWaterNearLand;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TryLaySpawnOnWaterNearLand.class)
public class TryLaySpawnOnWaterNearLandMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static BehaviorControl<LivingEntity> create(Block block) {
        return BehaviorBuilder.create((instance) -> {
            return instance.group(instance.absent(MemoryModuleType.ATTACK_TARGET), instance.present(MemoryModuleType.WALK_TARGET), instance.present(MemoryModuleType.IS_PREGNANT)).apply(instance, (memoryAccessor, memoryAccessor2, memoryAccessor3) -> {
                return (serverLevel, livingEntity, l) -> {
                    if (!livingEntity.isInWater() && livingEntity.onGround()) {
                        BlockPos blockPos = livingEntity.blockPosition().below();

                        for (Direction direction : Direction.Plane.HORIZONTAL) {
                            BlockPos blockPos2 = blockPos.relative(direction);
                            if (serverLevel.getBlockState(blockPos2).getCollisionShape(serverLevel, blockPos2).getFaceShape(Direction.UP).isEmpty() && serverLevel.getFluidState(blockPos2).is(Fluids.WATER)) {
                                BlockPos blockPos3 = blockPos2.above();
                                if (serverLevel.getBlockState(blockPos3).isAir()) {
                                    BlockState blockState = block.defaultBlockState();
                                    if (!CraftEventFactory.callEntityChangeBlockEvent(livingEntity, blockPos3, blockState)) {
                                        memoryAccessor3.erase();
                                        return true;
                                    }
                                    serverLevel.setBlock(blockPos3, blockState, 3);
                                    serverLevel.gameEvent(GameEvent.BLOCK_PLACE, blockPos3, GameEvent.Context.of(livingEntity, blockState));
                                    serverLevel.playSound(null, livingEntity, SoundEvents.FROG_LAY_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                                    memoryAccessor3.erase();
                                    return true;
                                }
                            }
                        }

                        return true;
                    } else {
                        return false;
                    }
                };
            });
        });
    }
}
