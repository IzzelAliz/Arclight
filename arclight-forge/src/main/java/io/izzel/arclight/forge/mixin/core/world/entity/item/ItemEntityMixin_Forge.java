package io.izzel.arclight.forge.mixin.core.world.entity.item;

import io.izzel.arclight.common.bridge.core.entity.item.ItemEntityBridge;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemEntity.class)
public class ItemEntityMixin_Forge implements ItemEntityBridge {

    @Override
    public int bridge$forge$onItemPickup(Player player) {
        return ForgeEventFactory.onItemPickup((ItemEntity) (Object) this, player);
    }

    @Override
    public void bridge$forge$firePlayerItemPickupEvent(Player player, ItemStack clone) {
        ForgeEventFactory.firePlayerItemPickupEvent(player, (ItemEntity) (Object) this, clone);
    }

    @Override
    public boolean bridge$common$itemDespawnEvent() {
        return true;
    }
}
