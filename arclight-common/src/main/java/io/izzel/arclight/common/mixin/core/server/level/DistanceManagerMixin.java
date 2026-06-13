package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.mod.util.ArclightTickets;
import io.izzel.arclight.common.bridge.core.server.level.DistanceManagerBridge;
import io.izzel.arclight.common.bridge.core.server.level.TicketBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.util.*;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin implements DistanceManagerBridge {

    // @formatter:off
    @Shadow @Final private TicketStorage ticketStorage;
    @Shadow @Final protected Set<ChunkHolder> chunksToUpdateFutures;
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

    public boolean addRegionTicketAtDistance(TicketType type, ChunkPos pos, int level, Object value) {
        var ticket = ArclightTickets.create(type, 33 - level, value);
        var ret = this.ticketStorage.addTicket(ChunkPos.pack(pos.x(), pos.z()), ticket);
        return ret;
    }

    public boolean removeRegionTicketAtDistance(TicketType type, ChunkPos pos, int level, Object value) {
        var ticket = ArclightTickets.create(type, 33 - level, value);
        return this.ticketStorage.removeTicket(ChunkPos.pack(pos.x(), pos.z()), ticket);
    }

    public boolean addTicketAtLevel(TicketType type, ChunkPos pos, int level, Object value) {
        return this.ticketStorage.addTicket(ChunkPos.pack(pos.x(), pos.z()), ArclightTickets.create(type, level, value));
    }

    public boolean removeTicketAtLevel(TicketType type, ChunkPos pos, int level, Object value) {
        return this.ticketStorage.removeTicket(ChunkPos.pack(pos.x(), pos.z()), ArclightTickets.create(type, level, value));
    }

    @Override
    public boolean bridge$addTicketAtLevel(TicketType type, ChunkPos pos, int level, Object value) {
        return addTicketAtLevel(type, pos, level, value);
    }

    @Override
    public boolean bridge$removeTicketAtLevel(TicketType type, ChunkPos pos, int level, Object value) {
        return removeTicketAtLevel(type, pos, level, value);
    }

    @Override
    public boolean bridge$removeTicket(long chunkPos, Ticket ticket) {
        return this.ticketStorage.removeTicket(chunkPos, ticket);
    }

    @Override
    public boolean bridge$addTicket(long chunkPos, Ticket ticket) {
        return this.ticketStorage.addTicket(chunkPos, ticket);
    }

    @Override
    public void bridge$removeAllTicketsFor(TicketType ticketType, int ticketLevel, Object ticketIdentifier) {
        this.ticketStorage.removeTicketIf((ticket, chunkPos) ->
            ticket.getType() == ticketType
                && ticket.getTicketLevel() == ticketLevel
                && Objects.equals(((TicketBridge) (Object) ticket).bridge$getKey(), ticketIdentifier), null);
    }

    @Override
    public void bridge$tick(ChunkMap chunkMap) {
        this.ticketStorage.purgeStaleTickets(chunkMap);
    }
}
