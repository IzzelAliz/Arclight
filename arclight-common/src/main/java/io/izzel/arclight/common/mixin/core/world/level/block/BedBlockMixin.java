package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin {

    // @formatter:off
    @Shadow @Final public static EnumProperty<BedPart> PART;
    @Shadow @Final public static BooleanProperty OCCUPIED;
    @Shadow protected abstract boolean kickVillagerOutOfBed(Level p_49491_, BlockPos p_49492_);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason Shit logic
     */
    @Overwrite
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        } else {
            if (state.getValue(PART) != BedPart.HEAD) {
                pos = pos.relative(state.getValue(FACING));
                state = level.getBlockState(pos);
                if (!state.is((BedBlock) (Object) this)) {
                    return InteractionResult.CONSUME;
                }
            }

            /* if (!canSetSpawn(level)) {
                level.removeBlock(pos, false);
                BlockPos blockpos = pos.relative(state.getValue(FACING).getOpposite());
                if (level.getBlockState(blockpos).is((BedBlock) (Object) this)) {
                    level.removeBlock(blockpos, false);
                }

                level.explode((Entity) null, DamageSource.badRespawnPointExplosion(), (ExplosionDamageCalculator) null, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, 5.0F, true, Explosion.BlockInteraction.DESTROY);
                return InteractionResult.SUCCESS;
            } else */
            if (state.getValue(OCCUPIED)) {
                if (!this.kickVillagerOutOfBed(level, pos)) {
                    player.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
                }

                return InteractionResult.SUCCESS;
            } else {
                var maybeLeft = player.startSleepInBed(pos).left();
                if (maybeLeft.isPresent()) {
                    final var problem = maybeLeft.get();
                    if (!level.dimensionType().bedWorks()) {
                        level.removeBlock(pos, false);
                        BlockPos blockpos = pos.relative(state.getValue(FACING).getOpposite());
                        if (level.getBlockState(blockpos).is((BedBlock) (Object) this)) {
                            level.removeBlock(blockpos, false);
                        }

                        Vec3 vec3d = pos.getCenter();
                        level.explode(null, level.damageSources().badRespawnPointExplosion(vec3d), null, vec3d, 5.0F, true, Level.ExplosionInteraction.BLOCK);
                    } else if (problem.getMessage() != null) {
                        player.displayClientMessage(problem.getMessage(), true);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
    }
}
