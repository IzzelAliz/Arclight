package io.izzel.arclight.common.mixin.core.entity.item;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SMountEntityPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LeashKnotEntity.class)
public abstract class LeashKnotEntityMixin extends HangingEntityMixin {

    @Inject(method = "updateBoundingBox", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;chunkCheck(Lnet/minecraft/entity/Entity;)V"))
    private void arclight$checkIfValid(CallbackInfo ci) {
        if (!valid) ci.cancel();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @SuppressWarnings("ConstantConditions")
    @Overwrite
    public ActionResultType processInitialInteract(final PlayerEntity entityhuman, final Hand enumhand) {
        if (this.world.isRemote) {
            return ActionResultType.SUCCESS;
        }
        boolean flag = false;
        final double d0 = 7.0;
        final List<MobEntity> list = this.world.getEntitiesWithinAABB(MobEntity.class, new AxisAlignedBB(this.getPosX() - 7.0, this.getPosY() - 7.0, this.getPosZ() - 7.0, this.getPosX() + 7.0, this.getPosY() + 7.0, this.getPosZ() + 7.0));
        for (final MobEntity entityinsentient : list) {
            if (entityinsentient.getLeashHolder() == entityhuman) {
                if (CraftEventFactory.callPlayerLeashEntityEvent(entityinsentient, (LeashKnotEntity) (Object) this, entityhuman).isCancelled()) {
                    ((ServerPlayerEntity) entityhuman).connection.sendPacket(new SMountEntityPacket(entityinsentient, entityinsentient.getLeashHolder()));
                } else {
                    entityinsentient.setLeashHolder((LeashKnotEntity) (Object) this, true);
                    flag = true;
                }
            }
        }
        if (!flag) {
            boolean die = true;
            for (final MobEntity entityinsentient : list) {
                if (entityinsentient.getLeashed() && entityinsentient.getLeashHolder() == (Object) this) {
                    if (CraftEventFactory.callPlayerUnleashEntityEvent(entityinsentient, entityhuman).isCancelled()) {
                        die = false;
                    } else {
                        entityinsentient.clearLeashed(true, !entityhuman.abilities.isCreativeMode);
                    }
                }
            }
            if (die) {
                this.remove();
            }
        }
        return ActionResultType.CONSUME;
    }
}
