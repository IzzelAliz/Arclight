package io.izzel.arclight.common.mod.server.event;

import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import io.izzel.arclight.common.bridge.tags.NetworkTagCollectionBridge;

public class NetworkEventDispatcher {

    @SubscribeEvent
    public void onTagUpdate(TagsUpdatedEvent event) {
        ((NetworkTagCollectionBridge) event.getTagManager().getBlocks()).bridge$increaseTag();
        ((NetworkTagCollectionBridge) event.getTagManager().getEntityTypes()).bridge$increaseTag();
        ((NetworkTagCollectionBridge) event.getTagManager().getFluids()).bridge$increaseTag();
        ((NetworkTagCollectionBridge) event.getTagManager().getItems()).bridge$increaseTag();
    }
}
