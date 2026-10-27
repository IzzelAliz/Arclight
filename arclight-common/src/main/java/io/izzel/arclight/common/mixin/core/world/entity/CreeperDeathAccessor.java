package io.izzel.arclight.common.mixin.core.world.entity;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Creeper.class)
public interface CreeperDeathAccessor {
    @Accessor("droppedSkulls")
    void arclight$droppedSkulls(boolean dropped);
}
