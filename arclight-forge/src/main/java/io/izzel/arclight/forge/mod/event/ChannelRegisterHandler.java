package io.izzel.arclight.forge.mod.event;

import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraftforge.event.network.ChannelRegistrationChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ChannelRegisterHandler {

    @SubscribeEvent
    public void onRegistrationChange(ChannelRegistrationChangeEvent event) {
        var listener = event.getSource().getPacketListener();
        if (listener instanceof ServerCommonPacketListenerImpl common) {
            var bridge = (ServerCommonPacketListenerBridge) common;
            var server = bridge.bridge$getPlayer().server;
            server.executeIfPossible(() -> {
                var craftbukkit = bridge.bridge$getCraftPlayer();
                switch (event.getType()) {
                    case REGISTER -> {
                        for (var channel : event.getChannels()) {
                            craftbukkit.addChannel(channel.toString());
                        }
                    }
                    case UNREGISTER -> {
                        for (var channel : event.getChannels()) {
                            craftbukkit.removeChannel(channel.toString());
                        }
                    }
                }
            });
        }
    }
}
