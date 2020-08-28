package io.izzel.arclight.common.mixin.core.world.server;

import io.izzel.arclight.common.bridge.world.server.TicketTypeBridge;
import net.minecraft.util.Unit;
import net.minecraft.world.server.TicketType;
import org.bukkit.plugin.Plugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Comparator;

@Mixin(TicketType.class)
public abstract class TicketTypeMixin implements TicketTypeBridge {

    private static final TicketType<Unit> PLUGIN = TicketType.create("plugin", (a, b) -> 0);
    private static final TicketType<Plugin> PLUGIN_TICKET = TicketType.create("plugin_ticket", Comparator.comparing(it -> it.getClass().getName()));

    @Override @Accessor(value = "lifespan")
    public abstract void bridge$setLifespan(long lifespan);
}
