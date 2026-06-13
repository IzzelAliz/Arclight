package io.izzel.arclight.forge.mixin.core.server;

import com.google.common.collect.ImmutableSet;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin_Forge implements MinecraftServerBridge {

    // @formatter:off
    @Shadow(remap = false) public abstract void markWorldsDirty();
    // @formatter:on

    @Override
    public void arclight$onServerLoad(ServerLevel level) {
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(level));
    }

    @Override
    public void arclight$onServerUnload(ServerLevel level) {
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Unload(level));
    }

    @Override
    public void bridge$forge$markLevelsDirty() {
        this.markWorldsDirty();
    }

    @Override
    public void bridge$forge$reinstatePersistentChunks(ServerLevel level, LongSet forcedChunks) {
        ForgeChunkManager.reinstatePersistentChunks(level, forcedChunks);
    }

    private static Set<IForgeRegistry<?>> arclight$registries() {
        return ImmutableSet.of(ForgeRegistries.BLOCKS, ForgeRegistries.ITEMS,
            ForgeRegistries.MOB_EFFECTS, ForgeRegistries.POTIONS,
            ForgeRegistries.ENTITY_TYPES, ForgeRegistries.BLOCK_ENTITY_TYPES,
            ForgeRegistries.BIOMES);
    }

    @Override
    public void bridge$forge$lockRegistries() {
        for (var registry : arclight$registries()) {
            if (registry instanceof ForgeRegistry) {
                ((ForgeRegistry<?>) registry).freeze();
            }
        }
    }

    @Override
    public void bridge$forge$unlockRegistries() {
        for (var registry : arclight$registries()) {
            if (registry instanceof ForgeRegistry) {
                ((ForgeRegistry<?>) registry).unfreeze();
            }
        }
    }
}
