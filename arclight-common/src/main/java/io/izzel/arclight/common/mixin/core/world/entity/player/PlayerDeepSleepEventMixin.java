package io.izzel.arclight.common.mixin.core.world.entity.player;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerDeepSleepEventMixin {

    @Shadow public int sleepCounter;

    @Decorate(method = "tick()V", inject = true, require = 1, allow = 1,
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;sleepCounter:I",
            opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER))
    private void arclight$playerDeepSleep() {
        // Only the sleeping branch's increment: dispatch before the native cap
        // and before either loader evaluates whether the player must wake up.
        if (this.sleepCounter == Player.SLEEP_DURATION
            && !new PlayerDeepSleepEvent((org.bukkit.entity.Player) ((PlayerBridge) this).bridge$getBukkitEntity()).callEvent()) {
            this.sleepCounter = Integer.MIN_VALUE;
        }
    }
}
