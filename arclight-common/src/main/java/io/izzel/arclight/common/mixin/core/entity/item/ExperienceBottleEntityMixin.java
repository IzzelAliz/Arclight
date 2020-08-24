package io.izzel.arclight.common.mixin.core.entity.item;

import io.izzel.arclight.common.mixin.core.entity.projectile.ProjectileItemEntityMixin;
import net.minecraft.entity.item.ExperienceBottleEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.util.math.RayTraceResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.ExpBottleEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ExperienceBottleEntity.class)
public abstract class ExperienceBottleEntityMixin extends ProjectileItemEntityMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void onImpact(RayTraceResult result) {
        super.onImpact(result);
        if (!this.world.isRemote) {
            int i = 3 + this.world.rand.nextInt(5) + this.world.rand.nextInt(5);
            ExpBottleEvent event = CraftEventFactory.callExpBottleEvent((ExperienceBottleEntity) (Object) this, i);
            i = event.getExperience();
            if (event.getShowEffect()) {
                this.world.playEvent(2002, this.getPosition(), PotionUtils.getPotionColor(Potions.WATER));
            }
            while (i > 0) {
                int j = ExperienceOrbEntity.getXPSplit(i);
                i -= j;
                this.world.addEntity(new ExperienceOrbEntity(this.world, this.getPosX(), this.getPosY(), this.getPosZ(), j));
            }
            this.remove();
        }
    }
}
