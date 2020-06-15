package io.izzel.arclight.common.mixin.v1_15.entity.monster;

import io.izzel.arclight.common.bridge.entity.monster.EndermanEntityBridge;
import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.network.datasync.DataParameter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EndermanEntity.class)
public abstract class EndermanEntityMixin_1_15 extends CreatureEntityMixin implements EndermanEntityBridge {

    // @formatter:off
    @Shadow private int targetChangeTime;
    @Shadow @Final private static DataParameter<Boolean> SCREAMING;
    @Shadow @Final private static DataParameter<Boolean> field_226535_bx_;
    @Shadow @Final private static AttributeModifier ATTACKING_SPEED_BOOST;
    // @formatter:on

    @Override
    public void bridge$updateTarget(LivingEntity livingEntity) {
        IAttributeInstance iattributeinstance = this.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (livingEntity == null) {
            this.targetChangeTime = 0;
            this.dataManager.set(SCREAMING, false);
            this.dataManager.set(field_226535_bx_, false);
            iattributeinstance.removeModifier(ATTACKING_SPEED_BOOST);
        } else {
            this.targetChangeTime = this.ticksExisted;
            this.dataManager.set(SCREAMING, true);
            if (!iattributeinstance.hasModifier(ATTACKING_SPEED_BOOST)) {
                iattributeinstance.applyModifier(ATTACKING_SPEED_BOOST);
            }
        }
    }
}
