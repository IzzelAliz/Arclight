package io.izzel.arclight.neoforge.mixin.core.world.entity.vehicle;

import io.izzel.arclight.common.bridge.core.world.entity.vehicle.AbstractMinecartBridge;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin_NeoForge implements AbstractMinecartBridge {

    @Override
    public boolean bridge$forge$canUseRail() {
        // NeoForge 21.11 removed the mutable canUseRail extension. Preserve the
        // current native behavior rather than reintroducing legacy speed limits
        // or rail callbacks around the new MinecartBehavior implementation.
        return true;
    }
}
