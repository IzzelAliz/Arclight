package io.izzel.arclight.common.bridge.core.world.level.levelgen;

import org.bukkit.event.world.AsyncStructureGenerateEvent;

public interface StructureStartBridge {

    void bridge$setGenerateCause(AsyncStructureGenerateEvent.Cause cause);
}
