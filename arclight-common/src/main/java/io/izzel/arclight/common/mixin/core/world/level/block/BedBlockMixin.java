package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
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
     * @reason
     */
    @Overwrite
    public InteractionResult use(BlockState p_49515_, Level level, BlockPos p_49517_, Player p_49518_, InteractionHand p_49519_, BlockHitResult p_49520_) {
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        } else {
            if (p_49515_.getValue(PART) != BedPart.HEAD) {
                p_49517_ = p_49517_.relative(p_49515_.getValue(FACING));
                p_49515_ = level.getBlockState(p_49517_);
                if (!p_49515_.is((BedBlock) (Object) this)) {
                    return InteractionResult.CONSUME;
                }
            }

            /* if (!canSetSpawn(level)) {
                level.removeBlock(p_49517_, false);
                BlockPos blockpos = p_49517_.relative(p_49515_.getValue(FACING).getOpposite());
                if (level.getBlockState(blockpos).is((BedBlock) (Object) this)) {
                    level.removeBlock(blockpos, false);
                }

                level.explode((Entity) null, DamageSource.badRespawnPointExplosion(), (ExplosionDamageCalculator) null, (double) p_49517_.getX() + 0.5D, (double) p_49517_.getY() + 0.5D, (double) p_49517_.getZ() + 0.5D, 5.0F, true, Explosion.BlockInteraction.DESTROY);
                return InteractionResult.SUCCESS;
            } else */
            if (p_49515_.getValue(OCCUPIED)) {
                if (!this.kickVillagerOutOfBed(level, p_49517_)) {
                    p_49518_.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
                }

                return InteractionResult.SUCCESS;
            } else {
                var pos = p_49517_;
                var state = p_49515_;
                p_49518_.startSleepInBed(pos).ifLeft((p_49477_) -> {
                    if (!level.dimensionType().bedWorks()) {
                        level.removeBlock(pos, false);
                        BlockPos blockpos = pos.relative(state.getValue(FACING).getOpposite());
                        if (level.getBlockState(blockpos).is((BedBlock) (Object) this)) {
                            level.removeBlock(blockpos, false);
                        }

                        Vec3 vec3d = pos.getCenter();
                        level.explode(null, level.damageSources().badRespawnPointExplosion(vec3d), null, vec3d, 5.0F, true, Level.ExplosionInteraction.BLOCK);
                    } else if (p_49477_.getMessage() != null) {
                        p_49518_.displayClientMessage(p_49477_.getMessage(), true);
                    }
                });
                return InteractionResult.SUCCESS;
            }
        }
    }
}
