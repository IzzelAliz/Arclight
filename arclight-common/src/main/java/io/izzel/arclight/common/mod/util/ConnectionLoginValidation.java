package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.bridge.core.network.ConnectionLoginValidationBridge;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.connection.PaperPlayerLoginConnection;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/** Login validation transport; the legacy event remains the final login-phase consumer. */
public final class ConnectionLoginValidation {

    private ConnectionLoginValidation() {
    }

    public static final class State {
        private @Nullable PaperPlayerLoginConnection loginConnection;
        private @Nullable Optional<Component> legacyOverride;
    }

    private static State state(Connection connection) {
        return ((ConnectionLoginValidationBridge) connection).bridge$getLoginValidationState();
    }

    /** Call immediately before the retained PlayerLoginEvent dispatch. */
    public static PlayerLoginEvent.Result beforeLegacyLogin(Connection connection, ServerLoginPacketListenerImpl listener, PlayerLoginEvent legacy) {
        State state = state(connection);
        if (state.loginConnection == null) {
            state.loginConnection = new PaperPlayerLoginConnection(listener);
        }
        var event = new PlayerConnectionValidateLoginEvent(state.loginConnection,
            legacy.getResult() == PlayerLoginEvent.Result.ALLOWED ? null : legacy.kickMessage());
        event.callEvent();
        if (event.isAllowed()) {
            legacy.allow();
        } else {
            PlayerLoginEvent.Result reason = legacy.getResult() == PlayerLoginEvent.Result.ALLOWED
                ? PlayerLoginEvent.Result.KICK_OTHER : legacy.getResult();
            legacy.disallow(reason, event.getKickMessage());
        }
        return legacy.getResult();
    }

    /** Call immediately after the retained PlayerLoginEvent dispatch, before its result is consumed. */
    public static void afterLegacyLogin(Connection connection, PlayerLoginEvent legacy, PlayerLoginEvent.Result initialResult) {
        if (legacy.getResult() != initialResult) {
            state(connection).legacyOverride = Optional.ofNullable(legacy.getResult() == PlayerLoginEvent.Result.ALLOWED
                ? null : PaperAdventure.asVanilla(legacy.kickMessage()));
        }
    }

    public static @Nullable Component validateConfiguration(Connection connection, PlayerConfigurationConnection configuration, @Nullable Component nativeReason) {
        var event = new PlayerConnectionValidateLoginEvent(configuration,
            nativeReason == null ? null : PaperAdventure.asAdventure(nativeReason));
        event.callEvent();
        Component reason = event.isAllowed() ? null : PaperAdventure.asVanilla(event.getKickMessage());
        Optional<Component> override = state(connection).legacyOverride;
        // Paper deliberately preserves a legacy plugin's changed verdict across both native checks.
        return override == null ? reason : override.orElse(null);
    }
}
