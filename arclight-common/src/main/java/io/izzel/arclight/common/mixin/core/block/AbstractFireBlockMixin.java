package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Blocks;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFireBlock.class)
public class AbstractFireBlockMixin {

    @Redirect(method = "onBlockAdded", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    public boolean arclight$extinguish2(World world, BlockPos pos, boolean isMoving) {
        if (!CraftEventFactory.callBlockFadeEvent(world, pos, Blocks.AIR.getDefaultState()).isCancelled()) {
            world.removeBlock(pos, isMoving);
        }
        return false;
    }

    @Inject(method = "canLightPortal", cancellable = true, at = @At("RETURN"))
    private static void arclight$lightPortal(World world, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            RegistryKey<DimensionType> typeKey = ((WorldBridge) world).bridge$getTypeKey();
            cir.setReturnValue(typeKey == DimensionType.OVERWORLD || typeKey == DimensionType.THE_NETHER);
        }
    }
}
