package io.izzel.arclight.common.mixin.core.world.entity.ai.goal;

import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RunAroundLikeCrazyGoal.class)
public class RunAroundLikeCrazyGoalMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/horse/AbstractHorse;tameWithName(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean arclight$tame(AbstractHorse instance, Player player) {
        if (!CraftEventFactory.callEntityTameEvent(instance, player).isCancelled()) {
            return instance.tameWithName(player);
        } else {
            return false;
        }
    }
}
