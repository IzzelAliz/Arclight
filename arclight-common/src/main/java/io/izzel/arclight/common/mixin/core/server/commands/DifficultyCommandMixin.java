package io.izzel.arclight.common.mixin.core.server.commands;

import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.common.bridge.core.world.level.storage.DerivedLevelDataBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.DifficultyCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Retain the native query/feedback/duplicate guard; route only its set read and write to the source world's owner. */
@Mixin(DifficultyCommand.class)
public abstract class DifficultyCommandMixin {

    @Redirect(method = "setDifficulty", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorldData()Lnet/minecraft/world/level/storage/WorldData;"))
    private static WorldData arclight$sourceDifficultyRead(MinecraftServer server, CommandSourceStack source, Difficulty difficulty) {
        return arclight$owner(source.getLevel());
    }

    @Redirect(method = "setDifficulty", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;setDifficulty(Lnet/minecraft/world/Difficulty;Z)V"))
    private static void arclight$sourceDifficultyWrite(MinecraftServer server, Difficulty difficulty, boolean ignoreLock, CommandSourceStack source, Difficulty requested) {
        // The command's vanilla call passes ignoreLock=true. CraftBukkit writes the world
        // data directly; do not call the server setter, which rewrites the global world and
        // broadcasts to every player. PrimaryLevelData#setDifficulty owns scoped packets.
        arclight$owner(source.getLevel()).setDifficulty(difficulty);
    }

    private static WorldData arclight$owner(ServerLevel level) {
        LevelData data = level.getLevelData();
        if (data instanceof WorldData worldData) {
            return worldData;
        }
        if (data instanceof DerivedLevelDataBridge derived) {
            return derived.bridge$getWorldData();
        }
        // Match CraftWorld's unknown-adapter fallback instead of writing the global data.
        return ((ServerLevelBridge) level).bridge$getPrimaryLevelData();
    }
}
