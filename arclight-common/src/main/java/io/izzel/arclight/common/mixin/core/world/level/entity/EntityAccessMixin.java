package io.izzel.arclight.common.mixin.core.world.level.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityAccess.class)
public interface EntityAccessMixin {

    // @formatter:off
    @Shadow void setRemoved(Entity.RemovalReason arg);
    // @formatter:on

    default void setRemoved(Entity.RemovalReason reason, EntityRemoveEvent.Cause cause) {
        if (this instanceof EntityBridge bridge) {
            bridge.bridge$pushEntityRemoveCause(cause);
        }
        setRemoved(reason);
    }
}
