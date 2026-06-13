package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.bridge.core.server.level.TicketBridge;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;

public final class ArclightTickets {

    private ArclightTickets() {
    }

    public static Ticket create(TicketType type, int level, Object key) {
        Ticket ticket = new Ticket(type, level);
        ((TicketBridge) (Object) ticket).bridge$setKey(key);
        return ticket;
    }
}
