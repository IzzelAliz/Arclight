package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BannerBlockEntity.class)
public abstract class BannerBlockEntityMixin extends BlockEntity {

    @Shadow private BannerPatternLayers patterns;

    public BannerBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void arclight$setPatterns(ValueInput input, CallbackInfo ci) {
        this.setPatterns(this.patterns);
    }

    @Inject(method = "applyImplicitComponents", at = @At("RETURN"))
    private void arclight$applyLimits(DataComponentGetter dataComponentInput, CallbackInfo ci) {
        this.setPatterns(this.patterns); // CraftBukkit - apply limits
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
