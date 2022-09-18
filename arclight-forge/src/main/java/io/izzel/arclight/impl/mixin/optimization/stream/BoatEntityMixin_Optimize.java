package io.izzel.arclight.impl.mixin.optimization.stream;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.monster.AbstractRaiderEntity;
import net.minecraft.entity.monster.RavagerEntity;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(BoatEntity.class)
public abstract class BoatEntityMixin_Optimize extends Entity {

    public BoatEntityMixin_Optimize(EntityType<?> entityTypeIn, World worldIn) {
        super(entityTypeIn, worldIn);
    }

    /**
     * @author danorris709
     * @reason `getPassengers` instantiates a new instance of the list when called
     */
    @Overwrite
    public Entity getControllingPassenger() {
        return this.passengers.isEmpty() ? null : this.passengers.get(0);
    }

    @Redirect(method = "Lnet/minecraft/entity/item/BoatEntity;tick()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/BoatEntity;getPassengers()Ljava/util/List;"))
    public List<Entity> arclight$getPassengers$tick(BoatEntity instance) {
        return this.passengers;
    }

    @Redirect(method = "Lnet/minecraft/entity/item/BoatEntity;updatePassenger(Lnet/minecraft/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/BoatEntity;getPassengers()Ljava/util/List;"))
    public List<Entity> arclight$getPassengers$updatePassenger(BoatEntity instance) {
        return this.passengers;
    }

    /**
     * @author danorris709
     * @reason `getPassengers` instantiates a new instance of the list when called
     */
    @Overwrite
    protected boolean canFitPassenger(Entity passenger) {
        return this.passengers.size() < 2 && !this.areEyesInFluid(FluidTags.WATER);
    }


}
