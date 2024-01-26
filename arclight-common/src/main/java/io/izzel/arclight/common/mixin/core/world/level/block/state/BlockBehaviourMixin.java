package io.izzel.arclight.common.mixin.core.world.level.block.state;

import io.izzel.arclight.common.bridge.core.world.ExplosionBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.state.BlockBehaviourBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.BiConsumer;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin implements BlockBehaviourBridge {

    // @formatter:off
    @Shadow public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) { return null; }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Deprecated
    public void onExplosionHit(BlockState p_310712_, Level p_311693_, BlockPos p_311490_, Explosion p_312709_, BiConsumer<ItemStack, BlockPos> p_311277_) {
        if (!p_310712_.isAir() && p_312709_.getBlockInteraction() != Explosion.BlockInteraction.TRIGGER_BLOCK) {
            Block block = p_310712_.getBlock();
            boolean flag = p_312709_.getIndirectSourceEntity() instanceof Player;
            if (this.bridge$forge$canDropFromExplosion(p_310712_, p_311693_, p_311490_, p_312709_) && p_311693_ instanceof ServerLevel) {
                ServerLevel serverlevel = (ServerLevel) p_311693_;
                BlockEntity blockentity = p_310712_.hasBlockEntity() ? p_311693_.getBlockEntity(p_311490_) : null;
                LootParams.Builder lootparams$builder = (new LootParams.Builder(serverlevel)).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(p_311490_)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockentity).withOptionalParameter(LootContextParams.THIS_ENTITY, p_312709_.getDirectSourceEntity());
                if (((ExplosionBridge) p_312709_).bridge$getYield() < 1.0F) {
                    lootparams$builder.withParameter(LootContextParams.EXPLOSION_RADIUS, 1.0F / ((ExplosionBridge) p_312709_).bridge$getYield());
                }

                p_310712_.spawnAfterBreak(serverlevel, p_311490_, ItemStack.EMPTY, flag);
                p_310712_.getDrops(lootparams$builder).forEach((p_309419_) -> {
                    p_311277_.accept(p_309419_, p_311490_);
                });
            }
            this.bridge$forge$onBlockExploded(p_310712_, p_311693_, p_311490_, p_312709_);
        }
    }
}
