package io.izzel.arclight.common.mixin.core.world.level.levelgen.structure.structures;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.StructurePieceBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.structures.IglooPieces;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(IglooPieces.IglooPiece.class)
public class IglooPiecesPieceMixin {
    @Redirect(method = "handleDataMarker", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/ChestBlockEntity;setLootTable(Lnet/minecraft/resources/ResourceKey;J)V"))
    private void arclight$customSetLootTable(ChestBlockEntity instance, ResourceKey<LootTable> resourceKey, long l, String string, BlockPos pos, ServerLevelAccessor level, RandomSource random) {
        ((StructurePieceBridge) this).bridge$setCraftLootTable(level, pos.below(), random, resourceKey);
    }
}
