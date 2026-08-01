package io.izzel.arclight.common.bridge.core.world.level.entity;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;

public interface PersistentEntitySectionManagerBridge {
    EntitySectionStorage<? extends EntityAccess> getSectionStorage();
}
