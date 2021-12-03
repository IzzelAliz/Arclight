package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.entity.monster.EndermanEntityBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(EnderMan.class)
public abstract class EnderManMixin extends PathfinderMobMixin implements EndermanEntityBridge {

    // @formatter:off
    @Shadow private int targetChangeTime;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_CREEPY;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_STARED_AT;
    @Shadow @Final private static AttributeModifier SPEED_MODIFIER_ATTACKING;
    // @formatter:on

    @Override
    public void bridge$updateTarget(LivingEntity livingEntity) {
        AttributeInstance modifiableattributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (livingEntity == null) {
            this.targetChangeTime = 0;
            this.entityData.set(DATA_CREEPY, false);
            this.entityData.set(DATA_STARED_AT, false);
            modifiableattributeinstance.removeModifier(SPEED_MODIFIER_ATTACKING);
        } else {
            this.targetChangeTime = this.tickCount;
            this.entityData.set(DATA_CREEPY, true);
            if (!modifiableattributeinstance.hasModifier(SPEED_MODIFIER_ATTACKING)) {
                modifiableattributeinstance.addTransientModifier(SPEED_MODIFIER_ATTACKING);
            }
        }
    }

    @Override
    public boolean setTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (!super.setTarget(livingEntity, reason, fireEvent)) {
            return false;
        }
        bridge$updateTarget(getTarget());
        return true;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void setTarget(@Nullable LivingEntity entity) {
        this.bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.CLOSEST_PLAYER, true);
        super.setTarget(entity);
        if (arclight$targetSuccess) {
            bridge$updateTarget(getTarget());
        }
    }
}
