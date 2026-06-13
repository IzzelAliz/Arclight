package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.mixin.Decorate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(BannerBlockEntity.class)
public abstract class BannerBlockEntityMixin extends BlockEntity {

    @Shadow private BannerPatternLayers patterns;

    public BannerBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Decorate(method = {"method_58121", "lambda$loadAdditional$1"}, inject = true, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/BannerBlockEntity;patterns:Lnet/minecraft/world/level/block/entity/BannerPatternLayers;", opcode = Opcodes.PUTFIELD), require = 1)
    private void arclight$setPatterns(BannerPatternLayers layers) {
        this.setPatterns(layers);
    }

    @Decorate(method = "applyImplicitComponents", inject = true, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/BannerBlockEntity;patterns:Lnet/minecraft/world/level/block/entity/BannerPatternLayers;", opcode = Opcodes.PUTFIELD))
    private void arclight$applyLimits(DataComponentGetter dataComponentGetter) {
        this.setPatterns((BannerPatternLayers) dataComponentGetter.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)); // CraftBukkit - apply limits
    }

    // CraftBukkit start
    public void setPatterns(BannerPatternLayers bannerpatternlayers) {
        if (bannerpatternlayers.layers().size() > 20) {
            bannerpatternlayers = new BannerPatternLayers(List.copyOf(bannerpatternlayers.layers().subList(0, 20)));
        }
        this.patterns = bannerpatternlayers;
    }
    // CraftBukkit end
}
