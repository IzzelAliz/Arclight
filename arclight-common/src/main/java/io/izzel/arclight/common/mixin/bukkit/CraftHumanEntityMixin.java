package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftHumanEntity.class, remap = false)
public abstract class CraftHumanEntityMixin extends CraftEntity {

    // @formatter:off
    @Shadow private CraftInventoryPlayer inventory;
    // @formatter:on

    public CraftHumanEntityMixin(CraftServer server, Entity entity) {
        super(server, entity);
    }

    @Override
    public void setHandle(Entity entity) {
        super.setHandle(entity);
        this.inventory = new CraftInventoryPlayer(((PlayerEntity) entity).inventory);
    }
}
