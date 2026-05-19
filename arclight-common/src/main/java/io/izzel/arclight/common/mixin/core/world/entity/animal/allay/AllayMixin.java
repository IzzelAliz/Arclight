package io.izzel.arclight.common.mixin.core.world.entity.animal.allay;

import io.izzel.arclight.common.mixin.core.world.entity.MobMixin;
import io.izzel.arclight.common.mod.mixins.annotation.RenameInto;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Allay.class)
public abstract class AllayMixin extends MobMixin {

    // @formatter:off
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_CAN_DUPLICATE;
    // @formatter:on

    public boolean forceDancing = false;

    public void setCanDuplicate(boolean canDuplicate) {
        this.entityData.set(DATA_CAN_DUPLICATE, canDuplicate);
    }

    @RenameInto("duplicateAllay")
    public Allay bukkit$duplicateAllay() {
        Allay allay = EntityType.ALLAY.create(this.level(), EntitySpawnReason.BREEDING);
        if (allay != null) {
            allay.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            allay.resetFallDistance();
            ((AllayMixin) (Object) allay).setDuplicationCooldown(6000L);
            this.setDuplicationCooldown(6000L);
            this.level().addFreshEntity(allay);
        }
        return allay;
    }

    @Shadow public abstract void setDuplicationCooldown(long duplicationCooldown);
}
