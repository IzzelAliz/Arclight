package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ThrownExperienceBottle.class)
public abstract class ThrownExperienceBottleMixin extends ThrowableItemProjectileMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            int i = 3 + this.level().random.nextInt(5) + this.level().random.nextInt(5);
            ExpBottleEvent event = CraftEventFactory.callExpBottleEvent((ThrownExperienceBottle) (Object) this, result, i);
            i = event.getExperience();
            if (event.getShowEffect()) {
                this.level().levelEvent(2002, this.blockPosition(), new PotionContents(Potions.WATER).getColor());
            }
            if (result instanceof BlockHitResult blockHitResult) {
                Vec3 direction = blockHitResult.getDirection().getUnitVec3();
                ExperienceOrb.awardWithDirection((ServerLevel) this.level(), result.getLocation(), direction, i);
            } else {
                ExperienceOrb.awardWithDirection((ServerLevel) this.level(), result.getLocation(), this.getDeltaMovement().scale(-1.0D), i);
            }
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
            this.discard();
        }
    }
}
