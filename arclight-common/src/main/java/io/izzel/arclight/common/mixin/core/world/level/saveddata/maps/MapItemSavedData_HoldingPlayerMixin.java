package io.izzel.arclight.common.mixin.core.world.level.saveddata.maps;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.world.storage.MapDataBridge;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.craftbukkit.v.map.RenderData;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.map.MapCursor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

@Mixin(MapItemSavedData.HoldingPlayer.class)
public abstract class MapItemSavedData_HoldingPlayerMixin {

    // @formatter:off
    @SuppressWarnings("target") @Shadow(aliases = {"this$0", "f_77961_"}, remap = false) private MapItemSavedData outerThis;
    @Shadow private boolean dirtyData;
    @Shadow private int minDirtyX;
    @Shadow private int minDirtyY;
    @Shadow private int maxDirtyX;
    @Shadow private int maxDirtyY;
    @Shadow private int tick;
    @Shadow @Final public Player player;
    @Shadow private boolean dirtyDecorations;
    @Shadow protected abstract MapItemSavedData.MapPatch createPatch();
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    public Packet<?> nextUpdatePacket(int i) {
        RenderData render = ((MapDataBridge) outerThis).bridge$getMapView().render(((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity()); // CraftBukkit
        MapItemSavedData.MapPatch patch;
        if (this.dirtyData) {
            this.dirtyData = false;
            var colors = outerThis.colors;
            outerThis.colors = render.buffer;
            patch = this.createPatch();
            outerThis.colors = colors;
        } else {
            patch = null;
        }

        Collection<MapDecoration> icons;
        if (this.tick++ % 5 == 0) {
            this.dirtyDecorations = false;
            icons = new ArrayList<>();
            for (MapCursor cursor : render.cursors) {
                if (cursor.isVisible()) {
                    icons.add(new MapDecoration(MapDecoration.Type.byIcon(cursor.getRawType()),
                        cursor.getX(), cursor.getY(), cursor.getDirection(), CraftChatMessage.fromStringOrNull(cursor.getCaption())));
                }
            }
        } else {
            icons = null;
        }
        return icons == null && patch == null ? null : new ClientboundMapItemDataPacket(i, outerThis.scale, outerThis.locked, icons, patch);
    }
}
