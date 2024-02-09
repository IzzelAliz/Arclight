package io.izzel.arclight.neoforge.mixin.core.world.entity.item;

import io.izzel.arclight.common.bridge.core.entity.item.ItemEntityBridge;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin_NeoForge implements ItemEntityBridge {

    @Override
    public int bridge$forge$onItemPickup(Player player) {
        return EventHooks.onItemPickup((ItemEntity) (Object) this, player);
    }

    @Override
    public void bridge$forge$firePlayerItemPickupEvent(Player player, ItemStack clone) {
        EventHooks.firePlayerItemPickupEvent(player, (ItemEntity) (Object) this, clone);
    }

    @Override
    public boolean bridge$common$itemDespawnEvent() {
        return true;
    }
}
