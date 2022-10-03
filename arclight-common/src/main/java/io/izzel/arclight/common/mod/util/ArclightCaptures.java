package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import org.bukkit.event.entity.EntityPotionEffectEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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

    /**
     * Indicates that next BlockBreakEvent is fired directly by ServerPlayerGameMode#destroyBlock
     * and need to be captured as primary event.
     *
     * @see net.minecraft.server.level.ServerPlayerGameMode#destroyBlock(BlockPos)
     */
    public static boolean isPrimaryEvent = false;
    public static Stack<BlockBreakEventContext> blockBreakEventStack = new Stack<>();

    public static void captureNextBlockBreakEventAsPrimaryEvent() {
        // fix #674, some mod will implement their own "destroyBlock(...)"
        // and its context cannot be tracked by Arclight directly.
        // This is used to tell whether the event is fired by vanilla destroyBlock.
        isPrimaryEvent = true;
    }

    public static void captureBlockBreakPlayer(BlockBreakEvent event) {
        blockBreakEventStack.push(new BlockBreakEventContext(event, isPrimaryEvent));
        isPrimaryEvent = false;
    }

    public static List<ItemEntity> getBlockDrops() {
        if (!blockBreakEventStack.empty()) {
            return blockBreakEventStack.peek().getBlockDrops();
        }
        return null;
    }

    public static BlockBreakEventContext popPrimaryBlockBreakEvent() {
        if (blockBreakEventStack.size() > 0) {
            BlockBreakEventContext eventContext = blockBreakEventStack.pop();

            // deal with unhandled secondary events
            // should never happen, but just in case
            ArrayList<BlockBreakEventContext> unhandledEvents = new ArrayList<>();
            while (!blockBreakEventStack.empty() && !eventContext.isPrimary()) {
                unhandledEvents.add(eventContext);
                eventContext = blockBreakEventStack.pop();
            }

            if (unhandledEvents.size() > 0) {
                // ArclightMod.LOGGER.warn("Unhandled secondary block break event");
                eventContext.mergeAllDrops(unhandledEvents);
            }

            return eventContext;
        } else {
            return null;
        }
    }

    public static BlockBreakEventContext popSecondaryBlockBreakEvent() {
        if (blockBreakEventStack.size() > 0) {
            BlockBreakEventContext eventContext = blockBreakEventStack.peek();
            if (!eventContext.isPrimary()) {
                return blockBreakEventStack.pop();
            }
        }
        return null;
    }

    public static void clearBlockBreakEventContexts() {
        if (!blockBreakEventStack.empty()) {
            // ArclightMod.LOGGER.warn("Unhandled block break event");
            blockBreakEventStack.clear();
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

    private static ServerLevel tickingLevel;
    private static BlockPos tickingPosition;

    public static void captureTickingBlock(ServerLevel level, BlockPos pos) {
        tickingLevel = level;
        tickingPosition = pos;
    }

    public static ServerLevel getTickingLevel() {
        return tickingLevel;
    }

    public static BlockPos getTickingPosition() {
        return tickingPosition;
    }

    public static void resetTickingBlock() {
        tickingLevel = null;
        tickingPosition = null;
    }

    private static Entity tickingEntity;

    public static void captureTickingEntity(Entity entity) {
        tickingEntity = entity;
    }

    public static Entity getTickingEntity() {
        return tickingEntity;
    }

    public static void resetTickingEntity() {
        tickingEntity = null;
    }

    private static EntityPotionEffectEvent.Cause effectCause;

    public static void captureEffectCause(EntityPotionEffectEvent.Cause cause) {
        effectCause = cause;
    }

    public static EntityPotionEffectEvent.Cause getEffectCause() {
        try {
            return effectCause;
        } finally {
            effectCause = null;
        }
    }

    private static BlockPos spreadPos;

    public static void captureSpreadSource(BlockPos source) {
        spreadPos = source.immutable();
    }

    public static BlockPos getSpreadPos() {
        return spreadPos;
    }

    public static void resetSpreadSource() {
        spreadPos = null;
    }

    private static void recapture(String type) {
        throw new IllegalStateException("Recapturing " + type);
    }

    public static class BlockBreakEventContext {

        final private BlockBreakEvent blockBreakEvent;
        final private ArrayList<ItemEntity> blockDrops;
        final private BlockState blockBreakPlayerState;
        final private boolean primary;

        public BlockBreakEventContext(BlockBreakEvent event, boolean primary) {
            this.blockBreakEvent = event;
            this.blockDrops = new ArrayList<>();
            this.blockBreakPlayerState = event.getBlock().getState();
            this.primary = primary;
        }

        public BlockBreakEvent getEvent() {
            return blockBreakEvent;
        }

        public ArrayList<ItemEntity> getBlockDrops() {
            return blockDrops;
        }

        public BlockState getBlockBreakPlayerState() {
            return blockBreakPlayerState;
        }

        public void mergeAllDrops(List<BlockBreakEventContext> others) {
            for (BlockBreakEventContext other : others) {
                this.getBlockDrops().addAll(other.getBlockDrops());
            }
        }

        public boolean isPrimary() {
            return primary;
        }
    }
}
