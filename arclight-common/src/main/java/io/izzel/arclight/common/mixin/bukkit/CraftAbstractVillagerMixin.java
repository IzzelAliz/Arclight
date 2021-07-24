package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.world.entity.npc.AbstractVillager;
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
    public AbstractVillager getHandle() {
        return (AbstractVillager) this.entity;
    }
}
