package io.izzel.arclight.common.mixin.bukkit.util;

import io.izzel.arclight.common.bridge.core.world.level.IWorldWriterBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.WorldGenLevel;
import org.bukkit.craftbukkit.util.DelegatedLevelAccessor;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DelegatedLevelAccessor.class)
public abstract class DelegatedGeneratorAccessMixin implements IWorldWriterBridge {
    @Shadow public abstract WorldGenLevel getDelegate();

    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (getDelegate() != (Object) this) {
            return ((IWorldWriterBridge) getDelegate()).bridge$addEntity(entity, reason);
        } else {
            this.bridge$pushAddEntityReason(reason);
            return getDelegate().addFreshEntity(entity);
        }
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
        if (getDelegate() != (Object) this) {
            ((IWorldWriterBridge) getDelegate()).bridge$pushAddEntityReason(reason);
        }
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        if (getDelegate() != (Object) this) {
            return ((IWorldWriterBridge) getDelegate()).bridge$getAddEntityReason();
        }
        return null;
    }
}
