package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.level.TicketBridge;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(Ticket.class)
public abstract class TicketMixin implements TicketBridge {

    @Shadow public abstract TicketType getType();
    @Shadow public abstract int getTicketLevel();

    @Unique
    private Object arclight$key;

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    public static Ticket of(TicketType type, int level, Object key) {
        Ticket ticket = new Ticket(type, level);
        ((TicketBridge) (Object) ticket).bridge$setKey(key);
        return ticket;
    }

    @Override
    public Object bridge$getKey() {
        return this.arclight$key;
    }

    @Override
    public void bridge$setKey(Object key) {
        this.arclight$key = key;
    }

    /**
     * @author Arclight
     * @reason Bukkit plugin chunk tickets compare by plugin key
     */
    @Overwrite
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Ticket other)) {
            return false;
        }
        if (this.getTicketLevel() != other.getTicketLevel() || !Objects.equals(this.getType(), other.getType())) {
            return false;
        }
        Object otherKey = ((TicketBridge) (Object) other).bridge$getKey();
        return Objects.equals(this.arclight$key, otherKey);
    }

    /**
     * @author Arclight
     * @reason Bukkit plugin chunk tickets compare by plugin key
     */
    @Overwrite
    public int hashCode() {
        return Objects.hash(this.getType(), this.getTicketLevel(), this.arclight$key);
    }
}
