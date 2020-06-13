package io.izzel.arclight.impl.mixin.v1_14.world.server;

import io.izzel.arclight.common.bridge.world.server.TicketManagerBridge;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Iterator;

@Mixin(TicketManager.class)
public abstract class TicketManagerMixin_1_14 implements TicketManagerBridge {

    // @formatter:off
    @Shadow @Final public Long2ObjectOpenHashMap<ObjectSortedSet<Ticket<?>>> tickets;
    @Shadow private long currentTime;
    @Shadow protected abstract ObjectSortedSet<Ticket<?>> getTickets(long chunkPosIn);
    @Shadow @Final private TicketManager.ChunkTicketTracker ticketTracker;
    @Shadow protected abstract int getChunkLevel(ObjectSortedSet<Ticket<?>> ticketsIn);
    @Invoker("tick") public abstract void bridge$tick();
    // @formatter:on

    @SuppressWarnings("unused") // mock
    public <T> boolean func_219356_a(TicketType<T> type, ChunkPos pos, int level, T value) {
        return addTicketAtLevel(type, pos, level, value);
    }

    @SuppressWarnings("unused") // mock
    public <T> boolean func_219345_b(TicketType<T> type, ChunkPos pos, int level, T value) {
        return removeTicketAtLevel(type, pos, level, value);
    }

    public <T> boolean addTicketAtLevel(TicketType<T> type, ChunkPos pos, int level, T value) {
        Ticket<T> ticket = new Ticket<>(type, level, value, this.currentTime);
        return this.addTicket(pos.asLong(), ticket);
    }

    public <T> boolean removeTicketAtLevel(TicketType<T> type, ChunkPos pos, int level, T value) {
        Ticket<T> ticket = new Ticket<>(type, level, value, this.currentTime);
        return this.removeTicket(pos.asLong(), ticket);
    }

    @Override
    public <T> boolean bridge$addTicketAtLevel(TicketType<T> type, ChunkPos pos, int level, T value) {
        return addTicketAtLevel(type, pos, level, value);
    }

    @Override
    public <T> boolean bridge$removeTicketAtLevel(TicketType<T> type, ChunkPos pos, int level, T value) {
        return removeTicketAtLevel(type, pos, level, value);
    }

    @SuppressWarnings("unused") // mock
    private boolean func_219349_b(long chunkPosIn, Ticket<?> ticketIn) {
        return removeTicket(chunkPosIn, ticketIn);
    }

    private boolean removeTicket(long chunkPosIn, Ticket<?> ticketIn) {
        ObjectSortedSet<Ticket<?>> objectsortedset = this.getTickets(chunkPosIn);

        boolean ret = false;
        if (objectsortedset.remove(ticketIn)) {
            ret = true;
        }

        if (objectsortedset.isEmpty()) {
            this.tickets.remove(chunkPosIn);
        }

        this.ticketTracker.updateSourceLevel(chunkPosIn, this.getChunkLevel(objectsortedset), false);
        return ret;
    }

    @Override
    public boolean bridge$removeTicket(long chunkPos, Ticket<?> ticket) {
        return removeTicket(chunkPos, ticket);
    }

    @SuppressWarnings("unused") // mock
    private boolean func_219347_a(long chunkPosIn, Ticket<?> ticketIn) {
        return addTicket(chunkPosIn, ticketIn);
    }

    private boolean addTicket(long chunkPosIn, Ticket<?> ticketIn) {
        ObjectSortedSet<Ticket<?>> objectsortedset = this.getTickets(chunkPosIn);
        ObjectBidirectionalIterator<Ticket<?>> objectbidirectionaliterator = objectsortedset.iterator();
        int i;
        if (objectbidirectionaliterator.hasNext()) {
            i = objectbidirectionaliterator.next().getLevel();
        } else {
            i = ChunkManager.MAX_LOADED_LEVEL + 1;
        }
        boolean ret = false;
        if (objectsortedset.add(ticketIn)) {
            ret = true;
        }

        if (ticketIn.getLevel() < i) {
            this.ticketTracker.updateSourceLevel(chunkPosIn, ticketIn.getLevel(), true);
        }

        return ret;
    }

    @Override
    public boolean bridge$addTicket(long chunkPos, Ticket<?> ticket) {
        return addTicket(chunkPos, ticket);
    }

    public <T> void removeAllTicketsFor(TicketType<T> ticketType, int ticketLevel, T ticketIdentifier) {
        Ticket<T> target = new Ticket<>(ticketType, ticketLevel, ticketIdentifier, this.currentTime);

        for (Iterator<ObjectSortedSet<Ticket<?>>> iterator = this.tickets.values().iterator(); iterator.hasNext(); ) {
            ObjectSortedSet<Ticket<?>> tickets = iterator.next();
            tickets.remove(target);

            if (tickets.isEmpty()) {
                iterator.remove();
            }
        }
    }

    @Override
    public <T> void bridge$removeAllTicketsFor(TicketType<T> ticketType, int ticketLevel, T ticketIdentifier) {
        removeAllTicketsFor(ticketType, ticketLevel, ticketIdentifier);
    }
}
