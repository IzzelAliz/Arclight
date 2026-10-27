package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import io.papermc.paper.event.player.PlayerItemGroupCooldownEvent;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ServerItemCooldowns;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerItemCooldowns.class)
public abstract class ServerItemCooldownsMixin extends ItemCooldownsMixin {

    @Shadow @Final private ServerPlayer player;

    @Override
    protected PlayerItemGroupCooldownEvent arclight$createCooldownEvent(ItemStack item, Identifier group, int ticks) {
        var bukkitPlayer = ((ServerPlayerBridge) this.player).bridge$getBukkitEntity();
        var key = CraftNamespacedKey.fromMinecraft(group);
        // Both event classes share one HandlerList; a stack call must dispatch only the subtype.
        return item == null
            ? new PlayerItemGroupCooldownEvent(bukkitPlayer, key, ticks)
            : new PlayerItemCooldownEvent(bukkitPlayer, CraftItemType.minecraftToBukkit(item.getItem()), key, ticks);
    }

    @Override
    protected void arclight$resyncCooldown(Identifier group) {
        this.player.connection.send(new ClientboundCooldownPacket(group, this.arclight$currentCooldown(group)));
    }
}
