package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.common.bridge.core.world.level.storage.DerivedLevelDataBridge;
import io.papermc.paper.event.world.WorldDifficultyChangeEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.WorldData;
import org.bukkit.craftbukkit.util.CraftDifficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerDifficultyEventMixin implements MinecraftServerBridge {

    // CraftWorld and DifficultyCommand enter this world-specific bridge instead of
    // the native global setter; retain their existing WorldData ownership and writes.
    public void bridge$setDifficulty(ServerLevel level, Difficulty difficulty, CommandSourceStack source, boolean ignoreLock) {
        WorldData data = arclight$difficultyOwner(level);
        if (ignoreLock || !data.isDifficultyLocked()) {
            new WorldDifficultyChangeEvent(((ServerLevelBridge) level).bridge$getWorld(),
                (io.papermc.paper.command.brigadier.CommandSourceStack) (Object) source,
                CraftDifficulty.toBukkit(difficulty)).callEvent();
            // Keep the existing world-scoped write; the event is notification-only.
            data.setDifficulty(difficulty);
            // PrimaryLevelDataMixin already sends packets to players sharing this actual
            // WorldData owner. Do not duplicate those sends or broadcast to other owners.
        }
    }

    @Unique
    private static WorldData arclight$difficultyOwner(ServerLevel level) {
        var data = level.getLevelData();
        if (data instanceof WorldData worldData) {
            return worldData;
        }
        if (data instanceof DerivedLevelDataBridge derived) {
            return derived.bridge$getWorldData();
        }
        return ((ServerLevelBridge) level).bridge$getPrimaryLevelData();
    }
}
