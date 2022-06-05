package io.izzel.arclight.common.mixin.optimization.general;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.monster.AbstractRaiderEntity;
import net.minecraft.entity.monster.RavagerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(RavagerEntity.class)
public abstract class RavagerEntityMixin_Optimize extends AbstractRaiderEntity {

    protected RavagerEntityMixin_Optimize(EntityType<? extends AbstractRaiderEntity> type, World worldIn) {
        super(type, worldIn);
    }

    /**
     * @author danorris709
     * @reason `getPassengers` instantiates a new instance of the list when called
     */
    @Overwrite
    public Entity getControllingPassenger() {
        return this.passengers.isEmpty() ? null : this.passengers.get(0);
    }
}
