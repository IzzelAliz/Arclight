package io.izzel.arclight.common.bridge.world;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
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

    TileEntity bridge$getTileEntity(BlockPos pos, boolean validate);

    SpigotWorldConfig bridge$spigotConfig();

    long bridge$ticksPerAnimalSpawns();

    long bridge$ticksPerMonsterSpawns();

    long bridge$ticksPerWaterSpawns();

    long bridge$ticksPerAmbientSpawns();

    long bridge$ticksPerWaterAmbientSpawns();

    RegistryKey<DimensionType> bridge$getTypeKey();
}
