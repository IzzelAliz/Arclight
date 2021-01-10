package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import org.bukkit.craftbukkit.v.entity.CraftAbstractVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CraftAbstractVillager.class)
public class CraftAbstractVillagerMixin extends CraftEntityMixin {

    @Overwrite
    public AbstractVillagerEntity getHandle() {
        return (AbstractVillagerEntity) this.entity;
    }
}
