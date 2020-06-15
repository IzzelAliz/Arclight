package io.izzel.arclight.impl.mixin.v1_14.core.entity.monster;

import io.izzel.arclight.impl.mixin.v1_14.core.entity.MobEntityMixin_1_14;
import net.minecraft.block.Blocks;
import net.minecraft.entity.monster.RavagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RavagerEntity.class)
public abstract class RavagerEntityMixin_1_14 extends MobEntityMixin_1_14 {

    @Redirect(method = "livingTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;destroyBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    private boolean arclight$entityChangeBlock(World world, BlockPos pos, boolean dropBlock) {
        return !CraftEventFactory.callEntityChangeBlockEvent((RavagerEntity) (Object) this, pos, Blocks.AIR.getDefaultState()).isCancelled()
            && world.destroyBlock(pos, dropBlock);
    }
}
