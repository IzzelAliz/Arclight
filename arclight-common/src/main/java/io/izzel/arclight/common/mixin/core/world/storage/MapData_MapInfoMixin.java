package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import org.bukkit.craftbukkit.v.map.RenderData;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.map.MapCursor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import io.izzel.arclight.common.bridge.world.storage.MapDataBridge;

import javax.annotation.Nullable;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import java.util.ArrayList;
import java.util.Collection;

@Mixin(MapItemSavedData.HoldingPlayer.class)
public class MapData_MapInfoMixin {

    // @formatter:off
    @SuppressWarnings("target") @Shadow(aliases = {"this$0", "field_176107_c"}, remap = false) private MapItemSavedData outerThis;
    @Shadow private boolean dirtyData;
    @Shadow private int minDirtyX;
    @Shadow private int minDirtyY;
    @Shadow private int maxDirtyX;
    @Shadow private int maxDirtyY;
    @Shadow private int tick;
    @Shadow @Final public Player player;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    public Packet<?> nextUpdatePacket(ItemStack stack) {
        RenderData render = ((MapDataBridge) outerThis).bridge$getMapView().render(((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity()); // CraftBukkit
        Collection<MapDecoration> icons = new ArrayList<>();
        for (MapCursor cursor : render.cursors) {
            if (cursor.isVisible()) {
                icons.add(new MapDecoration(MapDecoration.Type.byIcon(cursor.getRawType()),
                    cursor.getX(), cursor.getY(), cursor.getDirection(), CraftChatMessage.fromStringOrNull(cursor.getCaption())));
            }
        }
        if (this.dirtyData) {
            this.dirtyData = false;
            return new ClientboundMapItemDataPacket(MapItem.getMapId(stack), outerThis.scale, outerThis.trackingPosition, outerThis.locked, icons, render.buffer, this.minDirtyX, this.minDirtyY, this.maxDirtyX + 1 - this.minDirtyX, this.maxDirtyY + 1 - this.minDirtyY);
        } else {
            return this.tick++ % 5 == 0 ? new ClientboundMapItemDataPacket(MapItem.getMapId(stack), outerThis.scale, outerThis.trackingPosition, outerThis.locked, icons, render.buffer, 0, 0, 0, 0) : null;
        }
    }
}
