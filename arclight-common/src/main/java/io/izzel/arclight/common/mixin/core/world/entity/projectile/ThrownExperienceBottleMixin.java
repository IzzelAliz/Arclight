package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.HitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
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
        if (!this.level.isClientSide) {
            int i = 3 + this.level.random.nextInt(5) + this.level.random.nextInt(5);
            ExpBottleEvent event = CraftEventFactory.callExpBottleEvent((ThrownExperienceBottle) (Object) this, i);
            i = event.getExperience();
            if (event.getShowEffect()) {
                this.level.levelEvent(2002, this.blockPosition(), PotionUtils.getColor(Potions.WATER));
            }
            ExperienceOrb.award((ServerLevel) this.level, this.position(), i);
            this.discard();
        }
    }
}
