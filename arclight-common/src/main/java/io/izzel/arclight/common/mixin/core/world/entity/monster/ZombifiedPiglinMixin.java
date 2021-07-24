package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.phys.AABB;
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

@Mixin(ZombifiedPiglin.class)
public abstract class ZombifiedPiglinMixin extends ZombieMixin {

    // @formatter:off
    @Shadow public abstract UUID getPersistentAngerTarget();
    @Shadow public abstract void setPersistentAngerTarget(@Nullable UUID target);
    @Shadow public abstract void setRemainingPersistentAngerTime(int time);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void alertOthers() {
        double d0 = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB axisalignedbb = AABB.unitCubeFromLowerCorner(this.position()).inflate(d0, 10.0D, d0);
        for (ZombifiedPiglin piglinEntity : this.level.getEntitiesOfClass(ZombifiedPiglin.class, axisalignedbb)) {
            if (piglinEntity != (Object) this) {
                if (piglinEntity.getTarget() == null) {
                    if (!piglinEntity.isAlliedTo(this.getTarget())) {
                        ((MobEntityBridge) piglinEntity).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.TARGET_ATTACKED_NEARBY_ENTITY, true);
                        piglinEntity.setTarget(this.getTarget());
                    }
                }
            }
        }
    }

    @Eject(method = "func_230258_H__", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/ZombifiedPiglinEntity;setAngerTime(I)V"))
    private void arclight$pigAngry(ZombifiedPiglin piglinEntity, int time, CallbackInfo ci) {
        Entity entity = ((ServerLevel) this.level).getEntity(this.getPersistentAngerTarget());
        PigZombieAngerEvent event = new PigZombieAngerEvent((PigZombie) this.getBukkitEntity(), entity == null ? null : ((EntityBridge) entity).bridge$getBukkitEntity(), time);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.setPersistentAngerTarget(null);
            ci.cancel();
        }
        this.setRemainingPersistentAngerTime(event.getNewAnger());
    }
}
