package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CraftHumanEntity.class)
public abstract class CraftHumanEntityMixin extends CraftEntity {

    // @formatter:off
    @Shadow(remap = false) public abstract Player getHandle();
    @Shadow(remap = false) public abstract void setHandle(Player entity);
    // @formatter:on

    public CraftHumanEntityMixin(CraftServer server, Entity entity) {
        super(server, entity);
    }

    @Decorate(method = "getOpenInventory", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;getBukkitView()Lorg/bukkit/inventory/InventoryView;"))
    private InventoryView arclight$capturePlayer(AbstractContainerMenu instance) throws Throwable {
        Player handle = getHandle();
        try {
            ArclightCaptures.captureContainerOwner(handle);
            return (InventoryView) DecorationOps.callsite().invoke(instance);
        } finally {
            ArclightCaptures.popContainerOwner(handle);
        }
    }

    @Override
    public void setHandle(Entity entity) {
        setHandle((Player) entity);
    }
}
