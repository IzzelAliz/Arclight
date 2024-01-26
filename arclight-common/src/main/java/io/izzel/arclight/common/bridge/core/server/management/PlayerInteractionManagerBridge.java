package io.izzel.arclight.common.bridge.core.server.management;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;

public interface PlayerInteractionManagerBridge {

    boolean bridge$isFiredInteract();

    void bridge$setFiredInteract(boolean b);

    boolean bridge$getInteractResult();

    void bridge$setInteractResult(boolean b);

    void bridge$handleBlockDrop(ArclightCaptures.BlockBreakEventContext breakEventContext, BlockPos pos);
}
