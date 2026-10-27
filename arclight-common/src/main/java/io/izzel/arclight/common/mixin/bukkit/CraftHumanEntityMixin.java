package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftHumanEntity.class)
public abstract class CraftHumanEntityMixin extends CraftEntity {

    // @formatter:off
    @Shadow(remap = false) public abstract Player getHandle();
    @Shadow(remap = false) public abstract void setHandle(Player entity);
    // @formatter:on

    public CraftHumanEntityMixin(CraftServer server, Entity entity) {
        super(server, entity);
    }

    @Override
    public void setHandle(Entity entity) {
        setHandle((Player) entity);
    }
}
