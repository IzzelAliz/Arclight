package io.izzel.arclight.common.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.server.level.TicketBridge;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Objects;

@Mixin(TicketStorage.class)
public abstract class TicketStorageMixin {

    /**
     * @author IzzelAliz
     * @reason CraftBukkit includes the plugin key in ticket identity for both add and remove.
     */
    @Overwrite
    private static boolean isTicketSameTypeAndLevel(Ticket ticket, Ticket other) {
        return other.getType() == ticket.getType()
            && other.getTicketLevel() == ticket.getTicketLevel()
            && Objects.equals(((TicketBridge) (Object) ticket).bridge$getKey(),
                ((TicketBridge) (Object) other).bridge$getKey());
    }
}
