package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.entity.passive.TurtleEntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.entity.animal.Turtle$TurtleLayEggGoal")
public abstract class Turtle_LayEggGoalMixin extends MoveToBlockGoal {

    @Shadow @Final private Turtle turtle;

    public Turtle_LayEggGoalMixin(PathfinderMob creature, double speedIn, int length) {
        super(creature, speedIn, length);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        super.tick();
        BlockPos blockpos = this.turtle.blockPosition();
        if (!this.turtle.isInWater() && this.isReachedTarget()) {
            if (((TurtleEntityBridge) this.turtle).bridge$getDigging() < 1) {
                ((TurtleEntityBridge) this.turtle).bridge$setDigging(true);
            } else if (((TurtleEntityBridge) this.turtle).bridge$getDigging() > 200) {
                Level world = this.turtle.level;
                if (!CraftEventFactory.callEntityChangeBlockEvent(this.turtle, this.blockPos.above(), (Blocks.TURTLE_EGG.defaultBlockState()).setValue(TurtleEggBlock.EGGS, this.turtle.getRandom().nextInt(4) + 1)).isCancelled()) {
                    world.playSound(null, blockpos, SoundEvents.TURTLE_LAY_EGG, SoundSource.BLOCKS, 0.3f, 0.9f + world.random.nextFloat() * 0.2f);
                    world.setBlock(this.blockPos.above(), (Blocks.TURTLE_EGG.defaultBlockState()).setValue(TurtleEggBlock.EGGS, this.turtle.getRandom().nextInt(4) + 1), 3);
                }
                ((TurtleEntityBridge) this.turtle).bridge$setHasEgg(false);
                ((TurtleEntityBridge) this.turtle).bridge$setDigging(false);
                this.turtle.setInLoveTime(600);
            }

            if (this.turtle.isLayingEgg()) {
                ((TurtleEntityBridge) this.turtle).bridge$setDigging(((TurtleEntityBridge) this.turtle).bridge$getDigging() + 1);
            }
        }

    }
}
