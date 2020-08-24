package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.api.ArclightVersion;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SilverfishBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.monster.SilverfishEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(targets = "net.minecraft.entity.monster.SilverfishEntity.SummonSilverfishGoal")
public abstract class SilverfishEntity_SummonSilverfishGoalMixin extends Goal {

    @Shadow private int lookForFriends;
    @Shadow @Final private SilverfishEntity silverfish;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        --this.lookForFriends;
        if (this.lookForFriends <= 0) {
            World world = this.silverfish.world;
            Random random = this.silverfish.getRNG();
            BlockPos blockpos = this.silverfish.getPosition();

            for (int i = 0; i <= 5 && i >= -5; i = (i <= 0 ? 1 : 0) - i) {
                for (int j = 0; j <= 10 && j >= -10; j = (j <= 0 ? 1 : 0) - j) {
                    for (int k = 0; k <= 10 && k >= -10; k = (k <= 0 ? 1 : 0) - k) {
                        BlockPos blockpos1 = blockpos.add(j, i, k);
                        BlockState blockstate = world.getBlockState(blockpos1);
                        Block block = blockstate.getBlock();
                        if (block instanceof SilverfishBlock) {
                            if (CraftEventFactory.callEntityChangeBlockEvent(this.silverfish, blockpos1, Blocks.AIR.getDefaultState()).isCancelled()) {
                                continue;
                            }
                            if (ForgeEventFactory.getMobGriefingEvent(world, this.silverfish)) {
                                if (ArclightVersion.atLeast(ArclightVersion.v1_15)) {
                                    world.destroyBlock(blockpos1, true, this.silverfish);
                                } else {
                                    world.destroyBlock(blockpos1, true);
                                }
                            } else {
                                world.setBlockState(blockpos1, ((SilverfishBlock) block).getMimickedBlock().getDefaultState(), 3);
                            }

                            if (random.nextBoolean()) {
                                return;
                            }
                        }
                    }
                }
            }
        }

    }
}
