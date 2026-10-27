package io.izzel.arclight.common.mixin.core.world.entity.animal;

import com.destroystokyo.paper.event.entity.TurtleGoHomeEvent;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.animal.turtle.Turtle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.animal.turtle.Turtle$TurtleGoHomeGoal")
public abstract class TurtleGoHomeGoalMixin {

    @Shadow @Final private Turtle turtle;

    // Vanilla has four return sites; NeoForge merges the random/distance result into one (three sites).
    @Decorate(method = "canUse()Z", at = @At("RETURN"), require = 3, allow = 4)
    private void arclight$goHomeEvent(boolean canUse) throws Throwable {
        if (canUse) {
            canUse = new TurtleGoHomeEvent(
                (org.bukkit.entity.Turtle) ((EntityBridge) this.turtle).bridge$getBukkitEntity()).callEvent();
        }
        DecorationOps.callsite().invoke(canUse);
    }
}
