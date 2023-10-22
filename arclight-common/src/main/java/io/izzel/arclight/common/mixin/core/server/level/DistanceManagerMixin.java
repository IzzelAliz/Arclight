package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.server.TicketManagerBridge;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.*;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin implements TicketManagerBridge {

    // @formatter:off
    @Shadow private long ticketTickCounter;
    @Shadow @Final private DistanceManager.ChunkTicketTracker ticketTracker;
    @Shadow protected abstract SortedArraySet<Ticket<?>> getTickets(long p_229848_1_);
    @Shadow private static int getTicketLevelAt(SortedArraySet<Ticket<?>> p_229844_0_) { return 0; }
    @Shadow @Final public Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets;
    @Shadow abstract TickingTracker tickingTracker();
    @Shadow @Final private Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> forcedTickets;
    @Invoker("purgeStaleTickets") public abstract void bridge$tick();
    // @formatter:on

    @Inject(method = "removePlayer", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", remap = false, target = "Lit/unimi/dsi/fastutil/objects/ObjectSet;remove(Ljava/lang/Object;)Z"))
    private void arclight$remove(SectionPos p_140829_, ServerPlayer p_140830_, CallbackInfo ci, ChunkPos pos, long l, ObjectSet<?> set) {
        if (set == null) {
            ci.cancel();
        }
    }

    @Redirect(method = "runAllUpdates", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Set;forEach(Ljava/util/function/Consumer;)V"))
    private void arclight$safeIter(Set<ChunkHolder> instance, Consumer<ChunkHolder> consumer) {
        // Iterate pending chunk updates with protection against concurrent modification exceptions
        var iter = instance.iterator();
        var expectedSize = instance.size();
        do {
            var chunkHolder = iter.next();
            iter.remove();
            expectedSize--;

            consumer.accept(chunkHolder);

            // Reset iterator if set was modified using add()
            if (instance.size() != expectedSize) {
                expectedSize = instance.size();
                iter = instance.iterator();
            }
        } while (iter.hasNext());
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
        if (ticketIn.isForceTicks()) {
            SortedArraySet<Ticket<?>> tickets = this.forcedTickets.get(chunkPosIn);
            if (tickets != null) {
                tickets.remove(ticketIn);
            }
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
        if (ticketIn.isForceTicks()) {
            SortedArraySet<Ticket<?>> tickets = this.forcedTickets.computeIfAbsent(chunkPosIn, e -> SortedArraySet.create(4));
            tickets.addOrGet(ticketIn);
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
