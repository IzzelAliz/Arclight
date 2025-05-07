package io.izzel.arclight.common.mixin.core.world.level.levelgen.structure.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidStructure;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.bukkit.craftbukkit.v.CraftLootTable;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.craftbukkit.v.block.CraftBrushableBlock;
import org.bukkit.craftbukkit.v.util.TransformerGeneratorAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DesertPyramidStructure.class)
public class DesertPyramidStructureMixin {

    @Inject(method = "placeSuspiciousSand", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static void arclight$customPlaceSuspiciousSand(BoundingBox aabb, WorldGenLevel level, BlockPos pos, CallbackInfo ci) {
        if (level instanceof TransformerGeneratorAccess craftLevel) {
            CraftBrushableBlock brushable = (CraftBrushableBlock) CraftBlockStates.getBlockState(level, pos, Blocks.SUSPICIOUS_SAND.defaultBlockState(), null);
            brushable.setLootTable(CraftLootTable.minecraftToBukkit(BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY));
            brushable.setSeed(pos.asLong());
            craftLevel.setCraftBlock(pos, brushable, 2);
            ci.cancel();
        }
    }
}
