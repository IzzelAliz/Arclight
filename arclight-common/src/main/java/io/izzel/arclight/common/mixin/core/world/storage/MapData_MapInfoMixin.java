package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SMapDataPacket;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.MapDecoration;
import org.bukkit.craftbukkit.v.map.RenderData;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.map.MapCursor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import io.izzel.arclight.common.bridge.world.storage.MapDataBridge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

@Mixin(MapData.MapInfo.class)
public class MapData_MapInfoMixin {

    // @formatter:off
    @SuppressWarnings("target") @Shadow(aliases = {"this$0", "field_176107_c"}, remap = false) private MapData outerThis;
    @Shadow private boolean isDirty;
    @Shadow private int minX;
    @Shadow private int minY;
    @Shadow private int maxX;
    @Shadow private int maxY;
    @Shadow private int tick;
    @Shadow @Final public PlayerEntity player;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    public IPacket<?> getPacket(ItemStack stack) {
        RenderData render = ((MapDataBridge) outerThis).bridge$getMapView().render(((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity()); // CraftBukkit
        Collection<MapDecoration> icons = new ArrayList<>();
        for (MapCursor cursor : render.cursors) {
            if (cursor.isVisible()) {
                icons.add(new MapDecoration(MapDecoration.Type.byIcon(cursor.getRawType()),
                    cursor.getX(), cursor.getY(), cursor.getDirection(), CraftChatMessage.fromStringOrNull(cursor.getCaption())));
            }
        }
        if (this.isDirty) {
            this.isDirty = false;
            return new SMapDataPacket(FilledMapItem.getMapId(stack), outerThis.scale, outerThis.trackingPosition, outerThis.locked, icons, outerThis.colors, this.minX, this.minY, this.maxX + 1 - this.minX, this.maxY + 1 - this.minY);
        } else {
            return this.tick++ % 5 == 0 ? new SMapDataPacket(FilledMapItem.getMapId(stack), outerThis.scale, outerThis.trackingPosition, outerThis.locked, icons, outerThis.colors, 0, 0, 0, 0) : null;
        }
    }
}
