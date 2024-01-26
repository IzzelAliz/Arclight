package io.izzel.arclight.common.mixin.core.world.item.enchantment;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FrostWalkerEnchantment.class)
public class FrostWalkerEnchantmentMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static void onEntityMoved(LivingEntity living, Level worldIn, BlockPos pos, int level) {
        if (living.onGround()) {
            BlockState blockstate = Blocks.FROSTED_ICE.defaultBlockState();
            int f = Math.min(16, 2 + level);
            BlockPos.MutableBlockPos blockpos$mutable = new BlockPos.MutableBlockPos();

            for (BlockPos blockpos : BlockPos.betweenClosed(pos.offset(-f, -1, -f), pos.offset(f, -1, f))) {
                if (blockpos.closerToCenterThan(living.position(), f)) {
                    blockpos$mutable.set(blockpos.getX(), blockpos.getY() + 1, blockpos.getZ());
                    BlockState blockstate1 = worldIn.getBlockState(blockpos$mutable);
                    if (blockstate1.isAir()) {
                        BlockState blockstate2 = worldIn.getBlockState(blockpos);
                        if (blockstate2 == FrostedIceBlock.meltsInto() && blockstate.canSurvive(worldIn, blockpos) && worldIn.isUnobstructed(blockstate, blockpos, CollisionContext.empty()) && !((WorldBridge) worldIn).bridge$forge$onBlockPlace(blockpos, living, Direction.UP)) {
                            if (CraftEventFactory.handleBlockFormEvent(worldIn, blockpos, blockstate, living)) {
                                worldIn.scheduleTick(blockpos, Blocks.FROSTED_ICE, Mth.nextInt(living.getRandom(), 60, 120));
                            }
                        }
                    }
                }
            }
        }
    }
}
