package io.izzel.arclight.common.mod.util;

import com.google.common.base.Preconditions;
import io.izzel.arclight.common.mod.ArclightConstants;
import joptsimple.OptionSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.event.PortalEventResult;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;

import java.util.*;

public class ArclightCaptures {

    public static final class ShearDrops {
        public final ServerLevel level;
        public final net.minecraft.world.item.ItemStack tool;
        public final net.minecraft.world.level.block.state.BlockState state;
        public final BlockEntity blockEntity;
        public final Entity entity;
        public final BlockPos pos;
        public final ArrayList<org.bukkit.inventory.ItemStack> drops = new ArrayList<>();
        public java.util.function.BiConsumer<ServerLevel, net.minecraft.world.item.ItemStack> dropper;

        public ShearDrops(ServerLevel level, net.minecraft.world.item.ItemStack tool, net.minecraft.world.level.block.state.BlockState state, BlockEntity blockEntity, Entity entity, BlockPos pos) {
            this.level = level;
            this.tool = tool;
            this.state = state;
            this.blockEntity = blockEntity;
            this.entity = entity;
            this.pos = pos.immutable();
        }
    }

    public static final class WaterPotionSplashContext {
        public final net.minecraft.world.phys.HitResult hitResult;
        public io.papermc.paper.event.entity.WaterBottleSplashEvent event;
        public boolean accepted;
        public int damageCount;
        public int cursor = -1;

        public WaterPotionSplashContext(net.minecraft.world.phys.HitResult hitResult) {
            this.hitResult = hitResult;
        }
    }

    private static OptionSet bukkitOptions;

    public static void captureBukkitOptions(OptionSet options) {
        bukkitOptions = Objects.requireNonNull(options, "options");
    }

    public static OptionSet getBukkitOptions() {
        return Objects.requireNonNull(bukkitOptions, "Bukkit command-line options were not captured");
    }

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

    private static boolean lastEntityChangeBlockResult;

    public static boolean getLastEntityChangeBlockResult() {
        return lastEntityChangeBlockResult;
    }

    public static boolean setLastEntityChangeBlockResult(boolean b) {
        return lastEntityChangeBlockResult = b;
    }

    /**
     * Indicates that next BlockBreakEvent is fired directly by ServerPlayerGameMode#destroyBlock
     * and need to be captured as primary event.
     *
     * @see net.minecraft.server.level.ServerPlayerGameMode#destroyBlock(BlockPos)
     */
    public static boolean isPrimaryEvent = false;
    public static Stack<BlockBreakEventContext> blockBreakEventStack = new Stack<>();
    private static BlockBreakCaptureScope blockBreakCaptureScope;

    public static BlockBreakCaptureScope beginBlockBreakCapture() {
        BlockBreakCaptureScope scope = new BlockBreakCaptureScope(blockBreakCaptureScope);
        blockBreakCaptureScope = scope;
        // A nested invocation must not consume its caller's pending primary marker.
        isPrimaryEvent = false;
        return scope;
    }

    private static int blockBreakCaptureDepth() {
        return blockBreakCaptureScope == null ? 0 : blockBreakCaptureScope.captureDepth;
    }

    public static final class BlockBreakCaptureScope implements AutoCloseable {
        private final BlockBreakCaptureScope parent;
        private final int captureDepth;
        private final boolean parentPrimaryEvent;

        private BlockBreakCaptureScope(BlockBreakCaptureScope parent) {
            this.parent = parent;
            this.captureDepth = blockBreakEventStack.size();
            this.parentPrimaryEvent = parent != null && isPrimaryEvent;
        }

        @Override
        public void close() {
            if (blockBreakCaptureScope != this) {
                throw new IllegalStateException("Block break capture scopes closed out of order");
            }
            try {
                while (blockBreakEventStack.size() > captureDepth) {
                    blockBreakEventStack.pop();
                }
            } finally {
                isPrimaryEvent = parentPrimaryEvent;
                blockBreakCaptureScope = parent;
            }
        }
    }

    public static void captureNextBlockBreakEventAsPrimaryEvent() {
        // fix #674, some mod will implement their own "destroyBlock(...)"
        // and its context cannot be tracked by Arclight directly.
        // This is used to tell whether the event is fired by vanilla destroyBlock.
        isPrimaryEvent = true;
    }

    public static void captureBlockBreakPlayer(BlockBreakEvent event) {
        captureBlockBreakPlayer(event, null);
    }

