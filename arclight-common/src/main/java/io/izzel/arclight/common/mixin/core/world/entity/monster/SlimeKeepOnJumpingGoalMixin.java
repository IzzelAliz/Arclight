package io.izzel.arclight.common.mixin.core.world.entity.monster;

import com.destroystokyo.paper.event.entity.SlimeWanderEvent;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.monster.cubemob.AbstractCubeMob$CubeMobKeepOnJumpingGoal")
public abstract class SlimeKeepOnJumpingGoalMixin {

    @Shadow @Final private AbstractCubeMob cubeMob;

    @Decorate(method = "canUse()Z", at = @At("RETURN"), require = 1, expect = 1, allow = 1)
    private void arclight$wanderEvent(boolean canUse) throws Throwable {
        // Retain the passenger check; inherited continuation reuses canUse().
        if (canUse) {
            SlimeWanderEvent event = new SlimeWanderEvent(
                (org.bukkit.entity.AbstractCubeMob) ((EntityBridge) this.cubeMob).bridge$getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);
            canUse = !event.isCancelled();
        }
        DecorationOps.callsite().invoke(canUse);
    }
}
