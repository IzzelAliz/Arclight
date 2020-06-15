package io.izzel.arclight.common.mixin.v1_15.world.server;

import io.izzel.arclight.common.mixin.core.world.server.ServerWorldMixin;
import net.minecraft.profiler.IProfiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.border.IBorderListener;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerMultiWorld;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.Executor;

@Mixin(ServerMultiWorld.class)
public abstract class ServerMultiWorldMixin_1_15 extends ServerWorldMixin {

    // @formatter:off
    @Shadow(remap = false) @Final @Mutable private ServerWorld delegate;
    @Shadow(remap = false) @Final @Mutable private IBorderListener borderListener;
    // @formatter:on

    public void arclight$constructor$super(MinecraftServer serverIn, Executor executor, SaveHandler saveHandler, WorldInfo worldInfo, DimensionType dimType, IProfiler profiler, IChunkStatusListener listener, org.bukkit.generator.ChunkGenerator gen, org.bukkit.World.Environment env) {
        throw new RuntimeException();
    }

    public void arclight$constructor(ServerWorld world, MinecraftServer serverIn, Executor executor, SaveHandler saveHandler, DimensionType dimType, IProfiler profiler, IChunkStatusListener listener, WorldInfo worldInfo, org.bukkit.generator.ChunkGenerator gen, org.bukkit.World.Environment env) {
        arclight$constructor$super(serverIn, executor, saveHandler, worldInfo, dimType, profiler, listener, gen, env);
        this.delegate = world;
        this.borderListener = new IBorderListener.Impl(this.getWorldBorder());
        world.getWorldBorder().addListener(this.borderListener);
    }
}