package io.izzel.arclight.forge.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.izzel.arclight.common.mod.util.TreeCapture;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.NetherFungusBlock;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NetherFungusBlock.class)
public class NetherFungusBlockMixin_Forge {

    // Observe Forge's replacement after denial, before native Holder.value and placement.
    @ModifyExpressionValue(method = "lambda$performBonemeal$1(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/level/BlockFeatureGrowEvent;getFeature()Lnet/minecraft/core/Holder;", remap = false),
        require = 1, expect = 1, allow = 1)
    private static Holder<Feature> arclight$finalHolder(Holder<Feature> holder) {
        TreeCapture.selectGrowth(holder, false);
        return holder;
    }
}
