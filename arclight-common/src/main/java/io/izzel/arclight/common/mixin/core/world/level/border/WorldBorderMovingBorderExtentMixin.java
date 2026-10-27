package io.izzel.arclight.common.mixin.core.world.level.border;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.level.border.WorldBorderBridge;
import io.izzel.arclight.mixin.Decorate;
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeFinishEvent;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.level.border.WorldBorder$MovingBorderExtent")
public abstract class WorldBorderMovingBorderExtentMixin {

    @Shadow(aliases = "this$0", remap = false) @Final private WorldBorder outerThis;
    @Shadow @Final private double from;
    @Shadow @Final private double to;
    @Shadow @Final private double lerpDuration;

    @Decorate(method = "update()Lnet/minecraft/world/level/border/WorldBorder$BorderExtent;", inject = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;setDirty()V"), require = 1, allow = 1)
    private void arclight$boundsChangeFinished() {
        var world = ((WorldBorderBridge) this.outerThis).bridge$getWorld();
        if (world != null) {
            var bukkitWorld = ((WorldBridge) world).bridge$getWorld();
            // Paper passes the native tick duration through unchanged to this notification event.
            new WorldBorderBoundsChangeFinishEvent(bukkitWorld, bukkitWorld.getWorldBorder(), this.from, this.to, this.lerpDuration).callEvent();
        }
    }
}
