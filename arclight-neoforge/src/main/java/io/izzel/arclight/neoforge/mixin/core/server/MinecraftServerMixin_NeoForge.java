package io.izzel.arclight.neoforge.mixin.core.server;

import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.TicketStorage;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin_NeoForge implements MinecraftServerBridge {

    // @formatter:off
    @Shadow(remap = false) public abstract void markWorldsDirty();
    // @formatter:on

    @Override
    public void arclight$onServerLoad(ServerLevel level) {
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
    }

    @Override
    public void arclight$onServerUnload(ServerLevel level) {
        NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));
    }

    @Override
    public void bridge$forge$markLevelsDirty() {
        this.markWorldsDirty();
    }

    @Override
    public void bridge$forge$reinstatePersistentChunks(ServerLevel level, TicketStorage savedData) {
        ForcedChunkManager.activateAllDeactivatedTickets(level, savedData);
    }
}
