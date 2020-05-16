package io.izzel.arclight.bridge.world;

import org.bukkit.craftbukkit.v1_14_R1.CraftServer;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.generator.ChunkGenerator;

public interface WorldBridge extends IWorldWriterBridge {

    CraftServer bridge$getServer();

    CraftWorld bridge$getWorld();

    boolean bridge$isPvpMode();

    boolean bridge$isKeepSpawnInMemory();

    boolean bridge$isPopulating();

    void bridge$setPopulating(boolean populating);

    ChunkGenerator bridge$getGenerator();
}
