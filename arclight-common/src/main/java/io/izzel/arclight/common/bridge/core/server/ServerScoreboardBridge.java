package io.izzel.arclight.common.bridge.core.server;

import java.util.Collection;
import net.minecraft.world.scores.PlayerTeam;

public interface ServerScoreboardBridge {
    boolean bridge$addPlayersToTeam(Collection<String> players, PlayerTeam team);
    void bridge$removePlayersFromTeam(Collection<String> players, PlayerTeam team);

    default boolean addPlayersToTeam(java.util.Collection<String> players, net.minecraft.world.scores.PlayerTeam team) {
        return this.bridge$addPlayersToTeam(players, team);
    }

    default void removePlayersFromTeam(java.util.Collection<String> players, net.minecraft.world.scores.PlayerTeam team) {
        this.bridge$removePlayersFromTeam(players, team);
    }

}
