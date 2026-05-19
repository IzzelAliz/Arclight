package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.level.TicketTypeBridge;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.server.level.TicketType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TicketType.class)
public abstract class TicketTypeMixin implements TicketTypeBridge {

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static long pluginTimeout = 0L;
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
    private static final TicketType PLUGIN = new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION);
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
    private static final TicketType PLUGIN_TICKET = new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION);

    /**
     * @author IzzelAliz
     * @reason CraftBukkit exposes chunk-gc.period-in-ticks through TicketType.PLUGIN.
     */
    @Overwrite
    public long timeout() {
        return (Object) this == PLUGIN ? pluginTimeout : this.arclight$timeout();
    }

    @Accessor("timeout")
    public abstract long arclight$timeout();

    @Override @Accessor(value = "timeout")
    public abstract void bridge$setLifespan(long lifespan);
}
