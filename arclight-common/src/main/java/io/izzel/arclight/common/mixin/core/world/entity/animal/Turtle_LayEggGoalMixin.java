package io.izzel.arclight.common.mixin.core.world.entity.animal;

import com.destroystokyo.paper.event.entity.TurtleLayEggEvent;
import com.destroystokyo.paper.event.entity.TurtleStartDiggingEvent;
import io.izzel.arclight.common.bridge.core.world.entity.animal.TurtleBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.entity.animal.turtle.Turtle$TurtleLayEggGoal")
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
            if (((TurtleBridge) this.turtle).bridge$getDigging() < 1) {
                ((TurtleBridge) this.turtle).bridge$setDigging(new TurtleStartDiggingEvent(
                    (org.bukkit.entity.Turtle) ((TurtleBridge) this.turtle).bridge$getBukkitEntity(),
                    CraftLocation.toBukkit(this.blockPos, this.turtle.level())).callEvent());
            } else if (((TurtleBridge) this.turtle).bridge$getDigging() > this.adjustedTickDelay(200)) {
                TurtleLayEggEvent event = new TurtleLayEggEvent(
                    (org.bukkit.entity.Turtle) ((TurtleBridge) this.turtle).bridge$getBukkitEntity(),
                    CraftLocation.toBukkit(this.blockPos.above(), this.turtle.level()), this.turtle.getRandom().nextInt(4) + 1);
                if (event.callEvent() && CraftEventFactory.callEntityChangeBlockEvent(this.turtle, this.blockPos.above(),
                    Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS, event.getEggCount()))) {
                    Level world = this.turtle.level();
                    world.playSound(null, blockpos, net.minecraft.sounds.SoundEvents.TURTLE_LAY_EGG,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.3F, 0.9F + world.getRandom().nextFloat() * 0.2F);
                    BlockPos blockpos1 = this.blockPos.above();
                    BlockState blockstate = Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS, event.getEggCount());
                    world.setBlock(blockpos1, blockstate, 3);
                    world.gameEvent(GameEvent.BLOCK_PLACE, blockpos1, GameEvent.Context.of(this.turtle, blockstate));
                }
                ((TurtleBridge) this.turtle).bridge$setHasEgg(false);
                ((TurtleBridge) this.turtle).bridge$setDigging(false);
                this.turtle.setInLoveTime(600);
            }

            if (this.turtle.isLayingEgg()) {
                ((TurtleBridge) this.turtle).bridge$setDigging(((TurtleBridge) this.turtle).bridge$getDigging() + 1);
            }
        }

    }
}
