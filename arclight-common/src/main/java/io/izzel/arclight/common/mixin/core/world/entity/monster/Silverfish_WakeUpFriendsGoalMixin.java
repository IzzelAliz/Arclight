package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.api.ArclightVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(targets = "net.minecraft.world.entity.monster.Silverfish$SilverfishWakeUpFriendsGoal")
public abstract class Silverfish_WakeUpFriendsGoalMixin extends Goal {

    @Shadow private int lookForFriends;
    @Shadow @Final private Silverfish silverfish;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        --this.lookForFriends;
        if (this.lookForFriends <= 0) {
            Level world = this.silverfish.level;
            Random random = this.silverfish.getRandom();
            BlockPos blockpos = this.silverfish.blockPosition();

            for (int i = 0; i <= 5 && i >= -5; i = (i <= 0 ? 1 : 0) - i) {
                for (int j = 0; j <= 10 && j >= -10; j = (j <= 0 ? 1 : 0) - j) {
                    for (int k = 0; k <= 10 && k >= -10; k = (k <= 0 ? 1 : 0) - k) {
                        BlockPos blockpos1 = blockpos.offset(j, i, k);
                        BlockState blockstate = world.getBlockState(blockpos1);
                        Block block = blockstate.getBlock();
                        if (block instanceof InfestedBlock) {
                            if (CraftEventFactory.callEntityChangeBlockEvent(this.silverfish, blockpos1, Blocks.AIR.defaultBlockState()).isCancelled()) {
                                continue;
                            }
                            if (ForgeEventFactory.getMobGriefingEvent(world, this.silverfish)) {
                                if (ArclightVersion.atLeast(ArclightVersion.v1_15)) {
                                    world.destroyBlock(blockpos1, true, this.silverfish);
                                } else {
                                    world.destroyBlock(blockpos1, true);
                                }
                            } else {
                                world.setBlock(blockpos1, ((InfestedBlock) block).getHostBlock().defaultBlockState(), 3);
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
