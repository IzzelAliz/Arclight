package io.izzel.arclight.common.mixin.optimization.general;

import net.minecraft.entity.Entity;
import net.minecraft.world.TrackedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Set;

@Mixin(TrackedEntity.class)
public interface TrackedEntityAccessor {

    @Accessor void setPassengers(List<Entity> passengers);

}
