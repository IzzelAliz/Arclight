package io.izzel.arclight.common.mixin.vanilla.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.bridge.core.world.level.levelgen.structure.templatesystem.StructureTemplateBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.bukkit.craftbukkit.v.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.craftbukkit.v.block.CraftLootable;
import org.bukkit.craftbukkit.v.util.CraftStructureTransformer;
import org.bukkit.craftbukkit.v.util.TransformerGeneratorAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;

@Mixin(value = StructureTemplate.class)
public abstract class StructureTemplateMixin_Vanilla implements StructureTemplateBridge {

    // @formatter:off
    @Shadow protected abstract void placeEntities(ServerLevelAccessor serverLevelAccessor, BlockPos blockPos, Mirror mirror, Rotation rotation, BlockPos blockPos2, @Nullable BoundingBox boundingBox, boolean bl);
    @Shadow @Final public List<StructureTemplate.Palette> palettes;
    @Shadow @Final public List<StructureTemplate.StructureEntityInfo> entityInfoList;
    @Shadow private Vec3i size;
    @Shadow public static void updateShapeAtEdge(LevelAccessor p_74511_, int p_74512_, DiscreteVoxelShape p_74513_, int p_74514_, int p_74515_, int p_74516_) { }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean placeInWorld(ServerLevelAccessor p_230329_, BlockPos pos, BlockPos p_230331_, StructurePlaceSettings placeSettings, RandomSource p_230333_, int p_230334_) {
        if (this.palettes.isEmpty()) {
            return false;
        } else {
            // CraftBukkit start
            // We only want the TransformerGeneratorAccess at certain locations because in here are many "block update" calls that shouldn't be transformed
            var wrappedAccess = p_230329_;
            CraftStructureTransformer structureTransformer = null;
            if (wrappedAccess instanceof TransformerGeneratorAccess transformerAccess) {
                p_230329_ = transformerAccess.getHandle();
                structureTransformer = transformerAccess.getStructureTransformer();
                // The structureTransformer is not needed if we can not transform blocks therefore we can save a little bit of performance doing this
                if (structureTransformer != null && !structureTransformer.canTransformBlocks()) {
                    structureTransformer = null;
                }
            }
            // CraftBukkit end
            List<StructureTemplate.StructureBlockInfo> list = placeSettings.getRandomPalette(this.palettes, pos).blocks();
            if ((!list.isEmpty() || !placeSettings.isIgnoreEntities() && !this.entityInfoList.isEmpty()) && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1) {
                BoundingBox boundingbox = placeSettings.getBoundingBox();
                List<BlockPos> list1 = Lists.newArrayListWithCapacity(placeSettings.shouldKeepLiquids() ? list.size() : 0);
                List<BlockPos> list2 = Lists.newArrayListWithCapacity(placeSettings.shouldKeepLiquids() ? list.size() : 0);
                List<Pair<BlockPos, CompoundTag>> list3 = Lists.newArrayListWithCapacity(list.size());
                int i = Integer.MAX_VALUE;
                int j = Integer.MAX_VALUE;
                int k = Integer.MAX_VALUE;
                int l = Integer.MIN_VALUE;
                int i1 = Integer.MIN_VALUE;
                int j1 = Integer.MIN_VALUE;

                for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : StructureTemplate.processBlockInfos(p_230329_, pos, p_230331_, placeSettings, list)) {
                    BlockPos blockpos = structuretemplate$structureblockinfo.pos();
                    if (boundingbox == null || boundingbox.isInside(blockpos)) {
                        FluidState fluidstate = placeSettings.shouldKeepLiquids() ? p_230329_.getFluidState(blockpos) : null;
                        BlockState blockstate = structuretemplate$structureblockinfo.state().mirror(placeSettings.getMirror()).rotate(placeSettings.getRotation());
                        if (structuretemplate$structureblockinfo.nbt() != null) {
                            BlockEntity blockentity = p_230329_.getBlockEntity(blockpos);
                            Clearable.tryClear(blockentity);
                            p_230329_.setBlock(blockpos, Blocks.BARRIER.defaultBlockState(), 20);
                        }
                        // CraftBukkit start
                        if (structureTransformer != null) {
                            var craftBlockState = (CraftBlockState) CraftBlockStates.getBlockState(blockpos, blockstate, null);
                            if (structuretemplate$structureblockinfo.nbt() != null && craftBlockState instanceof CraftBlockEntityState<?> entityState) {
                                entityState.loadData(structuretemplate$structureblockinfo.nbt());
                                if (craftBlockState instanceof CraftLootable<?> craftLootable) {
                                    craftLootable.setSeed(p_230333_.nextLong());
                                }
                            }
                            craftBlockState = structureTransformer.transformCraftState(craftBlockState);
                            blockstate = craftBlockState.getHandle();
                            structuretemplate$structureblockinfo = new StructureTemplate.StructureBlockInfo(blockpos, blockstate, (craftBlockState instanceof CraftBlockEntityState<?> craftBlockEntityState ? craftBlockEntityState.getSnapshotNBT() : null));
                        }
                        // CraftBukkit end

                        if (p_230329_.setBlock(blockpos, blockstate, p_230334_)) {
                            i = Math.min(i, blockpos.getX());
                            j = Math.min(j, blockpos.getY());
                            k = Math.min(k, blockpos.getZ());
                            l = Math.max(l, blockpos.getX());
                            i1 = Math.max(i1, blockpos.getY());
                            j1 = Math.max(j1, blockpos.getZ());
                            list3.add(Pair.of(blockpos, structuretemplate$structureblockinfo.nbt()));
                            if (structuretemplate$structureblockinfo.nbt() != null) {
                                BlockEntity blockentity1 = p_230329_.getBlockEntity(blockpos);
                                if (blockentity1 != null) {
                                    if (structureTransformer == null && blockentity1 instanceof RandomizableContainerBlockEntity) {
                                        structuretemplate$structureblockinfo.nbt().putLong("LootTableSeed", p_230333_.nextLong());
                                    }

                                    blockentity1.load(structuretemplate$structureblockinfo.nbt());
                                }
                            }

                            if (fluidstate != null) {
                                if (blockstate.getFluidState().isSource()) {
                                    list2.add(blockpos);
                                } else if (blockstate.getBlock() instanceof LiquidBlockContainer) {
                                    ((LiquidBlockContainer) blockstate.getBlock()).placeLiquid(p_230329_, blockpos, blockstate, fluidstate);
                                    if (!fluidstate.isSource()) {
                                        list1.add(blockpos);
                                    }
                                }
                            }
                        }
                    }
                }

                boolean flag = true;
                Direction[] adirection = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

                while (flag && !list1.isEmpty()) {
                    flag = false;
                    Iterator<BlockPos> iterator = list1.iterator();

                    while (iterator.hasNext()) {
                        BlockPos blockpos3 = iterator.next();
                        FluidState fluidstate2 = p_230329_.getFluidState(blockpos3);

                        for (int i2 = 0; i2 < adirection.length && !fluidstate2.isSource(); ++i2) {
                            BlockPos blockpos1 = blockpos3.relative(adirection[i2]);
                            FluidState fluidstate1 = p_230329_.getFluidState(blockpos1);
                            if (fluidstate1.isSource() && !list2.contains(blockpos1)) {
                                fluidstate2 = fluidstate1;
                            }
                        }

                        if (fluidstate2.isSource()) {
                            BlockState blockstate1 = p_230329_.getBlockState(blockpos3);
                            Block block = blockstate1.getBlock();
                            if (block instanceof LiquidBlockContainer) {
                                ((LiquidBlockContainer) block).placeLiquid(p_230329_, blockpos3, blockstate1, fluidstate2);
                                flag = true;
                                iterator.remove();
                            }
                        }
                    }
                }

                if (i <= l) {
                    if (!placeSettings.getKnownShape()) {
                        DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(l - i + 1, i1 - j + 1, j1 - k + 1);
                        int k1 = i;
                        int l1 = j;
                        int j2 = k;

                        for (Pair<BlockPos, CompoundTag> pair1 : list3) {
                            BlockPos blockpos2 = pair1.getFirst();
                            discretevoxelshape.fill(blockpos2.getX() - k1, blockpos2.getY() - l1, blockpos2.getZ() - j2);
                        }

                        updateShapeAtEdge(p_230329_, p_230334_, discretevoxelshape, k1, l1, j2);
                    }

                    for (Pair<BlockPos, CompoundTag> pair : list3) {
                        BlockPos blockpos4 = pair.getFirst();
                        if (!placeSettings.getKnownShape()) {
                            BlockState blockstate2 = p_230329_.getBlockState(blockpos4);
                            BlockState blockstate3 = Block.updateFromNeighbourShapes(blockstate2, p_230329_, blockpos4);
                            if (blockstate2 != blockstate3) {
                                p_230329_.setBlock(blockpos4, blockstate3, p_230334_ & -2 | 16);
                            }

                            p_230329_.blockUpdated(blockpos4, blockstate3.getBlock());
                        }

                        if (pair.getSecond() != null) {
                            BlockEntity blockentity2 = p_230329_.getBlockEntity(blockpos4);
                            if (blockentity2 != null) {
                                blockentity2.setChanged();
                            }
                        }
                    }
                }

                if (!placeSettings.isIgnoreEntities()) {
                    this.placeEntities(wrappedAccess, pos, placeSettings.getMirror(), placeSettings.getRotation(), placeSettings.getRotationPivot(), placeSettings.getBoundingBox(), placeSettings.shouldFinalizeEntities());
                }

                return true;
            } else {
                return false;
            }
        }
    }
}
