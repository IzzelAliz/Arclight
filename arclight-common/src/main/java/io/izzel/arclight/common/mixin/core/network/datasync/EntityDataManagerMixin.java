package io.izzel.arclight.common.mixin.core.network.datasync;

import io.izzel.arclight.common.bridge.network.datasync.EntityDataManagerBridge;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityDataManager.class)
public abstract class EntityDataManagerMixin implements EntityDataManagerBridge {

    // @formatter:off
    @Shadow protected abstract <T> EntityDataManager.DataEntry<T> getEntry(DataParameter<T> key);
    @Shadow private boolean dirty;
    // @formatter:on

    public <T> void markDirty(DataParameter<T> key) {
        EntityDataManager.DataEntry<T> entry = this.getEntry(key);
        entry.setDirty(true);
        this.dirty = true;
    }

    @Override
    public <T> void bridge$markDirty(DataParameter<T> key) {
        this.markDirty(key);
    }
}
