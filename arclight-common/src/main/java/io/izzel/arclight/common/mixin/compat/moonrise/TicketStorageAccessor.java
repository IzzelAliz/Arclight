package io.izzel.arclight.common.mixin.compat.moonrise;

import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.function.BiConsumer;

@Mixin(TicketStorage.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.PRESENT)
public interface TicketStorageAccessor {
    @Invoker("forEachTicket")
    void arclight$forEachTicket(BiConsumer<ChunkPos, Ticket> consumer);
}
