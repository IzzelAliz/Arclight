package io.izzel.arclight.mixin.core.world.server;

import io.izzel.arclight.bridge.world.server.TicketTypeBridge;
import net.minecraft.util.Unit;
import net.minecraft.world.server.TicketType;
import org.bukkit.plugin.Plugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import io.izzel.arclight.mod.ArclightConstants;

@Mixin(TicketType.class)
public abstract class TicketTypeMixin implements TicketTypeBridge {

    private static final TicketType<Unit> PLUGIN = ArclightConstants.PLUGIN;
    private static final TicketType<Plugin> PLUGIN_TICKET = ArclightConstants.PLUGIN_TICKET;

    @Override @Accessor(value = "lifespan")
    public abstract void bridge$setLifespan(long lifespan);
}
