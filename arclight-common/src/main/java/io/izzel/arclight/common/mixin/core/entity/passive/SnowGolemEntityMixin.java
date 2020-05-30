package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;

@Mixin(SnowGolemEntity.class)
public abstract class SnowGolemEntityMixin extends CreatureEntityMixin {

    @Redirect(method = "livingTick", at = @At(value = "FIELD", target = "Lnet/minecraft/util/DamageSource;ON_FIRE:Lnet/minecraft/util/DamageSource;"))
    private DamageSource arclight$useMelting() {
        return CraftEventFactory.MELTING;
    }

    @Redirect(method = "livingTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private boolean arclight$blockForm(World world, BlockPos pos, BlockState state) {
        return CraftEventFactory.handleBlockFormEvent(world, pos, state, (SnowGolemEntity) (Object) this);
    }
}
