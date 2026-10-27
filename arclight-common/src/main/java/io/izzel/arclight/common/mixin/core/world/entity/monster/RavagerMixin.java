package io.izzel.arclight.common.mixin.core.world.entity.monster;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Ravager.class)
public abstract class RavagerMixin extends PathfinderMobMixin {

    @Decorate(method = "aiStep()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/Ravager;jumpFromGround()V"), require = 1, allow = 1)
    private void arclight$entityJump(Ravager ravager) throws Throwable {
        if (this.level().isClientSide() || new EntityJumpEvent(this.getBukkitEntity()).callEvent()) {
            DecorationOps.callsite().invoke(ravager);
        } else {
            ravager.setJumping(false);
        }
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$entityChangeBlock(ServerLevel world, BlockPos pos, boolean dropBlock, Entity entityIn) {
        return CraftEventFactory.callEntityChangeBlockEvent((Ravager) (Object) this, pos, Blocks.AIR.defaultBlockState())
            && world.destroyBlock(pos, dropBlock, entityIn);
    }
}
