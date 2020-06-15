package io.izzel.arclight.common.bridge.server.management;

import net.minecraft.block.BlockState;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.server.SPlayerDiggingPacket;
import net.minecraft.util.math.BlockPos;

public interface PlayerInteractionManagerBridge {

    boolean bridge$isFiredInteract();

    void bridge$setFiredInteract(boolean b);

    boolean bridge$getInteractResult();

    void bridge$setInteractResult(boolean b);

    SPlayerDiggingPacket bridge$diggingPacket(BlockPos pos, BlockState state, CPlayerDiggingPacket.Action action, boolean successful, String context);

    void bridge$creativeHarvestBlock(BlockPos pos, CPlayerDiggingPacket.Action action, String context);
}
