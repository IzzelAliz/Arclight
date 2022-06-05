package io.izzel.arclight.common.mixin.optimization.general;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PigEntity.class)
public abstract class PigEntityMixin_Optimize extends AnimalEntity {

    protected PigEntityMixin_Optimize(EntityType<? extends AnimalEntity> type, World worldIn) {
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
