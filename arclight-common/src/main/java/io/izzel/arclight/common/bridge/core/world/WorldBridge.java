package io.izzel.arclight.common.bridge.core.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.dimension.LevelStem;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.generator.ChunkGenerator;
import org.spigotmc.SpigotWorldConfig;

public interface WorldBridge extends IWorldWriterBridge, IWorldBridge {

    CraftServer bridge$getServer();

    CraftWorld bridge$getWorld();

    boolean bridge$isPvpMode();

    boolean bridge$isKeepSpawnInMemory();

    boolean bridge$isPopulating();

    void bridge$setPopulating(boolean populating);

    ChunkGenerator bridge$getGenerator();

    BlockEntity bridge$getTileEntity(BlockPos pos, boolean validate);

    SpigotWorldConfig bridge$spigotConfig();

    long bridge$ticksPerAnimalSpawns();

    long bridge$ticksPerMonsterSpawns();

    long bridge$ticksPerWaterSpawns();

    long bridge$ticksPerAmbientSpawns();

    long bridge$ticksPerWaterAmbientSpawns();

    long bridge$ticksPerWaterUndergroundSpawns();

    ResourceKey<LevelStem> bridge$getTypeKey();
}
