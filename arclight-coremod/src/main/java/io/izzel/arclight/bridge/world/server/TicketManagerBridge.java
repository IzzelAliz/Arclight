package io.izzel.arclight.bridge.world.server;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketType;

public interface TicketManagerBridge {

    <T> boolean bridge$addTicketAtLevel(TicketType<T> type, ChunkPos pos, int level, T value);

    <T> boolean bridge$removeTicketAtLevel(TicketType<T> type, ChunkPos pos, int level, T value);

    boolean bridge$addTicket(long chunkPos, Ticket<?> ticket);

    boolean bridge$removeTicket(long chunkPos, Ticket<?> ticket);

    void bridge$tick();

    Long2ObjectOpenHashMap<ObjectSortedSet<Ticket<?>>> bridge$getTickets();

    <T> void bridge$removeAllTicketsFor(TicketType<T> ticketType, int ticketLevel, T ticketIdentifier);
}
