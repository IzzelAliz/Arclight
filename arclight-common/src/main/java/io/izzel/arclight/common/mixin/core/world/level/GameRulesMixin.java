package io.izzel.arclight.common.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.world.level.GameRulesBridge;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.stream.Collectors;

@Mixin(GameRules.class)
public abstract class GameRulesMixin implements GameRulesBridge {

    @Shadow public abstract java.util.stream.Stream<GameRule<?>> availableRules();

    @Shadow public abstract <T> T get(GameRule<T> rule);

    @Shadow public abstract <T> void set(GameRule<T> rule, T value, MinecraftServer server);

    @Unique
    public void assignFrom(GameRules source, @Nullable ServerLevel level) {
        MinecraftServer server = level != null ? level.getServer() : null;
        for (GameRule<?> rule : ((GameRulesBridge) source).arclight$getAllRules()) {
            copyRule(rule, source, server);
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private <T> void copyRule(GameRule<T> rule, GameRules source, @Nullable MinecraftServer server) {
        if (server != null) {
            this.set(rule, source.get(rule), server);
        }
    }

    @Override
    public Set<GameRule<?>> arclight$getAllRules() {
        return this.availableRules().collect(Collectors.toSet());
    }

    @Inject(method = "set", at = @At("RETURN"))
    private <T> void arclight$perWorldCallback(GameRule<T> rule, T value, MinecraftServer server, CallbackInfo ci) {
        if (server == null) {
            return;
        }
        if (rule == GameRules.REDUCED_DEBUG_INFO) {
            boolean enabled = (Boolean) value;
            byte flag = (byte) (enabled ? 22 : 23);
            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : level.players()) {
                    player.connection.send(new ClientboundEntityEventPacket(player, flag));
                }
            }
        } else if (rule == GameRules.LIMITED_CRAFTING) {
            float flag = (Boolean) value ? 1.0F : 0.0F;
            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : level.players()) {
                    player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LIMITED_CRAFTING, flag));
                }
            }
        } else if (rule == GameRules.IMMEDIATE_RESPAWN) {
            float flag = (Boolean) value ? 1.0F : 0.0F;
            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : level.players()) {
                    player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.IMMEDIATE_RESPAWN, flag));
                }
            }
        } else if (rule == GameRules.RESPAWN_RADIUS) {
            for (ServerLevel level : server.getAllLevels()) {
                io.izzel.arclight.common.mod.util.ArclightLevelHelper.setDefaultSpawnPos(level, io.izzel.arclight.common.mod.util.ArclightLevelHelper.getSharedSpawnPos(level), io.izzel.arclight.common.mod.util.ArclightLevelHelper.getSharedSpawnAngle(level));
            }
        }
    }
}
