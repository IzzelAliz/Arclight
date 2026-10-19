package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.world.entity.ai.goal.TemptGoalBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.jspecify.annotations.Nullable;

@Mixin(targets = "net.minecraft.world.entity.animal.feline.Cat$CatTemptGoal")
public abstract class Cat_CatTemptGoalMixin {

    @Shadow @Final private Cat cat;
    @Shadow @Nullable private Player selectedPlayer;

    @Unique private @Nullable LivingEntity arclight$selectedLiving;

    @Overwrite
    public void tick() {
        TemptGoalBridge goal = (TemptGoalBridge) (Object) this;
        goal.arclight$runTemptTick();
        LivingEntity target = goal.arclight$getEffectiveTarget();

        // Preserve Cat's exact 600/500 adjusted-delay draw order and else-if behavior.
        if (this.arclight$selectedLiving == null && this.cat.getRandom().nextInt(goal.arclight$adjustedTickDelay(600)) == 0) {
            this.arclight$selectedLiving = target;
            this.selectedPlayer = target instanceof Player ? (Player) target : null;
        } else if (this.cat.getRandom().nextInt(goal.arclight$adjustedTickDelay(500)) == 0) {
            this.arclight$selectedLiving = null;
            this.selectedPlayer = null;
        }
    }

    @Overwrite
    protected boolean canScare() {
        TemptGoalBridge goal = (TemptGoalBridge) (Object) this;
        return this.arclight$selectedLiving != null && this.arclight$selectedLiving.equals(goal.arclight$getEffectiveTarget())
            ? false
            : goal.arclight$baseCanScare();
    }
}
