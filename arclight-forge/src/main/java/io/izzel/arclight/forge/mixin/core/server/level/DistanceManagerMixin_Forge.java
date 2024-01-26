package io.izzel.arclight.forge.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.server.TicketManagerBridge;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin_Forge implements TicketManagerBridge {

    // @formatter:off
    @Shadow(remap = false) @Final private Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> forcedTickets;
    // @formatter:on

    @Override
    public boolean bridge$platform$isTicketForceTick(Ticket<?> ticket) {
        return ticket.isForceTicks();
    }

    @Override
    public void bridge$forge$addForcedTicket(long chunkPosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> tickets = this.forcedTickets.computeIfAbsent(chunkPosIn, e -> SortedArraySet.create(4));
        tickets.addOrGet(ticketIn);
    }

    @Override
    public void bridge$forge$removeForcedTicket(long chunkPosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> tickets = this.forcedTickets.get(chunkPosIn);
        if (tickets != null) {
            tickets.remove(ticketIn);
        }
    }
}
