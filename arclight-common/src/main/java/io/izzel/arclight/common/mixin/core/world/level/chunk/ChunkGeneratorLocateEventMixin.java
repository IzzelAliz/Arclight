package io.izzel.arclight.common.mixin.core.world.level.chunk;

import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.world.StructuresLocateEvent;
import io.papermc.paper.util.MCUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.bukkit.craftbukkit.generator.structure.CraftStructure;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorLocateEventMixin {

    @Decorate(method = "findNearestMapStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/HolderSet;Lnet/minecraft/core/BlockPos;IZ)Lcom/mojang/datafixers/util/Pair;",
        inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getChunkSource()Lnet/minecraft/server/level/ServerChunkCache;"),
        require = 1, allow = 1)
    private void arclight$locateStructures(ServerLevel level, HolderSet<Structure> wantedStructures, BlockPos pos,
                                            int maxSearchRadius, boolean createReference) throws Throwable {
        // This call is after DEBUG_DISABLE_FEATURES, but before either placement search.
        var structures = wantedStructures.stream().map(CraftStructure::minecraftHolderToBukkit).toList();
        if (structures.isEmpty()) {
            return;
        }
        var world = ((ServerLevelBridge) level).bridge$getWorld();
        var event = new StructuresLocateEvent(world, CraftLocation.toBukkit(pos, world), structures, maxSearchRadius, createReference);
        if (!event.callEvent()) {
            DecorationOps.cancel().invoke((Pair<BlockPos, Holder<Structure>>) null);
            return;
        }
        var result = event.getResult();
        if (result != null) {
            Pair<BlockPos, Holder<Structure>> replacement = Pair.of(MCUtil.toBlockPos(result.pos()),
                CraftStructure.bukkitToMinecraftHolder(result.structure()));
            DecorationOps.cancel().invoke(replacement);
            return;
        }
        pos = CraftLocation.toBlockPos(event.getOrigin());
        maxSearchRadius = event.getRadius();
        createReference = event.shouldFindUnexplored();
        wantedStructures = HolderSet.direct(CraftStructure::bukkitToMinecraftHolder, event.getStructures());
        // Decorate maps these parameters to the actual host slots, not a copied request.
        DecorationOps.blackhole().invoke(wantedStructures, pos, maxSearchRadius, createReference);
    }
}