    public static void captureBlockBreakPlayer(BlockBreakEvent event, Block block) {
        blockBreakEventStack.push(new BlockBreakEventContext(event, isPrimaryEvent, block));
        isPrimaryEvent = false;
    }

    private static final Deque<BlockBreakExperienceScope> vanillaBlockBreakExperienceScopes = new ArrayDeque<>();

    public static void pushVanillaBlockBreakExperienceSuppression(ServerLevel level, BlockPos pos) {
        vanillaBlockBreakExperienceScopes.push(new BlockBreakExperienceScope(level, pos));
    }

    public static boolean shouldSuppressVanillaBlockBreakExperience(ServerLevel level, BlockPos pos) {
        Deque<BlockBreakExperienceScope> scopes = vanillaBlockBreakExperienceScopes;
        return !scopes.isEmpty() && scopes.peek().matches(level, pos);
    }

    public static void popVanillaBlockBreakExperienceSuppression() {
        Deque<BlockBreakExperienceScope> scopes = vanillaBlockBreakExperienceScopes;
        if (!scopes.isEmpty()) {
            scopes.pop();
        }
    }

    public static List<ItemEntity> getBlockDrops() {
        if (!blockBreakEventStack.empty()) {
            return blockBreakEventStack.peek().getBlockDrops();
        } else if (captureBlockPopRes) {
            return Preconditions.checkNotNull(extraDrops, "Not capturing drops!");
        } else {
            return null;
        }
    }

    public static boolean getBlockBreakDropItems() {
        if (!blockBreakEventStack.empty()) {
            return blockBreakEventStack.peek().isDropItems();
        }
        return true;
    }

    public static BlockBreakEventContext popPrimaryBlockBreakEvent() {
        int captureDepth = blockBreakCaptureDepth();
        if (blockBreakEventStack.size() > captureDepth) {
            BlockBreakEventContext eventContext = blockBreakEventStack.pop();

            // deal with unhandled secondary events
            // should never happen, but just in case
            ArrayList<BlockBreakEventContext> unhandledEvents = new ArrayList<>();
            while (blockBreakEventStack.size() > captureDepth && !eventContext.isPrimary()) {
                unhandledEvents.add(eventContext);
                eventContext = blockBreakEventStack.pop();
            }

            if (!unhandledEvents.isEmpty()) {
                // ArclightMod.LOGGER.warn("Unhandled secondary block break event");
                eventContext.mergeAllDrops(unhandledEvents);
            }

            return eventContext;
        } else {
            return null;
        }
    }

    public static BlockBreakEventContext popSecondaryBlockBreakEvent() {
        if (blockBreakEventStack.size() > blockBreakCaptureDepth()) {
            BlockBreakEventContext eventContext = blockBreakEventStack.peek();
            if (!eventContext.isPrimary()) {
                return blockBreakEventStack.pop();
            }
        }
        return null;
    }

