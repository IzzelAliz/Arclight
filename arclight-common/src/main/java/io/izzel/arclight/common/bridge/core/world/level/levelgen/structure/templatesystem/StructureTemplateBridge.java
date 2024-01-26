package io.izzel.arclight.common.bridge.core.world.level.levelgen.structure.templatesystem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;
import java.util.List;

public interface StructureTemplateBridge {

    List<StructureTemplate.StructureBlockInfo> bridge$platform$processBlockInfos(ServerLevelAccessor arg, BlockPos arg2, BlockPos arg3, StructurePlaceSettings arg4, List<StructureTemplate.StructureBlockInfo> list2, @Nullable StructureTemplate template);

    void bridge$platform$placeEntities(ServerLevelAccessor serverLevelAccessor, BlockPos blockPos, Mirror mirror, Rotation rotation, BlockPos blockPos2, @org.jetbrains.annotations.Nullable BoundingBox boundingBox, boolean bl, StructurePlaceSettings placementIn);
}
