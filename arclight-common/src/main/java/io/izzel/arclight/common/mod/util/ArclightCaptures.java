package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v.event.CraftPortalEvent;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.List;

public class ArclightCaptures {

    private static Entity entityChangeBlock;

    public static void captureEntityChangeBlock(Entity entity) {
        entityChangeBlock = entity;
    }

    public static Entity getEntityChangeBlock() {
        try {
            return entityChangeBlock;
        } finally {
            entityChangeBlock = null;
        }
    }

    private static BlockBreakEvent blockBreakEvent;
    private static List<ItemEntity> blockDrops;
    private static BlockState blockBreakPlayerState;

    public static void captureBlockBreakPlayer(BlockBreakEvent event) {
        if (blockBreakEvent == null) {
            // Force event context to be captured only when no event is being handled.
            // Otherwise, it should be fired by handlers of current event as the event system is single threaded,
            // which should not change the event context.
            blockBreakEvent = event;
            blockDrops = new ArrayList<>();
            blockBreakPlayerState = event.getBlock().getState();
        }
    }

    public static BlockBreakEvent getBlockBreakPlayer() {
        return blockBreakEvent;
    }

    public static BlockState getBlockBreakPlayerState() {
        return blockBreakPlayerState;
    }

    public static List<ItemEntity> getBlockDrops() {
        return blockDrops;
    }

    public static BlockBreakEvent resetBlockBreakPlayer() {
        try {
            return blockBreakEvent;
        } finally {
            blockBreakEvent = null;
            blockDrops = null;
            blockBreakPlayerState = null;
        }
    }

    private static String quitMessage;

    public static void captureQuitMessage(String quitMessage) {
        ArclightCaptures.quitMessage = quitMessage;
    }

    public static String getQuitMessage() {
        try {
            return quitMessage;
        } finally {
            quitMessage = null;
        }
    }

    private static Direction placeEventDirection;

    public static void capturePlaceEventDirection(Direction direction) {
        ArclightCaptures.placeEventDirection = direction;
    }

    public static Direction getPlaceEventDirection() {
        try {
            return placeEventDirection;
        } finally {
            placeEventDirection = null;
        }
    }

    private static InteractionHand placeEventHand;

    public static void capturePlaceEventHand(InteractionHand hand) {
        ArclightCaptures.placeEventHand = hand;
    }

    public static InteractionHand getPlaceEventHand(InteractionHand hand) {
        try {
            return placeEventHand == null ? hand : placeEventHand;
        } finally {
            placeEventHand = null;
        }
    }

    private static TreeType treeType;

    public static void captureTreeType(TreeType treeType) {
        ArclightCaptures.treeType = treeType;
    }

    public static TreeType getTreeType() {
        try {
            return treeType == null ? ArclightConstants.MOD : treeType;
        } finally {
            treeType = null;
        }
    }

    private static transient AbstractContainerMenu arclight$capturedContainer;

    public static void captureWorkbenchContainer(AbstractContainerMenu container) {
        arclight$capturedContainer = container;
    }

    public static AbstractContainerMenu getWorkbenchContainer() {
        try {
            return arclight$capturedContainer;
        } finally {
            arclight$capturedContainer = null;
        }
    }

    private static transient Entity damageEventEntity;

    public static void captureDamageEventEntity(Entity entity) {
        damageEventEntity = entity;
    }

    public static Entity getDamageEventEntity() {
        try {
            return damageEventEntity;
        } finally {
            damageEventEntity = null;
        }
    }

    private static transient BlockPos damageEventBlock;

    public static void captureDamageEventBlock(BlockPos blockState) {
        damageEventBlock = blockState;
    }

    public static BlockPos getDamageEventBlock() {
        try {
            return damageEventBlock;
        } finally {
            damageEventBlock = null;
        }
    }

    private static transient Player containerOwner;

    public static void captureContainerOwner(Player entity) {
        containerOwner = entity;
    }

    public static Player getContainerOwner() {
        return containerOwner;
    }

    public static void resetContainerOwner() {
        containerOwner = null;
    }

    private static transient CraftPortalEvent craftPortalEvent;

    public static void captureCraftPortalEvent(CraftPortalEvent event) {
        craftPortalEvent = event;
    }

    public static CraftPortalEvent getCraftPortalEvent() {
        try {
            return craftPortalEvent;
        } finally {
            craftPortalEvent = null;
        }
    }

    private static transient Entity endPortalEntity;
    private static transient boolean spawnPortal;

    public static void captureEndPortalEntity(Entity entity, boolean portal) {
        endPortalEntity = entity;
        spawnPortal = portal;
    }

    public static boolean getEndPortalSpawn() {
        return spawnPortal;
    }

    public static Entity getEndPortalEntity() {
        try {
            return endPortalEntity;
        } finally {
            endPortalEntity = null;
            spawnPortal = false;
        }
    }

    private static transient DataPackConfig datapackCodec;

    public static void captureDatapackConfig(DataPackConfig codec) {
        datapackCodec = codec;
    }

    public static DataPackConfig getDatapackConfig() {
        try {
            return datapackCodec;
        } finally {
            datapackCodec = null;
        }
    }

    private static transient BlockEntity tickingBlockEntity;

    public static void captureTickingBlockEntity(BlockEntity entity) {
        tickingBlockEntity = entity;
    }

    public static void resetTickingBlockEntity() {
        tickingBlockEntity = null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> T getTickingBlockEntity() {
        return (T) tickingBlockEntity;
    }

    private static void recapture(String type) {
        throw new IllegalStateException("Recapturing " + type);
    }

}
