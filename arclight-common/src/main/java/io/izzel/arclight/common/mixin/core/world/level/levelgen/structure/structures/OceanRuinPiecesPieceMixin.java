package io.izzel.arclight.common.mixin.core.world.level.levelgen.structure.structures;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.structure.StructurePieceBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.OceanRuinPieces;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.bukkit.craftbukkit.v.CraftLootTable;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.craftbukkit.v.block.CraftChest;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OceanRuinPieces.OceanRuinPiece.class)
public class OceanRuinPiecesPieceMixin {

    @Shadow @Final private boolean isLarge;

    @Inject(method = "handleDataMarker", cancellable = true, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/ServerLevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void arclight$customSetLootChest(String s, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox aabb, CallbackInfo ci) {
        CraftChest chest = (CraftChest) CraftBlockStates.getBlockState(level, pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, level.getFluidState(pos).is(FluidTags.WATER)), null);
        chest.setSeed(random.nextLong());
        chest.setLootTable(CraftLootTable.minecraftToBukkit(isLarge ? BuiltInLootTables.UNDERWATER_RUIN_BIG : BuiltInLootTables.UNDERWATER_RUIN_SMALL));
        ((StructurePieceBridge) this).bridge$placeCraftBlockEntity(level, pos, chest, 2);
        ci.cancel();
    }
}
