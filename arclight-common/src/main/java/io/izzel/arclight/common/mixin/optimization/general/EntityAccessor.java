package io.izzel.arclight.common.mixin.optimization.general;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(Entity.class)
public interface EntityAccessor {

    @Invoker void callGetRecursivePassengers(boolean playersOnly, Set<Entity> passengers);

}
