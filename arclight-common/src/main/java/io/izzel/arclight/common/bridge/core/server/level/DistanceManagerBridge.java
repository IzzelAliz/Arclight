package io.izzel.arclight.common.bridge.core.server.level;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

public interface DistanceManagerBridge {

    boolean bridge$addTicketAtLevel(TicketType type, ChunkPos pos, int level, Object value);

    boolean bridge$removeTicketAtLevel(TicketType type, ChunkPos pos, int level, Object value);

    boolean bridge$addTicket(long chunkPos, Ticket ticket);

    boolean bridge$removeTicket(long chunkPos, Ticket ticket);

    void bridge$tick(ChunkMap chunkMap);

    void bridge$removeAllTicketsFor(TicketType ticketType, int ticketLevel, Object ticketIdentifier);

    void arclight$offerUpdate(ChunkHolder holder);

    default boolean bridge$platform$isTicketForceTick(Ticket ticket) {
        return false;
    }

    default void bridge$forge$addForcedTicket(long chunkPosIn, Ticket ticketIn) {}

    default void bridge$forge$removeForcedTicket(long chunkPosIn, Ticket ticketIn) {}
}
