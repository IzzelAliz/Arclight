package io.izzel.arclight.mixin.core.block;

import net.minecraft.block.MagmaBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_14_R1.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MagmaBlock.class)
public class MagmaBlockMixin {

    @Inject(method = "onEntityWalk", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    public void arclight$blockDamagePre(World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        CraftEventFactory.blockDamage = CraftBlock.at(worldIn, pos);
    }

    @Inject(method = "onEntityWalk", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    public void arclight$blockDamagePost(World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        CraftEventFactory.blockDamage = null;
    }
}
