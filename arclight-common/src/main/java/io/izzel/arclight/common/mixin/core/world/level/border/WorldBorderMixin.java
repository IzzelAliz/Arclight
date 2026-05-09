package io.izzel.arclight.common.mixin.core.world.level.border;

import io.izzel.arclight.common.bridge.core.world.level.border.WorldBorderBridge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WorldBorder.class)
public class WorldBorderMixin implements WorldBorderBridge {

    @Shadow @Final private List<BorderChangeListener> listeners;
    public Level world;

    @Override
    public Level bridge$getWorld() {
        return this.world;
    }

    @Override
    public void bridge$setWorld(Level world) {
        this.world = world;
    }

    @Inject(method = "addListener", cancellable = true, at = @At("HEAD"))
    private void arclight$removeDuplicateListener(BorderChangeListener listener, CallbackInfo ci) {
        if (listeners.contains(listener)) {
            ci.cancel();
        }
    }
}
