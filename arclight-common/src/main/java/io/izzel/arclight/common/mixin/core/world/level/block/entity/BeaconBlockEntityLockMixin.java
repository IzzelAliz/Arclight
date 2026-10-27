package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.block.LockableTileState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityLockMixin {

    @Decorate(method = "createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/LockCode;canUnlock(Lnet/minecraft/world/entity/player/Player;)Z"),
        require = 1, allow = 1)
    private boolean arclight$beaconLockCheck(LockCode code, Player player) throws Throwable {
        BeaconBlockEntity beacon = (BeaconBlockEntity) (Object) this;
        // Match the helper's live-server context, while leaving unsupported mod block entities native.
        if (player instanceof ServerPlayer && beacon.getLevel() != null
            && beacon.getLevel().getBlockEntity(beacon.getBlockPos()) == beacon
            && LockableTileState.class.isAssignableFrom(CraftBlockStates.getBlockStateType(beacon.getType()))) {
            return CraftEventFactory.callBlockLockCheckEvent(beacon, code, beacon.getDisplayName(), player);
        }
        return (boolean) DecorationOps.callsite().invoke(code, player);
    }
}
