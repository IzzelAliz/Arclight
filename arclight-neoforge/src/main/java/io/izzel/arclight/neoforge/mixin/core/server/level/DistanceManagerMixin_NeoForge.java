package io.izzel.arclight.neoforge.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.level.DistanceManagerBridge;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.Ticket;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin_NeoForge implements DistanceManagerBridge {

    @Override
    public boolean bridge$platform$isTicketForceTick(Ticket ticket) {
        return false;
    }

    @Override
    public void bridge$forge$addForcedTicket(long chunkPosIn, Ticket ticketIn) {
    }

    @Override
    public void bridge$forge$removeForcedTicket(long chunkPosIn, Ticket ticketIn) {
    }
}
