package io.izzel.arclight.common.mixin.core.world.level.levelgen.structure.structures;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.StructurePieceBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import org.bukkit.entity.EntityType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetherFortressPieces.MonsterThrone.class)
public class NetherFortressPiecesPieceMixin {
    @Inject(method = "postProcess", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/level/levelgen/structure/structures/NetherFortressPieces$MonsterThrone;hasPlacedSpawner:Z"))
    private void arclight$customPlaceSpawner(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random, BoundingBox aabb, ChunkPos chunkIn, BlockPos blockIn, CallbackInfo ci) {
        ((StructurePieceBridge) this).bridge$placeCraftSpawner(level, blockIn, EntityType.CAVE_SPIDER, 2);
    }

    @Redirect(method = "postProcess", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$skipSetBlock(WorldGenLevel instance, BlockPos blockPos, BlockState blockState, int i) {
        return false;
    }

    @Redirect(method = "postProcess", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/WorldGenLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity arclight$getBlockEntity(WorldGenLevel instance, BlockPos blockPos) {
        return null;
    }
}
