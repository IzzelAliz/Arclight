package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.core.server.level.DistanceManagerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.*;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

import java.util.*;
import java.util.function.Consumer;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin implements DistanceManagerBridge {

    // @formatter:off
    @Shadow private long ticketTickCounter;
    @Shadow @Final private DistanceManager.ChunkTicketTracker ticketTracker;
    @Shadow protected abstract SortedArraySet<Ticket<?>> getTickets(long p_229848_1_);
    @Shadow private static int getTicketLevelAt(SortedArraySet<Ticket<?>> p_229844_0_) { return 0; }
    @Shadow @Final public Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets;
    @Shadow abstract TickingTracker tickingTracker();
    @Shadow @Final @Mutable private Set<ChunkHolder> chunksToUpdateFutures;
    @Invoker("purgeStaleTickets") public abstract void bridge$tick();
    // @formatter:on

    @Unique
    private Queue<ChunkHolder> arclight$scheduleUpdatingQueue = new LinkedList<>();

    @Override
    public void arclight$offerUpdate(ChunkHolder holder) {
        arclight$scheduleUpdatingQueue.add(holder);
    }

    @Decorate(method = "runAllUpdates", inject = true, at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"))
    private void arclight$runQueuedUpdates(ChunkMap map) {
        final var queue = arclight$scheduleUpdatingQueue;
        for (ChunkHolder now = queue.poll(); now != null; now = queue.poll()) {
            ((ChunkHolderBridge) now).bridge$callEventIfUnloading(map);
        }
    }

    @Decorate(method = "removePlayer", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"))
    private Object arclight$nullsafeRemovePlayer(Long2ObjectMap<ServerPlayer> instance, long l) throws Throwable {
        Object set = DecorationOps.callsite().invoke(instance, l);
        if (set == null) {
            return DecorationOps.cancel().invoke();
        }
        return set;
    }

    public <T> boolean addRegionTicketAtDistance(TicketType<T> type, ChunkPos pos, int level, T value) {
        var ticket = new Ticket<>(type, 33 - level, value);
        var ret = this.addTicket(pos.toLong(), ticket);
        this.tickingTracker().addTicket(pos.toLong(), ticket);
        return ret;
    }

    public <T> boolean removeRegionTicketAtDistance(TicketType<T> type, ChunkPos pos, int level, T value) {
        var ticket = new Ticket<>(type, 33 - level, value);
        var ret = this.removeTicket(pos.toLong(), ticket);
        this.tickingTracker().removeTicket(pos.toLong(), ticket);
        return ret;
    }

    public <T> boolean addTicketAtLevel(TicketType<T> type, ChunkPos pos, int level, T value) {
        Ticket<T> ticket = new Ticket<>(type, level, value);
        return this.addTicket(pos.toLong(), ticket);
    }

    public <T> boolean removeTicketAtLevel(TicketType<T> type, ChunkPos pos, int level, T value) {
        Ticket<T> ticket = new Ticket<>(type, level, value);
        return this.removeTicket(pos.toLong(), ticket);
    }

    @Override
    public <T> boolean bridge$addTicketAtLevel(TicketType<T> type, ChunkPos pos, int level, T value) {
        return addTicketAtLevel(type, pos, level, value);
    }

    @Override
    public <T> boolean bridge$removeTicketAtLevel(TicketType<T> type, ChunkPos pos, int level, T value) {
        return removeTicketAtLevel(type, pos, level, value);
    }

    boolean removeTicket(long chunkPosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> ticketSet = this.getTickets(chunkPosIn);
        boolean removed = false;
        if (ticketSet.remove(ticketIn)) {
            removed = true;
        }
        if (ticketSet.isEmpty()) {
            this.tickets.remove(chunkPosIn);
        }
        this.ticketTracker.update(chunkPosIn, getTicketLevelAt(ticketSet), false);
        if (bridge$platform$isTicketForceTick(ticketIn)) {
            this.bridge$forge$removeForcedTicket(chunkPosIn, ticketIn);
        }
        return removed;
    }

    @Override
    public boolean bridge$removeTicket(long chunkPos, Ticket<?> ticket) {
        return removeTicket(chunkPos, ticket);
    }

    boolean addTicket(long chunkPosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> ticketSet = this.getTickets(chunkPosIn);
        int level = getTicketLevelAt(ticketSet);
        Ticket<?> ticket = ticketSet.addOrGet(ticketIn);
        ticket.setCreatedTick(this.ticketTickCounter);
        if (ticketIn.getTicketLevel() < level) {
            this.ticketTracker.update(chunkPosIn, ticketIn.getTicketLevel(), true);
        }
        if (bridge$platform$isTicketForceTick(ticketIn)) {
            this.bridge$forge$addForcedTicket(chunkPosIn, ticketIn);
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
                this.ticketTracker.update(entry.getLongKey(), getTicketLevelAt(tickets), false);
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
