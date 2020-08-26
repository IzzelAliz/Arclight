package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.bridge.entity.passive.TurtleEntityBridge;
import net.minecraft.block.Blocks;
import net.minecraft.block.TurtleEggBlock;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.entity.passive.TurtleEntity.LayEggGoal")
public abstract class TurtleEntity_LayEggGoalMixin extends MoveToBlockGoal {

    @Shadow @Final private TurtleEntity turtle;

    public TurtleEntity_LayEggGoalMixin(CreatureEntity creature, double speedIn, int length) {
        super(creature, speedIn, length);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        super.tick();
        BlockPos blockpos = this.turtle.getPosition();
        if (!this.turtle.isInWater() && this.getIsAboveDestination()) {
            if (((TurtleEntityBridge) this.turtle).bridge$getDigging() < 1) {
                ((TurtleEntityBridge) this.turtle).bridge$setDigging(true);
            } else if (((TurtleEntityBridge) this.turtle).bridge$getDigging() > 200) {
                World world = this.turtle.world;
                if (!CraftEventFactory.callEntityChangeBlockEvent(this.turtle, this.destinationBlock.up(), (Blocks.TURTLE_EGG.getDefaultState()).with(TurtleEggBlock.EGGS, this.turtle.getRNG().nextInt(4) + 1)).isCancelled()) {
                    world.playSound(null, blockpos, SoundEvents.ENTITY_TURTLE_LAY_EGG, SoundCategory.BLOCKS, 0.3f, 0.9f + world.rand.nextFloat() * 0.2f);
                    world.setBlockState(this.destinationBlock.up(), (Blocks.TURTLE_EGG.getDefaultState()).with(TurtleEggBlock.EGGS, this.turtle.getRNG().nextInt(4) + 1), 3);
                }
                ((TurtleEntityBridge) this.turtle).bridge$setHasEgg(false);
                ((TurtleEntityBridge) this.turtle).bridge$setDigging(false);
                this.turtle.setInLove(600);
            }

            if (this.turtle.isDigging()) {
                ((TurtleEntityBridge) this.turtle).bridge$setDigging(((TurtleEntityBridge) this.turtle).bridge$getDigging() + 1);
            }
        }

    }
}
