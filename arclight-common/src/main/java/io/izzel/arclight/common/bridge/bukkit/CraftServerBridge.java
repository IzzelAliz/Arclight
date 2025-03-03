package io.izzel.arclight.common.bridge.bukkit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;

public interface CraftServerBridge {

    void bridge$setPlayerList(PlayerList playerList);

    void bridge$removeWorld(ServerLevel world);

    ChunkGenerator bridge$consumeGeneratorCache(String name);

    // One-shot custom chunk generation cache
    void bridge$offerGeneratorCache(String name, ChunkGenerator generator);

    BiomeProvider bridge$consumeBiomeProviderCache(String name);

    // One-shot custom chunk generation cache
    void bridge$offerBiomeProviderCache(String name, BiomeProvider provider);
}
