package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.MobEntityBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
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

import java.util.UUID;

@Mixin(ZombifiedPiglin.class)
public abstract class ZombifiedPiglinMixin extends ZombieMixin {

    // @formatter:off
    @Shadow public abstract UUID getPersistentAngerTarget();
    @Shadow public abstract int getRemainingPersistentAngerTime();
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

    @ModifyArg(method = "startPersistentAngerTimer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/ZombifiedPiglin;setRemainingPersistentAngerTime(I)V"))
    private int arclight$pigAngry(int time) {
        Entity entity = ((ServerLevel) this.level).getEntity(this.getPersistentAngerTarget());
        PigZombieAngerEvent event = new PigZombieAngerEvent((PigZombie) this.getBukkitEntity(), entity == null ? null : ((EntityBridge) entity).bridge$getBukkitEntity(), time);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return this.getRemainingPersistentAngerTime();
        }
        return event.getNewAnger();
    }
}
