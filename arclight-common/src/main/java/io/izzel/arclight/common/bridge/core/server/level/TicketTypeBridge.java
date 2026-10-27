package io.izzel.arclight.common.bridge.core.server.level;

public interface TicketTypeBridge {

    static net.minecraft.server.level.TicketType bridge$plugin() {
        return java.util.Objects.requireNonNull(net.minecraft.core.registries.BuiltInRegistries.TICKET_TYPE.getValue(
            net.minecraft.resources.Identifier.withDefaultNamespace("plugin")), "Plugin ticket type is not registered");
    }

    static net.minecraft.server.level.TicketType bridge$pluginTicket() {
        return java.util.Objects.requireNonNull(net.minecraft.core.registries.BuiltInRegistries.TICKET_TYPE.getValue(
            net.minecraft.resources.Identifier.withDefaultNamespace("plugin_ticket")), "Plugin-owned ticket type is not registered");
    }

    void bridge$setLifespan(long lifespan);

    void bridge$setPluginTimeout(long timeout);
}
