package io.izzel.arclight.common.mixin.core.world.entity.ai.goal;

import com.destroystokyo.paper.entity.ai.PaperGoal;
import io.izzel.arclight.common.bridge.core.world.entity.ai.GoalBridge;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.EnumSet;

@Mixin(Goal.class)
public abstract class GoalMixin implements GoalBridge {
    @Shadow public abstract EnumSet<Goal.Flag> getFlags();

    @Unique private PaperGoal<?> arclight$paperGoal;

    @Override
    public boolean hasFlag(Goal.Flag flag) {
        return this.getFlags().contains(flag);
    }

    @Override
    public void addFlag(Goal.Flag flag) {
        this.getFlags().add(flag);
    }

    @Override
    public <T extends org.bukkit.entity.Mob> com.destroystokyo.paper.entity.ai.Goal<T> asPaperGoal() {
        return this.bridge$asPaperGoal();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends org.bukkit.entity.Mob> com.destroystokyo.paper.entity.ai.Goal<T> bridge$asPaperGoal() {
        if (this.arclight$paperGoal == null) {
            this.arclight$paperGoal = new PaperGoal<>((Goal) (Object) this);
        }
        return (com.destroystokyo.paper.entity.ai.Goal<T>) this.arclight$paperGoal;
    }
}
