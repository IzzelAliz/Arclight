package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import org.bukkit.craftbukkit.v.entity.CraftAbstractVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = CraftAbstractVillager.class, remap = false)
public abstract class CraftAbstractVillagerMixin extends CraftEntityMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public AbstractVillagerEntity getHandle() {
        return (AbstractVillagerEntity) this.entity;
    }
}
