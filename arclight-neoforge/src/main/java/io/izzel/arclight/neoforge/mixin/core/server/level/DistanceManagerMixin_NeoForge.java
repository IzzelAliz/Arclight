package io.izzel.arclight.neoforge.mixin.core.server.level;

import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;

/**
 * NeoForge 26.1: {@code forcedTickets} / {@code Ticket#isForceTicks()} removed.
 * Forced-chunk behaviour uses {@link io.izzel.arclight.common.mixin.core.server.level.DistanceManagerMixin} + {@code TicketStorage}.
 */
@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin_NeoForge {
}
