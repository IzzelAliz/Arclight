package io.izzel.arclight.impl.mixin.optimization.general.realtime;

import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.server.management.PlayerInteractionManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerInteractionManager.class)
public class PlayerInteractionManagerMixin_Realtime {

    @Shadow private int ticks;

    private int lastTick = ArclightConstants.currentTick - 1;

    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/server/management/PlayerInteractionManager;ticks:I"))
    private void arclight$useWallTime(PlayerInteractionManager playerInteractionManager, int value) {
        int elapsedTicks = ArclightConstants.currentTick - this.lastTick;
        if (elapsedTicks < 1) {
            elapsedTicks = 1;
        }
        this.ticks += elapsedTicks;
        this.lastTick = ArclightConstants.currentTick;
    }
}
