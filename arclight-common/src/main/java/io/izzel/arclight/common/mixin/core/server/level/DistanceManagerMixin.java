package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.server.TicketManagerBridge;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin implements TicketManagerBridge {

    // @formatter:off
    @Shadow private long ticketTickCounter;
    @Shadow @Final private DistanceManager.ChunkTicketTracker ticketTracker;
    @Shadow protected abstract SortedArraySet<Ticket<?>> getTickets(long p_229848_1_);
    @Shadow private static int getTicketLevelAt(SortedArraySet<Ticket<?>> p_229844_0_) { return 0; }
    @Shadow @Final public Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets;
    @Invoker("purgeStaleTickets") public abstract void bridge$tick();
    // @formatter:on

    @Inject(method = "removePlayer", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", remap = false, target = "Lit/unimi/dsi/fastutil/objects/ObjectSet;remove(Ljava/lang/Object;)Z"))
    private void arclight$remove(SectionPos p_140829_, ServerPlayer p_140830_, CallbackInfo ci, ChunkPos pos, long l, ObjectSet<?> set) {
        if (set == null) {
            ci.cancel();
        }
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
