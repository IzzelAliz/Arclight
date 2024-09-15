package io.izzel.arclight.common.mixin.bukkit.util;

import io.izzel.arclight.common.bridge.core.world.IWorldWriterBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.WorldGenLevel;
import org.bukkit.craftbukkit.v.util.DelegatedGeneratorAccess;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DelegatedGeneratorAccess.class)
public abstract class DelegatedGeneratorAccessMixin implements IWorldWriterBridge {
    @Shadow public abstract WorldGenLevel getHandle();

    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (getHandle() != (Object) this) {
            return ((IWorldWriterBridge) getHandle()).bridge$addEntity(entity, reason);
        } else {
            this.bridge$pushAddEntityReason(reason);
            return getHandle().addFreshEntity(entity);
        }
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
        if (getHandle() != (Object) this) {
            ((IWorldWriterBridge) getHandle()).bridge$pushAddEntityReason(reason);
        }
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        if (getHandle() != (Object) this) {
            return ((IWorldWriterBridge) getHandle()).bridge$getAddEntityReason();
        }
        return null;
    }
}