    public static void clearBlockBreakEventContexts() {
        // tick/destroyAndAck may be reentered by a listener. Those are not new
        // capture sessions while an ancestor destroyBlock/finalizer is active.
        if (blockBreakCaptureScope != null) {
            return;
        }
        isPrimaryEvent = false;
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

    /** Returns the current damage entity without consuming it, for nested invocation restoration. */
    public static Entity peekDamageEventEntity() {
        return damageEventEntity;
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

    private static transient Stack<Player> containerOwner = new Stack<>();

    public static void captureContainerOwner(Player entity) {
        containerOwner.push(entity);
    }

    public static Player getContainerOwner() {
        return containerOwner.peek();
    }

    public static void popContainerOwner(Player entity) {
        if (!containerOwner.empty() && entity == getContainerOwner()) {
            containerOwner.pop();
        }
    }

    private static transient PortalEventResult craftPortalEvent;

    public static void captureCraftPortalEvent(PortalEventResult event) {
        craftPortalEvent = event;
    }

    public static PortalEventResult getCraftPortalEvent() {
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

    private static Stack<ArclightDamageContainer> damageContainers = new Stack<>();

    public static void captureDamageContainer(ArclightDamageContainer container) {
        damageContainers.push(container);
    }

    public static ArclightDamageContainer getDamageContainer() {
        return damageContainers.peek();
    }

    public static void popDamageContainer() {
        damageContainers.pop();
    }

    private static transient WorldLoader.DataLoadContext dataLoadContext;

    public static void captureDataLoadContext(WorldLoader.DataLoadContext context) {
        dataLoadContext = context;
    }

    public static WorldLoader.DataLoadContext getDataLoadContext() {
        try {
            return Objects.requireNonNull(dataLoadContext, "dataLoadContext");
        } finally {
            dataLoadContext = null;
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

    public static BlockPos captureSpreadSource(BlockPos source) {
        BlockPos old = spreadPos;
        spreadPos = source.immutable();
        return old;
    }

    public static BlockPos getSpreadPos() {
        return spreadPos;
    }

    public static void resetSpreadSource(BlockPos old, BlockPos now) {
        if (spreadPos == now) spreadPos = old;
    }

    private static boolean playerInteractCancelled;

    public static void cancelNextPlayerInteract() { playerInteractCancelled = true; }

    public static boolean shouldCancelPlayerInteract() {
        try {
            return playerInteractCancelled;
        } finally {
            playerInteractCancelled = false;
        }
    }

    private static boolean captureBlockPopRes = false;

    public static void captureBlockPopResForExtraDrops(boolean state) {
        captureBlockPopRes = state;
    }

    private static List<ItemEntity> extraDrops;

    /**
     * 1. Used by armor stands to capture their contents. <br/>
     * 2. Used by foxes to capture their drops. <br/>
     * 3. SENTINEL
     * @see io.izzel.arclight.common.mod.server.event.EntityEventHandler#monitorLivingDrops(LivingEntity, DamageSource, List, boolean)
     */
    public static void captureExtraDrops(List<ItemEntity> capturedDrops) {
        extraDrops = capturedDrops;
    }

    public static List<ItemEntity> consumeExtraDrops() {
        try {
            return extraDrops;
        } finally {
            extraDrops = null;
        }
    }

    private static Inventory deathPlayerInv;

    public static void capturePlayerDeathInv(Inventory inventory) {
        deathPlayerInv = inventory;
    }

    public static Inventory getDeathPlayerInv() {
        try {
            return deathPlayerInv;
        } finally {
            deathPlayerInv = null;
        }
    }

    private static void recapture(String type) {
        throw new IllegalStateException("Recapturing " + type);
    }

    public static class BlockBreakEventContext {

        private final BlockBreakEvent blockBreakEvent;
        private final ArrayList<ItemEntity> blockDrops;
        private final BlockState blockBreakPlayerState;
        private final boolean primary;
        private final Block nmsBlock;

        public BlockBreakEventContext(BlockBreakEvent event, boolean primary, Block nmsBlock) {
            this.blockBreakEvent = event;
            this.blockDrops = new ArrayList<>();
            this.blockBreakPlayerState = event.getBlock().getState();
            this.primary = primary;
            this.nmsBlock = nmsBlock;
        }

        public BlockBreakEvent getEvent() {
            return blockBreakEvent;
        }

        public ArrayList<ItemEntity> getBlockDrops() {
            return blockDrops;
        }

        public boolean isDropItems() {
            return blockBreakEvent.isDropItems();
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

        public Block getNmsBlock() {
            return nmsBlock;
        }

        public boolean shouldFinalizeExperience() {
            return blockBreakEvent != null && nmsBlock != null;
        }
    }

    private record BlockBreakExperienceScope(ServerLevel level, BlockPos pos) {
        private boolean matches(ServerLevel level, BlockPos pos) {
            return this.level == level && this.pos.equals(pos);
        }
    }

    public static com.mojang.datafixers.util.Pair<net.minecraft.world.item.ItemStack, LivingEntity> crossbowNoConsume;
    public static com.mojang.datafixers.util.Pair<Inventory, Integer> pickItemTarget;
    public static net.minecraft.server.level.ServerPlayer gameModeSwitcher;
    public static org.apache.commons.lang3.tuple.Triple<net.minecraft.world.item.ItemStack, LivingEntity, Boolean> itemDamage;
    public static com.mojang.datafixers.util.Pair<org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason, Runnable> preCreatureSpawn;

    public static org.bukkit.event.player.PlayerGameModeChangeEvent.Cause takeGameModeCause(net.minecraft.server.level.ServerPlayer player) {
        if (gameModeSwitcher == player) {
            gameModeSwitcher = null;
            return org.bukkit.event.player.PlayerGameModeChangeEvent.Cause.GAMEMODE_SWITCHER;
        }
        return org.bukkit.event.player.PlayerGameModeChangeEvent.Cause.COMMAND;
    }

    public static boolean isItemDamageForced(net.minecraft.world.item.ItemStack stack, LivingEntity owner) {
        return itemDamage != null && itemDamage.getLeft() == stack && itemDamage.getMiddle() == owner && itemDamage.getRight();
    }

    public static LivingEntity getItemDamageOwner(net.minecraft.world.item.ItemStack stack, LivingEntity nativeOwner) {
        var damage = itemDamage;
        if (damage != null && damage.getLeft() == stack && (damage.getMiddle() == nativeOwner
            || nativeOwner == null && !(damage.getMiddle() instanceof net.minecraft.server.level.ServerPlayer))) {
            return damage.getMiddle();
        }
        return null;
    }

    public static void withItemDamage(net.minecraft.world.item.ItemStack stack, LivingEntity owner, boolean force, Runnable action) {
        var previous = itemDamage;
        itemDamage = org.apache.commons.lang3.tuple.Triple.of(stack, owner, force);
        try {
            action.run();
        } finally {
            itemDamage = previous;
        }
    }

    public static <T> T withPreCreatureSpawn(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason,
                                            Runnable onAbort, java.util.function.Supplier<T> action) {
        var previous = preCreatureSpawn;
        preCreatureSpawn = com.mojang.datafixers.util.Pair.of(reason, onAbort);
        try {
            return action.get();
        } finally {
            preCreatureSpawn = previous;
        }
    }

    public static org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason getPreCreatureSpawnReason() {
        return preCreatureSpawn == null ? org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.DEFAULT : preCreatureSpawn.getFirst();
    }

    public static org.apache.commons.lang3.tuple.Triple<net.minecraft.world.item.component.BlocksAttacks, net.minecraft.world.item.ItemStack,
        com.mojang.datafixers.util.Pair<LivingEntity, LivingEntity>> shieldDisable;

    public static org.apache.commons.lang3.tuple.Triple<net.minecraft.world.entity.animal.golem.SnowGolem,
        net.minecraft.world.item.ItemInstance, java.util.List<net.minecraft.world.item.ItemStack>> snowGolemShearDrops;

    public static io.izzel.arclight.common.mod.server.event.ArclightEventFactory.BlockDestruction blockDestruction;

    public static com.mojang.datafixers.util.Pair<BlockPos,
        org.apache.commons.lang3.tuple.Triple<net.minecraft.server.level.ServerPlayer, net.minecraft.world.item.ItemStack, Integer>> furnaceExtraction;
    public static com.mojang.datafixers.util.Pair<BlockPos,
        org.apache.commons.lang3.tuple.Triple<net.minecraft.server.level.ServerPlayer, net.minecraft.world.item.ItemStack, Integer>> furnaceExperience;

    private static Deque<Map<Entity, Float>> fireDurations;
    public static void pushFireDurations() {
        Deque<Map<Entity, Float>> scopes = fireDurations;
        if (scopes == null) {
            scopes = new ArrayDeque<>();
            fireDurations = scopes;
        }
        scopes.push(new IdentityHashMap<>());
    }

    public static void popFireDurations() {
        Deque<Map<Entity, Float>> scopes = fireDurations;
        scopes.pop();
        if (scopes.isEmpty()) {
            fireDurations = null;
        }
    }

    public static void captureFireDuration(Entity entity, float duration) {
        Deque<Map<Entity, Float>> scopes = fireDurations;
        if (scopes != null && !scopes.isEmpty()) {
            scopes.peek().put(entity, duration);
        }
    }

    public static float consumeFireDuration(Entity entity, float fallback) {
        Deque<Map<Entity, Float>> scopes = fireDurations;
        if (scopes == null || scopes.isEmpty()) {
            return fallback;
        }
        Float duration = scopes.peek().remove(entity);
        return duration == null ? fallback : duration;
    }

    public static com.mojang.datafixers.util.Pair<LivingEntity, org.apache.commons.lang3.mutable.MutableBoolean> chorusPending;
    public static com.mojang.datafixers.util.Pair<LivingEntity, org.apache.commons.lang3.mutable.MutableBoolean> chorusActive;

    public static org.apache.commons.lang3.tuple.Triple<LivingEntity, net.minecraft.world.item.ItemInstance,
        com.mojang.datafixers.util.Pair<net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>,
            java.util.List<net.minecraft.world.item.ItemStack>>> forgeShearDrops;

    public static Object nightLevel;
    public static Object nightManager;
    public static Object nightClock;
    public static java.util.function.LongBinaryOperator nightProposal;
    public static boolean nightCancelled;

    public static final class NightSkipCancelled extends RuntimeException {
        public NightSkipCancelled() {
            super(null, null, false, false);
        }
    }

    public static io.izzel.arclight.common.mod.server.event.ArclightEventFactory.SlimeSplit slimeSplit;
}
