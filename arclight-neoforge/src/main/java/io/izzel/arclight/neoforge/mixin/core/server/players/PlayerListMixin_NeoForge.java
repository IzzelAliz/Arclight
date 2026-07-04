package io.izzel.arclight.neoforge.mixin.core.server.players;

import io.izzel.arclight.common.bridge.core.server.players.PlayerListBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin_NeoForge implements PlayerListBridge {

    @Override
    public void bridge$platform$onPlayerChangedDimension(Player player, ResourceKey<Level> fromDim, ResourceKey<Level> toDim) {
        EventHooks.firePlayerChangedDimensionEvent(player, fromDim, toDim);
    }
}
