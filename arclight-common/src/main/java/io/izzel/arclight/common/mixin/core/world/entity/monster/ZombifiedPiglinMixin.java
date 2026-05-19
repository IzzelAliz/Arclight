package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.phys.AABB;
import org.bukkit.Bukkit;
import org.bukkit.entity.PigZombie;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PigZombieAngerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ZombifiedPiglin.class)
public abstract class ZombifiedPiglinMixin extends ZombieMixin {

    // @formatter:off
    @Shadow public abstract EntityReference<LivingEntity> getPersistentAngerTarget();
    @Shadow public abstract long getPersistentAngerEndTime();
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void alertOthers() {
        double d0 = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB axisalignedbb = AABB.unitCubeFromLowerCorner(this.position()).inflate(d0, 10.0D, d0);
        for (ZombifiedPiglin piglinEntity : this.level().getEntitiesOfClass(ZombifiedPiglin.class, axisalignedbb)) {
            if (piglinEntity != (Object) this) {
                if (piglinEntity.getTarget() == null) {
                    if (!piglinEntity.isAlliedTo(this.getTarget())) {
                        ((MobBridge) piglinEntity).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.TARGET_ATTACKED_NEARBY_ENTITY, true);
                        piglinEntity.setTarget(this.getTarget());
                    }
                }
            }
        }
    }

    @ModifyArg(method = "startPersistentAngerTimer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/zombie/ZombifiedPiglin;setTimeToRemainAngry(J)V"))
    private long arclight$pigAngry(long time) {
        EntityReference<LivingEntity> angerTarget = this.getPersistentAngerTarget();
        Entity entity = angerTarget == null ? null : EntityReference.get(angerTarget, this.level(), LivingEntity.class);
        PigZombieAngerEvent event = new PigZombieAngerEvent((PigZombie) this.getBukkitEntity(), entity == null ? null : ((EntityBridge) entity).bridge$getBukkitEntity(), (int) time);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return Math.max(0L, this.getPersistentAngerEndTime() - this.level().getGameTime());
        }
        return event.getNewAnger();
    }
}
