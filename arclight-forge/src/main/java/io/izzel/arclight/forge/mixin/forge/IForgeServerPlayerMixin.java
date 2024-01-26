package io.izzel.arclight.forge.mixin.forge;

import io.izzel.arclight.common.bridge.core.inventory.container.ContainerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.common.extensions.IForgeServerPlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkInitialization;
import net.minecraftforge.network.packets.OpenContainer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Consumer;

@Mixin(value = IForgeServerPlayer.class, remap = false)
public interface IForgeServerPlayerMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @SuppressWarnings("resource")
    default void openMenu(MenuProvider containerSupplier, Consumer<FriendlyByteBuf> extraDataWriter) {
        var player = (ServerPlayer) this;
        if (player.level().isClientSide) return;
        player.doCloseContainer();
        player.nextContainerCounter();
        int openContainerId = player.containerCounter;
        FriendlyByteBuf extraData = new FriendlyByteBuf(Unpooled.buffer());
        extraDataWriter.accept(extraData);
        extraData.readerIndex(0); // reset to beginning in case modders read for whatever reason

        FriendlyByteBuf output = new FriendlyByteBuf(Unpooled.buffer());
        output.writeVarInt(extraData.readableBytes());
        output.writeBytes(extraData);

        if (output.readableBytes() > 32600 || output.readableBytes() < 1)
            throw new IllegalArgumentException("Invalid PacketBuffer for openGui, found " + output.readableBytes() + " bytes");
        var c = containerSupplier.createMenu(openContainerId, player.getInventory(), player);
        if (c == null)
            return;

        ((ContainerBridge) c).bridge$setTitle(containerSupplier.getDisplayName());
        ArclightCaptures.captureContainerOwner(player);
        c = CraftEventFactory.callInventoryOpenEvent(player, c);
        ArclightCaptures.resetContainerOwner();
        if (c == null) {
            if (containerSupplier instanceof Container) {
                ((Container) containerSupplier).stopOpen(player);
            }
            return;
        }

        var msg = new OpenContainer(c.getType(), openContainerId, containerSupplier.getDisplayName(), output);
        NetworkInitialization.PLAY.send(msg, player.connection.getConnection());

        player.containerMenu = c;
        player.initMenu(player.containerMenu);
        ForgeEventFactory.onPlayerOpenContainer(player, c);
    }
}
