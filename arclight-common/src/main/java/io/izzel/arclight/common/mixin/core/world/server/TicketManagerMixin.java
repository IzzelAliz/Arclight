package io.izzel.arclight.common.mixin.core.world.server;

import io.izzel.arclight.common.bridge.world.server.TicketManagerBridge;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Iterator;

@Mixin(TicketManager.class)
public abstract class TicketManagerMixin implements TicketManagerBridge {

    // @formatter:off
    @Shadow private long currentTime;
    @Shadow @Final private TicketManager.ChunkTicketTracker ticketTracker;
    @Shadow protected abstract SortedArraySet<Ticket<?>> getTicketSet(long p_229848_1_);
    @Shadow private static int getLevel(SortedArraySet<Ticket<?>> p_229844_0_) { return 0; }
    @Shadow @Final public Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets;
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
        Ticket<T> ticket = new Ticket<>(type, level, value);
        return this.addTicket(pos.asLong(), ticket);
    }

    public <T> boolean removeTicketAtLevel(TicketType<T> type, ChunkPos pos, int level, T value) {
        Ticket<T> ticket = new Ticket<>(type, level, value);
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
        SortedArraySet<Ticket<?>> ticketSet = this.getTicketSet(chunkPosIn);
        boolean removed = false;
        if (ticketSet.remove(ticketIn)) {
            removed = true;
        }
        if (ticketSet.isEmpty()) {
            this.tickets.remove(chunkPosIn);
        }
        this.ticketTracker.updateSourceLevel(chunkPosIn, getLevel(ticketSet), false);
        return removed;
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
        SortedArraySet<Ticket<?>> ticketSet = this.getTicketSet(chunkPosIn);
        int level = getLevel(ticketSet);
        Ticket<?> ticket = ticketSet.func_226175_a_(ticketIn);
        ticket.setTimestamp(this.currentTime);
        if (ticketIn.getLevel() < level) {
            this.ticketTracker.updateSourceLevel(chunkPosIn, ticketIn.getLevel(), true);
        }
        return ticketIn == ticket;
    }

    @Override
    public boolean bridge$addTicket(long chunkPos, Ticket<?> ticket) {
        return addTicket(chunkPos, ticket);
    }

    public <T> void removeAllTicketsFor(TicketType<T> ticketType, int ticketLevel, T ticketIdentifier) {
        Ticket<T> target = new Ticket<>(ticketType, ticketLevel, ticketIdentifier);
        Iterator<Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>>> iterator = this.tickets.long2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>> entry = iterator.next();
            SortedArraySet<Ticket<?>> tickets = entry.getValue();
            if (tickets.remove(target)) {
                this.ticketTracker.updateSourceLevel(entry.getLongKey(), getLevel(tickets), false);
                if (tickets.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public <T> void bridge$removeAllTicketsFor(TicketType<T> ticketType, int ticketLevel, T ticketIdentifier) {
        removeAllTicketsFor(ticketType, ticketLevel, ticketIdentifier);
    }
}
