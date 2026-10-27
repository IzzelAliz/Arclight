package io.izzel.arclight.common.bridge.core.server.players;

import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import org.jetbrains.annotations.Nullable;

public interface PlayerListLoginPolicyBridge {

    // Null permits this policy check; a component is the actual disconnect message.
    @Nullable Component bridge$checkWhitelistLogin(NameAndId profile);

    @Nullable Component bridge$checkFullServerLogin(NameAndId profile);
}
