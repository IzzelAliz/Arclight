package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import org.bukkit.craftbukkit.block.CraftBlock;
import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin {

    @org.spongepowered.asm.mixin.injection.Inject(method = "setPlacedBy", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"))
    private void arclight$deferPlacementPhysics(Level level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (((io.izzel.arclight.common.bridge.core.world.level.WorldBridge) level).bridge$isCapturingBlockBreak()) {
            ci.cancel();
        }
    }


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
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS_SERVER;
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
                    player.sendOverlayMessage(Component.translatable("block.minecraft.bed.occupied"));
                }

                return InteractionResult.SUCCESS_SERVER;
            } else {
                var maybeLeft = player.startSleepInBed(pos).left();
                if (maybeLeft.isPresent()) {
                    final var problem = maybeLeft.get();
                    if (level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos).explodes()) {
                        org.bukkit.block.BlockState bedSnapshot = CraftBlock.at(level, pos).getState();
                        level.removeBlock(pos, false);
                        BlockPos blockpos = pos.relative(state.getValue(FACING).getOpposite());
                        if (level.getBlockState(blockpos).is((BedBlock) (Object) this)) {
                            level.removeBlock(blockpos, false);
                        }

                        Vec3 vec3d = pos.getCenter();
                        level.explode(null, ((DamageSourceBridge) level.damageSources().badRespawnPointExplosion(vec3d)).bridge$directBlockState(bedSnapshot), null, vec3d, 5.0F, true, Level.ExplosionInteraction.BLOCK);
                    } else if (problem.message() != null) {
                        player.sendOverlayMessage(problem.message());
                    }
                }
                return InteractionResult.SUCCESS_SERVER;
            }
        }
    }
}
