package io.izzel.arclight.common.mixin.core.world.level.levelgen.structure.structures;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.StructurePieceBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EndCityPieces.EndCityPiece.class)
public class EndCityPiecesPieceMixin {
    @Redirect(method = "handleDataMarker", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/RandomizableContainer;setBlockEntityLootTable(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/resources/ResourceKey;)V"))
    private void arclight$customSetLootTable(BlockGetter level, RandomSource random, BlockPos pos, ResourceKey<LootTable> resourceKey) {
        ((StructurePieceBridge) this).bridge$setCraftLootTable((ServerLevelAccessor)  level, pos, random, resourceKey);
    }
}
