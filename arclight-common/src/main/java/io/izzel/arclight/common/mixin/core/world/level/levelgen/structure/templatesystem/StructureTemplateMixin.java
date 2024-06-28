package io.izzel.arclight.common.mixin.core.world.level.levelgen.structure.templatesystem;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.structure.templatesystem.StructureTemplateBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.bukkit.craftbukkit.v.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.craftbukkit.v.block.CraftLootable;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataTypeRegistry;
import org.bukkit.craftbukkit.v.util.CraftStructureTransformer;
import org.bukkit.craftbukkit.v.util.TransformerGeneratorAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin implements StructureTemplateBridge {

    private static final CraftPersistentDataTypeRegistry DATA_TYPE_REGISTRY = new CraftPersistentDataTypeRegistry();
    public CraftPersistentDataContainer persistentDataContainer = new CraftPersistentDataContainer(DATA_TYPE_REGISTRY);

    @Inject(method = "save", at = @At("RETURN"))
    private void arclight$savePdc(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (!this.persistentDataContainer.isEmpty()) {
            tag.put("BukkitValues", this.persistentDataContainer.toTagCompound());
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void arclight$loadPdc(HolderGetter<Block> reg, CompoundTag tag, CallbackInfo ci) {
        var base = tag.get("BukkitValues");
        if (base instanceof CompoundTag compoundTag) {
            this.persistentDataContainer.putAll(compoundTag);
        }
    }

    @Decorate(method = "placeInWorld", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;getRandomPalette(Ljava/util/List;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$Palette;"))
    private void arclight$unwrap(ServerLevelAccessor serverLevelAccessor, BlockPos blockPos, BlockPos blockPos2, StructurePlaceSettings structurePlaceSettings, RandomSource randomSource, int i,
                                 @Local(allocate = "wrappedAccess") ServerLevelAccessor wrappedAccess,
                                 @Local(allocate = "structureTransformer") CraftStructureTransformer structureTransformer) throws Throwable {
        wrappedAccess = serverLevelAccessor;
        structureTransformer = null;
        if (wrappedAccess instanceof TransformerGeneratorAccess transformerAccess) {
            serverLevelAccessor = transformerAccess.getHandle();
            structureTransformer = transformerAccess.getStructureTransformer();
            // The structureTransformer is not needed if we can not transform blocks therefore we can save a little bit of performance doing this
            if (structureTransformer != null && !structureTransformer.canTransformBlocks()) {
                structureTransformer = null;
            }
        }
        DecorationOps.blackhole().invoke(serverLevelAccessor, wrappedAccess, structureTransformer);
    }

    @Decorate(method = "placeInWorld", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/level/ServerLevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$useTransformer(ServerLevelAccessor instance, BlockPos pos, BlockState blockState, int i,
                                            @Local(ordinal = -1) RandomSource randomSource,
                                            @Local(ordinal = -1) StructureTemplate.StructureBlockInfo structureBlockInfo,
                                            @Local(ordinal = -1) BlockState blockstate,
                                            @Local(allocate = "structureTransformer") CraftStructureTransformer structureTransformer) throws Throwable {
        if (structureTransformer != null) {
            var craftBlockState = (CraftBlockState) CraftBlockStates.getBlockState(instance, pos, blockstate, null);
            if (structureBlockInfo.nbt() != null && craftBlockState instanceof CraftBlockEntityState<?> entityState) {
                entityState.loadData(structureBlockInfo.nbt());
                if (craftBlockState instanceof CraftLootable<?> craftLootable) {
                    craftLootable.setSeed(randomSource.nextLong());
                }
            }
            craftBlockState = structureTransformer.transformCraftState(craftBlockState);
            blockState = craftBlockState.getHandle();
            blockstate = blockState;
            structureBlockInfo = new StructureTemplate.StructureBlockInfo(pos, blockState, (craftBlockState instanceof CraftBlockEntityState<?> craftBlockEntityState ? craftBlockEntityState.getSnapshotNBT() : null));
        }
        DecorationOps.blackhole().invoke(structureBlockInfo, blockstate);
        return (boolean) DecorationOps.callsite().invoke(instance, pos, blockState, i);
    }

    @Decorate(method = "placeInWorld", inject = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;isIgnoreEntities()Z"))
    private void arclight$resetWrap(ServerLevelAccessor serverLevelAccessor, BlockPos blockPos, BlockPos blockPos2, StructurePlaceSettings structurePlaceSettings, RandomSource randomSource, int i,
                                    @Local(allocate = "wrappedAccess") ServerLevelAccessor wrappedAccess) throws Throwable {
        serverLevelAccessor = wrappedAccess;
        DecorationOps.blackhole().invoke(serverLevelAccessor);
    }
}
