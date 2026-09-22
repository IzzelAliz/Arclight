package io.izzel.arclight.common.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.world.level.GameRulesBridge;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(GameRules.class)
public abstract class GameRulesMixin implements GameRulesBridge {

    @Shadow
    @Final
    private GameRuleMap rules;

    // CraftWorld links against this Spigot-only, per-world overload.
    public <T> void set(GameRule<T> gameRule, T value, ServerLevel level) {
        if (!rules.has(gameRule)) {
            throw new IllegalArgumentException("Tried to set invalid game rule");
        }
        rules.set(gameRule, value);
        if (level == null) {
            return;
        }

        // Match Spigot's per-world MinecraftServer.onGameRuleChanged overload;
        // the vanilla callback would notify players and waypoints in other worlds.
        level.getServer().notificationManager().onGameRuleChanged(gameRule, value);
        if (gameRule == GameRules.REDUCED_DEBUG_INFO) {
            byte event = (byte) ((Boolean) value ? 22 : 23);
            for (ServerPlayer player : level.players()) {
                player.connection.send(new ClientboundEntityEventPacket(player, event));
            }
        } else if (gameRule == GameRules.LIMITED_CRAFTING || gameRule == GameRules.IMMEDIATE_RESPAWN) {
            var type = gameRule == GameRules.LIMITED_CRAFTING
                ? ClientboundGameEventPacket.LIMITED_CRAFTING : ClientboundGameEventPacket.IMMEDIATE_RESPAWN;
            var packet = new ClientboundGameEventPacket(type, (Boolean) value ? 1.0F : 0.0F);
            level.players().forEach(player -> player.connection.send(packet));
        } else if (gameRule == GameRules.LOCATOR_BAR) {
            var waypoints = level.getWaypointManager();
            if ((Boolean) value) {
                level.players().forEach(waypoints::updatePlayer);
            } else {
                waypoints.breakAllConnections();
            }
        } else if (gameRule == GameRules.SPAWN_MONSTERS) {
            level.setSpawnSettings(level.isSpawningMonsters());
        }
    }

    @Override
    public Set<GameRule<?>> arclight$getAllRules() {
        return rules.keySet();
    }
}
