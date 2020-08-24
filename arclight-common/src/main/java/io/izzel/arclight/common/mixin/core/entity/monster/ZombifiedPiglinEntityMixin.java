package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.monster.ZombifiedPiglinEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.entity.PigZombie;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PigZombieAngerEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ZombifiedPiglinEntity.class)
public abstract class ZombifiedPiglinEntityMixin extends ZombieEntityMixin {

    // @formatter:off
    @Shadow public abstract UUID getAngerTarget();
    @Shadow public abstract void setAngerTarget(@Nullable UUID target);
    @Shadow public abstract void setAngerTime(int time);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void func_241411_fa_() {
        double d0 = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        AxisAlignedBB axisalignedbb = AxisAlignedBB.fromVector(this.getPositionVec()).grow(d0, 10.0D, d0);
        for (ZombifiedPiglinEntity piglinEntity : this.world.getLoadedEntitiesWithinAABB(ZombifiedPiglinEntity.class, axisalignedbb)) {
            if (piglinEntity != (Object) this) {
                if (piglinEntity.getAttackTarget() == null) {
                    if (!piglinEntity.isOnSameTeam(this.getAttackTarget())) {
                        ((MobEntityBridge) piglinEntity).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.TARGET_ATTACKED_NEARBY_ENTITY, true);
                        piglinEntity.setAttackTarget(this.getAttackTarget());
                    }
                }
            }
        }
    }

    @Eject(method = "func_230258_H__", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/ZombifiedPiglinEntity;setAngerTime(I)V"))
    private void arclight$pigAngry(ZombifiedPiglinEntity piglinEntity, int time, CallbackInfo ci) {
        Entity entity = ((ServerWorld) this.world).getEntityByUuid(this.getAngerTarget());
        PigZombieAngerEvent event = new PigZombieAngerEvent((PigZombie) this.getBukkitEntity(), entity == null ? null : ((EntityBridge) entity).bridge$getBukkitEntity(), time);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.setAngerTarget(null);
            ci.cancel();
        }
        this.setAngerTime(event.getNewAnger());
    }
}
