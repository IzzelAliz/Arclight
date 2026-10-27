package io.izzel.arclight.common.mixin.core.world.entity.monster;

import com.destroystokyo.paper.event.entity.EndermanEscapeEvent;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanLookForPlayerGoal")
public abstract class EnderMan_EndermanLookForPlayerGoalMixin {

    @Decorate(method = "tick()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/EnderMan;teleport()Z"), require = 1, allow = 1)
    private boolean arclight$stareEscape(EnderMan enderman) throws Throwable {
        // Only the close-range stared-at branch escapes. Chasing teleportTowards stays native.
        return new EndermanEscapeEvent((org.bukkit.entity.Enderman) ((EntityBridge) enderman).bridge$getBukkitEntity(),
            EndermanEscapeEvent.Reason.STARE).callEvent() && (boolean) DecorationOps.callsite().invoke(enderman);
    }
}
