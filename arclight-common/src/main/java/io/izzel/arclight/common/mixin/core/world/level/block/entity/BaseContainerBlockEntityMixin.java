package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.block.LockableTileState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BaseContainerBlockEntity.class)
public abstract class BaseContainerBlockEntityMixin extends BlockEntityMixin implements IInventoryBridge, Container {

    @Decorate(method = "canOpen(Lnet/minecraft/world/entity/player/Player;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/LockCode;canUnlock(Lnet/minecraft/world/entity/player/Player;)Z"),
        require = 1, allow = 1)
    private boolean arclight$blockLockCheck(LockCode code, Player player) throws Throwable {
        BaseContainerBlockEntity container = (BaseContainerBlockEntity) (Object) this;
        // The Paper helper returns true outside its supported context; that must not unlock a mod container.
        // Inspect the registered state type without constructing a snapshot of an unknown mod block entity.
        if (player instanceof ServerPlayer && this.level != null
            && this.level.getBlockEntity(this.worldPosition) == container
            && LockableTileState.class.isAssignableFrom(CraftBlockStates.getBlockStateType(container.getType()))) {
            return CraftEventFactory.callBlockLockCheckEvent(container, code, container.getDisplayName(), player);
        }
        return (boolean) DecorationOps.callsite().invoke(code, player);
    }

    @Decorate(method = "sendChestLockedNotifications(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V",
        inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private static void arclight$blockLockNotifications(Vec3 pos, Player player, Component displayName) throws Throwable {
        // Here true means that the helper consumed the edited notification, not that opening was denied.
        if (CraftEventFactory.sendChestLockedNotifications(pos)) {
            DecorationOps.cancel().invoke();
            return;
        }
    }

    @Override
    public Location getLocation() {
        return CraftBlock.at(this.level, this.worldPosition).getLocation();
    }

    @Override
    public RecipeHolder<?> getCurrentRecipe() {
        return null;
    }

    @Override
    public void setCurrentRecipe(RecipeHolder<?> recipe) {
    }
}
