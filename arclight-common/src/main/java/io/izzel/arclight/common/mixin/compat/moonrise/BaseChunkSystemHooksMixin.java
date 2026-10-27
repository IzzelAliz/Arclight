package io.izzel.arclight.common.mixin.compat.moonrise;

import io.izzel.arclight.common.bridge.core.world.level.chunk.LevelChunkBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Bukkit;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import java.util.Arrays;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "ca.spottedleaf.moonrise.common.util.BaseChunkSystemHooks", remap = false)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.PRESENT)
public abstract class BaseChunkSystemHooksMixin {
    @Inject(method = "onChunkBorder(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/server/level/ChunkHolder;)V",
        at = @At("RETURN"), require = 1, allow = 1, remap = false)
    private void arclight$loadCallback(LevelChunk chunk, ChunkHolder holder, CallbackInfo ci) {
        ((LevelChunkBridge) chunk).bridge$loadCallback(loaded -> {
            var bukkitChunk = ((LevelChunkBridge) loaded).bridge$getBukkitChunk();
            Bukkit.getPluginManager().callEvent(new EntitiesLoadEvent(bukkitChunk, Arrays.asList(bukkitChunk.getEntities())));
        });
    }

    @Inject(method = "onChunkNotBorder(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/server/level/ChunkHolder;)V",
        at = @At("RETURN"), require = 1, allow = 1, remap = false)
    private void arclight$unloadCallback(LevelChunk chunk, ChunkHolder holder, CallbackInfo ci) {
        var bukkitChunk = ((LevelChunkBridge) chunk).bridge$getBukkitChunk();
        Bukkit.getPluginManager().callEvent(new EntitiesUnloadEvent(bukkitChunk, Arrays.asList(bukkitChunk.getEntities())));
        ((LevelChunkBridge) chunk).bridge$unloadCallback();
    }
}
