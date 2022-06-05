package io.izzel.arclight.common.mixin.optimization.general;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecraftEntityMixin_Optimize extends Entity {

    public AbstractMinecraftEntityMixin_Optimize(EntityType<?> entityTypeIn, World worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Redirect(method = "Lnet/minecraft/entity/item/minecart/AbstractMinecartEntity;moveAlongTrack(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/minecart/AbstractMinecartEntity;getPassengers()Ljava/util/List;"))
    public List<Entity> arclight$getPassengers$updatePassenger(AbstractMinecartEntity instance) {
        return this.passengers;
    }
}
