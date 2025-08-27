package io.izzel.arclight.common.mixin.core.world.level.levelgen.structure;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.StructurePieceBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.v.CraftLootTable;
import org.bukkit.craftbukkit.v.block.*;
import org.bukkit.craftbukkit.v.util.TransformerGeneratorAccess;
import org.bukkit.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(StructurePiece.class)
public abstract class StructurePieceMixin implements StructurePieceBridge {

    @Shadow protected abstract boolean canBeReplaced(LevelReader levelReader, int i, int j, int k, BoundingBox boundingBox);

    @Shadow private Mirror mirror;

    @Shadow private Rotation rotation;

    // Shift to after setBlock
    @Inject(method = "placeBlock", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void arclight$skipFluidIfPossible(WorldGenLevel level, BlockState p_73436_, int p_73437_, int p_73438_, int p_73439_, BoundingBox p_73440_, CallbackInfo ci, BlockPos pos) {
        if (level instanceof TransformerGeneratorAccess) {
            ci.cancel();
        }
    }

    @Inject(method = "createChest(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/block/state/BlockState;)Z", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    protected void arclight$bukkitCreateChest(ServerLevelAccessor level, BoundingBox aabb, RandomSource random, BlockPos pos, ResourceKey<LootTable> resourceKey, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        CraftBlockState maybeChest = (CraftBlockState) CraftBlockStates.getBlockState(level, pos, state, null);
        if (!(maybeChest instanceof CraftChest chest)) {
            // Bukkit won't need to process this
            return;
        }
        chest.setLootTable(CraftLootTable.minecraftToBukkit(resourceKey));
        chest.setSeed(random.nextLong());
        placeCraftBlockEntity(level, pos, chest, 2);
        cir.setReturnValue(true);
    }

    @Decorate(method = "createDispenser", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/StructurePiece;placeBlock(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/block/state/BlockState;IIILnet/minecraft/world/level/levelgen/structure/BoundingBox;)V"))
    protected void arclight$bukkitCreateDispenser(WorldGenLevel level, BoundingBox aabb, RandomSource random, int x, int y, int z, Direction direction, ResourceKey<LootTable> resourceKey, @Local(ordinal = -1) BlockPos mutablePos) throws Throwable {
        if (canBeReplaced(level, x, y, z, aabb)) {
            BlockState nmsDispenser = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, direction);
            if (this.mirror != Mirror.NONE) {
                nmsDispenser = nmsDispenser.mirror(this.mirror);
            }
            if (this.rotation != Rotation.NONE) {
                nmsDispenser = nmsDispenser.rotate(this.rotation);
            }

            CraftDispenser craftDispenser = (CraftDispenser) CraftBlockStates.getBlockState(level, mutablePos, nmsDispenser, null);
            craftDispenser.setLootTable(CraftLootTable.minecraftToBukkit(resourceKey));
            craftDispenser.setSeed(random.nextLong());
            placeCraftBlockEntity(level, mutablePos, craftDispenser, 2);
        }
        DecorationOps.cancel().invoke(true);
    }

    @Unique
    protected boolean placeCraftBlockEntity(ServerLevelAccessor worldAccess, BlockPos position, CraftBlockEntityState<?> craftBlockEntityState, int i) {
        if (worldAccess instanceof TransformerGeneratorAccess transformerAccess) {
            return transformerAccess.setCraftBlock(position, craftBlockEntityState, i);
        }
        boolean result = worldAccess.setBlock(position, craftBlockEntityState.getHandle(), i);
        var tileEntity = worldAccess.getBlockEntity(position);
        if (tileEntity != null) {
            tileEntity.loadWithComponents(craftBlockEntityState.getSnapshotNBT(), worldAccess.registryAccess());
        }
        return result;
    }

    @Unique
    protected void placeCraftSpawner(ServerLevelAccessor worldAccess, BlockPos position, org.bukkit.entity.EntityType entityType, int i) {
        // This method is used in structures that are generated by code and place spawners as they set the entity after the block was placed making it impossible for plugins to access that information
        var spawner = (CraftCreatureSpawner) CraftBlockStates.getBlockState(worldAccess, position, Blocks.SPAWNER.defaultBlockState(), null);
        spawner.setSpawnedType(entityType);
        placeCraftBlockEntity(worldAccess, position, spawner, i);
    }

    @Unique
    protected void setCraftLootTable(ServerLevelAccessor worldAccess, BlockPos position, RandomSource randomSource, ResourceKey<LootTable> loottableKey) {
        // This method is used in structures that use data markers to a loot table to loot containers as otherwise plugins won't have access to that information.
        var tileEntity = worldAccess.getBlockEntity(position);
        if (tileEntity instanceof RandomizableContainerBlockEntity tileEntityLootable) {
            tileEntityLootable.setLootTable(loottableKey, randomSource.nextLong());
            if (worldAccess instanceof TransformerGeneratorAccess transformerAccess) {
                transformerAccess.setCraftBlock(position, (CraftBlockState) CraftBlockStates.getBlockState(worldAccess, position, tileEntity.getBlockState(), tileEntityLootable.saveWithFullMetadata(worldAccess.registryAccess())), 3);
            }
        }
    }

    @Override
    public boolean bridge$placeCraftBlockEntity(ServerLevelAccessor worldAccess, BlockPos position, CraftBlockEntityState<?> craftBlockEntityState, int i) {
        return placeCraftBlockEntity(worldAccess, position, craftBlockEntityState, i);
    }

    @Override
    public boolean bridge$placeCraftSpawner(ServerLevelAccessor worldAccess, BlockPos position, EntityType entityType, int i) {
        var spawner = (CraftCreatureSpawner) CraftBlockStates.getBlockState(worldAccess, position, Blocks.SPAWNER.defaultBlockState(), null);
        spawner.setSpawnedType(entityType);
        return placeCraftBlockEntity(worldAccess, position, spawner, i);
    }

    @Override
    public void bridge$setCraftLootTable(ServerLevelAccessor worldAccess, BlockPos position, RandomSource randomSource, ResourceKey<LootTable> loottableKey) {
        setCraftLootTable(worldAccess, position, randomSource, loottableKey);
    }
}
