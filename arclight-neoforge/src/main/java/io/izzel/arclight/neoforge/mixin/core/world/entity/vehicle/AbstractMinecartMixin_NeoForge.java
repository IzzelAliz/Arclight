package io.izzel.arclight.neoforge.mixin.core.world.entity.vehicle;

import io.izzel.arclight.common.bridge.core.entity.vehicle.AbstractMinecartBridge;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin_NeoForge implements AbstractMinecartBridge {

    // @formatter:off
    @Shadow(remap = false) public abstract boolean canUseRail();
    // @formatter:on

    @Override
    public boolean bridge$forge$canUseRail() {
        return this.canUseRail();
    }
}
