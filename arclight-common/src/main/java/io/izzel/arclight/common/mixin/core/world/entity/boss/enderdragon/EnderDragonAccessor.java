package io.izzel.arclight.common.mixin.core.world.entity.boss.enderdragon;

import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderDragon.class)
public interface EnderDragonAccessor {

    @Accessor("subEntities")
    EnderDragonPart[] arclight$getSubEntities();
}
