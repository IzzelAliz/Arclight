package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.List;

public class ArclightCaptures {

    private static Entity entityChangeBlock;

    public static void captureEntityChangeBlock(Entity entity) {
        if (entityChangeBlock == null) {
            entityChangeBlock = entity;
        } else {
            recapture("entity change block");
        }
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
            blockBreakEvent = event;
            blockDrops = new ArrayList<>();
            blockBreakPlayerState = event.getBlock().getState();
        } else {
            recapture("block break");
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
        if (ArclightCaptures.quitMessage == null) {
            ArclightCaptures.quitMessage = quitMessage;
        } else {
            recapture("quit message");
        }
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

    private static Hand placeEventHand;

    public static void capturePlaceEventHand(Hand hand) {
        if (ArclightCaptures.placeEventHand == null) {
            ArclightCaptures.placeEventHand = hand;
        } else {
            recapture("place hand");
        }
    }

    public static Hand getPlaceEventHand(Hand hand) {
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

    private static transient Container arclight$capturedContainer;

    public static void captureWorkbenchContainer(Container container) {
        if (arclight$capturedContainer == null) {
            arclight$capturedContainer = container;
        } else {
            recapture("workbench container");
        }
    }

    public static Container getWorkbenchContainer() {
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

    private static transient PlayerEntity containerOwner;

    public static void captureContainerOwner(PlayerEntity entity) {
        containerOwner = entity;
    }

    public static PlayerEntity getContainerOwner() {
        return containerOwner;
    }

    public static void resetContainerOwner() {
        containerOwner = null;
    }

    private static void recapture(String type) {
        throw new IllegalStateException("Recapturing " + type);
    }

}
