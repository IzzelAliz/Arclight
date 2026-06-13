package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.level.TicketTypeBridge;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.server.level.TicketType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TicketType.class)
public abstract class TicketTypeMixin implements TicketTypeBridge {

    private static final int PLUGIN_FLAGS = TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_CAN_EXPIRE_IF_UNLOADED;

    @Unique
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
    public static final TicketType PLUGIN = new TicketType(TicketType.NO_TIMEOUT, 0);

    @Unique
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
    public static final TicketType PLUGIN_TICKET = new TicketType(TicketType.NO_TIMEOUT, PLUGIN_FLAGS);

    @Override
    @Accessor("timeout")
    @Mutable
    public abstract void bridge$setLifespan(long lifespan);
}
