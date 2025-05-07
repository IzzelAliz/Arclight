package io.izzel.arclight.common.mixin.core.world.level.levelgen.structure.structures;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.StructurePieceBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.IglooPieces;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IglooPieces.IglooPiece.class)
public class IglooPiecesPieceMixin {
    @Inject(method = "handleDataMarker", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private void arclight$customSetLootTable(String string, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox aabb, CallbackInfo ci) {
        ((StructurePieceBridge) this).bridge$setCraftLootTable(level, pos.below(), random, BuiltInLootTables.IGLOO_CHEST);
    }
}
