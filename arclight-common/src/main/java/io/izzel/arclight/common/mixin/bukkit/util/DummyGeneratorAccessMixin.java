package io.izzel.arclight.common.mixin.bukkit.util;

import io.izzel.arclight.common.bridge.core.world.IWorldWriterBridge;
import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.v.util.DummyGeneratorAccess;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DummyGeneratorAccess.class)
public class DummyGeneratorAccessMixin implements IWorldWriterBridge {
    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
