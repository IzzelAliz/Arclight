package io.izzel.arclight.impl.mixin.v1_14.world.chunk;

import io.izzel.arclight.common.bridge.world.chunk.ChunkBridge;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ITickList;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import org.bukkit.craftbukkit.v.CraftChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Chunk.class)
public abstract class ChunkMixin_1_14 implements ChunkBridge {

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;[Lnet/minecraft/world/biome/Biome;Lnet/minecraft/world/chunk/UpgradeData;Lnet/minecraft/world/ITickList;Lnet/minecraft/world/ITickList;J[Lnet/minecraft/world/chunk/ChunkSection;Ljava/util/function/Consumer;)V",
        at = @At("RETURN"))
    private void arclight$init(World worldIn, ChunkPos p_i49946_2_, Biome[] p_i49946_3_, UpgradeData p_i49946_4_, ITickList<Block> p_i49946_5_, ITickList<Fluid> p_i49946_6_, long p_i49946_7_, ChunkSection[] p_i49946_9_, Consumer<Chunk> p_i49946_10_, CallbackInfo ci) {
        bridge$setBukkitChunk(new CraftChunk((Chunk) (Object) this));
    }
}
