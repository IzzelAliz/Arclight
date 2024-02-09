package io.izzel.arclight.neoforge.mixin.core.world.level.levelgen.structure.templatesystem;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.structure.templatesystem.StructureTemplateBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin_NeoForge implements StructureTemplateBridge {

    // @formatter:off
    @Shadow(remap = false) public static List<StructureTemplate.StructureBlockInfo> processBlockInfos(ServerLevelAccessor p_278297_, BlockPos p_74519_, BlockPos p_74520_, StructurePlaceSettings p_74521_, List<StructureTemplate.StructureBlockInfo> p_74522_, @Nullable StructureTemplate template) { return null; }
    @Shadow(remap = false) protected abstract void addEntitiesToWorld(ServerLevelAccessor par1, BlockPos par2, StructurePlaceSettings par3);
    // @formatter:on

    @Override
    public List<StructureTemplate.StructureBlockInfo> bridge$platform$processBlockInfos(ServerLevelAccessor arg, BlockPos arg2, BlockPos arg3, StructurePlaceSettings arg4, List<StructureTemplate.StructureBlockInfo> list2, @Nullable StructureTemplate template) {
        return processBlockInfos(arg, arg2, arg3, arg4, list2, template);
    }

    @Override
    public void bridge$platform$placeEntities(ServerLevelAccessor serverLevelAccessor, BlockPos blockPos, Mirror mirror, Rotation rotation, BlockPos blockPos2, @Nullable BoundingBox boundingBox, boolean bl, StructurePlaceSettings placementIn) {
        this.addEntitiesToWorld(serverLevelAccessor, blockPos, placementIn);
    }
}
