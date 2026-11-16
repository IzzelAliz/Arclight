package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PointedDripstoneBlock.class)
public class PointedDripstoneBlockMixin {

    @Redirect(method = "fallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;stalagmite()Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource arclight$blockDamage(DamageSources instance, Level level, BlockState blockState, BlockPos blockPos) {
        return ((DamageSourceBridge) instance.stalagmite()).bridge$directBlock(CraftBlock.at(level, blockPos));
    }
}
