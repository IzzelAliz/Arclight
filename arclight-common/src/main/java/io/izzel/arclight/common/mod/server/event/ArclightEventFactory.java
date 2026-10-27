package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.BlockBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.damage.CraftDamageSource;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public abstract class ArclightEventFactory {

    public static<T extends Event> T callEvent(T event) {
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    /**
     * @see CraftEventFactory#callEntityDeathEvent
     */
    public static EntityDeathEvent callEntityDeathEvent(LivingEntity entity, DamageSource damageSource, List<ItemStack> drops) {
        CraftDamageSource bukkitDamageSource = new CraftDamageSource(damageSource);
        CraftLivingEntity craftLivingEntity = ((LivingEntityBridge) entity).bridge$getBukkitEntity();
        EntityDeathEvent event = new EntityDeathEvent(craftLivingEntity, bukkitDamageSource, drops, ((LivingEntityBridge) entity).bridge$getExpReward(damageSource.getEntity()));
        var death = (io.izzel.arclight.common.bridge.core.world.entity.DeathEventBridge) entity;
        death.bridge$prepareDeathEvent(event);
        callEvent(event);
        death.bridge$finishDeathEvent(event, damageSource);
        return event;
    }

    public static PlayerDeathEvent callPlayerDeathEvent(ServerPlayer victim, DamageSource damageSource, List<ItemStack> drops, int expReward, String deathMessage, boolean keepInventory) {
        CraftPlayer entity = (CraftPlayer) victim.bridge$getBukkitEntity();
        CraftDamageSource bukkitDamageSource = new CraftDamageSource(damageSource);
        PlayerDeathEvent event = new PlayerDeathEvent(entity, bukkitDamageSource, drops, expReward, 0, deathMessage);
        event.setKeepInventory(keepInventory);
        event.setKeepLevel(((ServerPlayerBridge) victim).arclight$isKeepLevel());
        var death = (io.izzel.arclight.common.bridge.core.world.entity.DeathEventBridge) victim;
        death.bridge$prepareDeathEvent(event);
        callEvent(event);
        death.bridge$finishDeathEvent(event, damageSource);
        return event;
    }

    /**
     * @see CraftEventFactory#handleBlockFormEvent
     */
    @Nullable
    public static BlockFormEvent callBlockFormEvent(Level world, BlockPos pos, BlockState block, int flag, @Nullable net.minecraft.world.entity.Entity entity) {
        // Suppress during worldgen
        if (!DistValidate.isValid(world)) {
            return null;
        }

        CraftBlockState blockState = CraftBlockStates.getBlockState(world, pos);
        blockState.setFlags(flag);
        blockState.setBlock(block);
        BlockFormEvent event = entity == null ? new BlockFormEvent(blockState.getBlock(), blockState) : new EntityBlockFormEvent(entity.bridge$getBukkitEntity(), blockState.getBlock(), blockState);
        return callEvent(event);
    }

    /**
     * @return if we can drop the item
     */
    public static boolean callEntityDropItemEvent(EntityBridge entity, ItemEntity drop) {
        EntityDropItemEvent bukkitEvent = new EntityDropItemEvent(entity.bridge$getBukkitEntity(), (Item) drop.bridge$getBukkitEntity());
        callEvent(bukkitEvent);
        return !bukkitEvent.isCancelled();
    }

    public static boolean onBlockBreak(ServerPlayerGameMode controller, ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, boolean isSwordNoBreak) {
        // Tell client the block is gone immediately then process events
        // Don't tell the client if it's a creative sword break because it's not broken!
        if (level.getBlockEntity(pos) == null && !isSwordNoBreak) {
            var packet = new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState());
            player.connection.send(packet);
        }

        var bblock = CraftBlock.at(level, pos);
        var event = new BlockBreakEvent(bblock, (Player) player.bridge$getBukkitEntity());
        ArclightCaptures.captureBlockBreakPlayer(event, state.getBlock());

        // Sword + Creative mode pre-cancel
        event.setCancelled(isSwordNoBreak);

        // Calculate default block experience
        var nmsBlock = state.getBlock();

        var itemstack = player.getItemBySlot(EquipmentSlot.MAINHAND);

        if (!event.isCancelled() && !controller.isCreative() && player.hasCorrectToolForDrops(nmsBlock.defaultBlockState())) {
            event.setExpToDrop(((BlockBridge) nmsBlock).bridge$getExpDrop(state, level, pos, itemstack));
        }

        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            if (isSwordNoBreak) {
                return false;
            }
            // Let the client know the block still exists
            player.connection.send(new ClientboundBlockUpdatePacket(level, pos));

            // Brute force all possible updates
            for (var dir : Direction.values()) {
                player.connection.send(new ClientboundBlockUpdatePacket(level, pos.relative(dir)));
            }

            // Update any tile entity data for this block
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                var packet = blockEntity.getUpdatePacket();
                if (packet != null) {
                    player.connection.send(packet);
                }
            }
            return false;
        }

        // CraftBukkit - update state from plugins
        if (level.getBlockState(pos).isAir()) {
            return false;
        }
        return true;
    }

    public static InteractionResult onBlockPlace(UseOnContext context, net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack oldStack, net.minecraft.world.item.ItemStack currentStack, InteractionResult result) {
        var world = (ServerLevel) context.getLevel();
        var blockposition = context.getClickedPos();
        var enumhand = context.getHand();
        org.bukkit.event.block.BlockPlaceEvent placeEvent = null;
        List<org.bukkit.block.BlockState> blocks = new java.util.ArrayList<>(((WorldBridge) world).bridge$getCapturedBlockState().values());

        // save new item data
        int newSize = currentStack.getCount();
        var newPatch = currentStack.getComponentsPatch();

        int size = oldStack.getCount();
        var oldPatch = oldStack.getComponentsPatch();

        currentStack.setCount(size);
        ((ItemStackBridge) (Object) currentStack).arclight$restorePatch(oldPatch);

        if (blocks.size() > 1) {
            placeEvent = CraftEventFactory.callBlockMultiPlaceEvent(world, player, enumhand, blocks, blockposition);
        } else if (blocks.size() == 1) {
            placeEvent = CraftEventFactory.callBlockPlaceEvent(world, player, enumhand, blocks.get(0), blockposition);
        }

        if (placeEvent != null && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
            result = InteractionResult.FAIL;
            // PAIL: Remove this when MC-99075 fixed
            placeEvent.getPlayer().updateInventory();
            // revert back all captured blocks
            ((WorldBridge) world).bridge$preventPoiUpdated(true); // CraftBukkit - SPIGOT-5710
            for (org.bukkit.block.BlockState blockstate : blocks) {
                blockstate.update(true, false);
            }
            ((WorldBridge) world).bridge$preventPoiUpdated(false);

            // Brute force all possible updates
            var placedPos = ((CraftBlock) placeEvent.getBlock()).getPosition();
            for (var dir : Direction.values()) {
                ((ServerPlayer) player).connection.send(new ClientboundBlockUpdatePacket(world, placedPos.relative(dir)));
            }
            // ItemSign.openSign = null; // SPIGOT-6758 - Reset on early return
        } else {
            // Change the stack to its new contents if it hasn't been tampered with.
            if (currentStack.getCount() == size && Objects.equals(currentStack.getComponentsPatch(), oldPatch)) {
                ((ItemStackBridge) (Object) currentStack).arclight$restorePatch(newPatch);
                currentStack.setCount(newSize);
            }

            for (var e : ((WorldBridge) world).bridge$getCapturedBlockEntity().entrySet()) {
                world.setBlockEntity(e.getValue());
            }

            for (var blockstate : blocks) {
                int updateFlag = ((CraftBlockState) blockstate).getFlags();
                var oldBlock = ((CraftBlockState) blockstate).getHandle();
                var newblockposition = ((CraftBlockState) blockstate).getPosition();
                var block = world.getBlockState(newblockposition);
                block.onPlace(world, newblockposition, oldBlock, true);

                ((WorldBridge) world).bridge$forge$notifyAndUpdatePhysics(newblockposition, null, oldBlock, block, updateFlag, 512); // send null chunk as chunk.k() returns false by this point
            }

            // SPIGOT-7315: Moved from BlockBed#setPlacedBy
            if (placeEvent != null && currentStack.getItem() instanceof BedItem) {
                var position = ((CraftBlock) placeEvent.getBlock()).getPosition();
                var blockData = world.getBlockState(position);

                if (blockData.getBlock() instanceof BedBlock) {
                    world.updateNeighborsAt(position, Blocks.AIR);
                    blockData.updateNeighbourShapes(world, position, 3);
                }
            }
        }
        return result;
    }

    public static void callEquipmentChangeEvent(org.bukkit.entity.LivingEntity entity,
            java.util.Map<org.bukkit.inventory.EquipmentSlot, com.mojang.datafixers.util.Pair<ItemStack, ItemStack>> snapshots) {
        var changes = new java.util.EnumMap<org.bukkit.inventory.EquipmentSlot, io.papermc.paper.event.entity.EntityEquipmentChangedEvent.EquipmentChange>(org.bukkit.inventory.EquipmentSlot.class);
        snapshots.forEach((slot, items) -> changes.put(slot, new io.papermc.paper.event.entity.EntityEquipmentChangedEvent.EquipmentChange() {
            @Override
            public ItemStack oldItem() {
                return items.getFirst().clone();
            }

            @Override
            public ItemStack newItem() {
                return items.getSecond().clone();
            }
        }));
        new io.papermc.paper.event.entity.EntityEquipmentChangedEvent(entity, changes).callEvent();
    }
    public static boolean wasProjectileAccepted(net.minecraft.world.entity.projectile.Projectile projectile) {
        return ((io.izzel.arclight.common.bridge.core.world.entity.projectile.ProjectileSpawnBridge) projectile).bridge$wasSpawnAccepted();
    }

    public static void resyncProjectileItem(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (player.containerMenu instanceof net.minecraft.world.inventory.InventoryMenu) {
            int slot = hand == net.minecraft.world.InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;
            ((io.izzel.arclight.common.bridge.core.world.inventory.AbstractContainerMenuBridge) player.containerMenu).bridge$forceSlot(player.getInventory(), slot);
        } else {
            player.containerMenu.sendAllDataToRemote();
            ((io.izzel.arclight.common.bridge.core.world.inventory.AbstractContainerMenuBridge) player.inventoryMenu).bridge$forceSlot(player.getInventory(), net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND);
        }
    }

    public static void failedProjectileLaunch(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.projectile.Projectile projectile, boolean cooldown) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (projectile instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl pearl) {
                serverPlayer.deregisterEnderPearl(pearl);
            }
            if (cooldown) {
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundCooldownPacket(player.getCooldowns().getCooldownGroup(stack), 0));
            }
        }
        resyncProjectileItem(player, hand);
    }


    public static void prepareProjectileLaunch(net.minecraft.world.entity.projectile.Projectile projectile,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack stack,
            java.util.function.Consumer<com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent> result) {
        ((io.izzel.arclight.common.bridge.core.world.entity.projectile.ProjectileSpawnBridge) projectile).bridge$setLaunchEvent(() -> {
            var event = new com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent(
                (org.bukkit.entity.Player) player.bridge$getBukkitEntity(), org.bukkit.craftbukkit.inventory.CraftItemStack.asCraftMirror(stack),
                (org.bukkit.entity.Projectile) projectile.bridge$getBukkitEntity());
            result.accept(event);
            return event.callEvent();
        });
    }

    public static final class BlockDestruction {


        private final LevelAccessor level;
        private final BlockPos pos;
        private final BlockPos source;
        private int experience;
        private List<net.minecraft.world.item.ItemStack> lootDrops = List.of();

        private BlockDestruction(LevelAccessor level, BlockPos pos, BlockPos source, int experience) {
            this.level = level;
            this.pos = pos.immutable();
            this.source = source == null ? null : source.immutable();
            this.experience = experience;
        }

        public static BlockDestruction blockBreak(LevelAccessor level, BlockPos pos, BlockPos source) {
            return new BlockDestruction(level, pos, source, 0);
        }

        public static BlockDestruction blockDestroy(ServerLevel level, BlockPos pos, int experience) {
            return new BlockDestruction(level, pos, null, experience);
        }

        public static BlockDestruction current(LevelAccessor level, BlockPos pos) {
            BlockDestruction context = ArclightCaptures.blockDestruction;
            return context != null && context.level == level && context.pos.equals(pos) ? context : null;
        }

        public boolean isBlockBreak() {
            return this.source != null;
        }

        public int experience() {
            return this.experience;
        }

        public void captureNeoForgeLoot(List<net.minecraft.world.item.ItemStack> drops) {
            this.lootDrops = drops;
        }

        public List<net.minecraft.world.item.ItemStack> editVanillaDrops(BlockState state, ServerLevel level, BlockPos pos,
                                              List<net.minecraft.world.item.ItemStack> drops) {
            int experience = ((BlockBridge) state.getBlock()).bridge$getExpDrop(state, level, pos, net.minecraft.world.item.ItemStack.EMPTY);
            List<ItemStack> bukkitDrops = new java.util.ArrayList<>(drops.size());
            for (net.minecraft.world.item.ItemStack drop : drops) {
                bukkitDrops.add(org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(drop));
            }
            io.papermc.paper.event.block.BlockBreakBlockEvent event = this.callBlockBreakEvent(bukkitDrops, experience);
            List<net.minecraft.world.item.ItemStack> result = new java.util.ArrayList<>(event.getDrops().size());
            for (ItemStack drop : event.getDrops()) {
                result.add(org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(drop));
            }
            return result;
        }

        /** Keep unchanged native item entities, including their motion and pickup metadata. */
        public void editNeoForgeDrops(ServerLevel level, BlockPos pos, List<ItemEntity> drops, int experience,
                                     java.util.function.BooleanSupplier maySpawnItems) {
            List<ItemStack> bukkitDrops = new java.util.ArrayList<>(drops.size());
            java.util.Map<ItemStack, ItemEntity> originals = new java.util.IdentityHashMap<>();
            if (maySpawnItems.getAsBoolean()) {
                for (ItemEntity entity : drops) {
                    ItemStack stack = org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(entity.getItem());
                    bukkitDrops.add(stack);
                    originals.put(stack, entity);
                }
            } else {
                // Native capture is empty when block drops are disabled; Paper still exposes the loot.
                for (net.minecraft.world.item.ItemStack stack : this.lootDrops) {
                    bukkitDrops.add(org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(stack));
                }
            }
            io.papermc.paper.event.block.BlockBreakBlockEvent event = this.callBlockBreakEvent(bukkitDrops, experience);
            drops.clear();
            if (!maySpawnItems.getAsBoolean()) {
                return;
            }
            for (ItemStack stack : event.getDrops()) {
                net.minecraft.world.item.ItemStack item = org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(stack);
                if (item.isEmpty()) {
                    continue;
                }
                ItemEntity entity = originals.remove(stack);
                if (entity == null) {
                    // Match Block.popResource for newly added Bukkit stacks; native entities are reused.
                    double x = pos.getX() + 0.5 + net.minecraft.util.Mth.nextDouble(level.getRandom(), -0.25, 0.25);
                    double y = pos.getY() + 0.5 + net.minecraft.util.Mth.nextDouble(level.getRandom(), -0.25, 0.25) - net.minecraft.world.entity.EntityType.ITEM.getHeight() / 2.0;
                    double z = pos.getZ() + 0.5 + net.minecraft.util.Mth.nextDouble(level.getRandom(), -0.25, 0.25);
                    entity = new ItemEntity(level, x, y, z, item);
                    entity.setDefaultPickUpDelay();
                } else {
                    entity.setItem(item);
                }
                drops.add(entity);
            }
        }

        private io.papermc.paper.event.block.BlockBreakBlockEvent callBlockBreakEvent(List<ItemStack> drops, int experience) {
            io.papermc.paper.event.block.BlockBreakBlockEvent event = new io.papermc.paper.event.block.BlockBreakBlockEvent(CraftBlock.at(this.level, this.pos),
                CraftBlock.at(this.level, this.source), drops);
            event.setExpToDrop(experience);
            event.callEvent();
            this.experience = event.getExpToDrop();
            return event;
        }

    }
    public static net.minecraft.world.item.ItemStack smeltResult(ServerLevel level, BlockPos pos, net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> items,
                                        net.minecraft.world.item.ItemStack input, net.minecraft.world.item.ItemStack result) {
        org.bukkit.event.inventory.FurnaceSmeltEvent event = new org.bukkit.event.inventory.FurnaceSmeltEvent(CraftBlock.at(level, pos),
            org.bukkit.craftbukkit.inventory.CraftItemStack.asCraftMirror(input), org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(result));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }
        ItemStack bukkitResult = event.getResult();
        net.minecraft.world.item.ItemStack replacement = org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(bukkitResult);
        net.minecraft.world.item.ItemStack output = items.get(2);
        if (!replacement.isEmpty() && !output.isEmpty()
            && !org.bukkit.craftbukkit.inventory.CraftItemStack.asCraftMirror(output).isSimilar(bukkitResult)) {
            return null;
        }
        return replacement;
    }

    public static int experienceToDrop(ServerLevel level, int experience) {
        var context = ArclightCaptures.furnaceExperience;
        if (context == null) {
            return experience;
        }
        org.bukkit.event.block.BlockExpEvent event;
        if (context.getSecond().getRight() != 0 && context.getSecond().getLeft() != null) {
            event = new org.bukkit.event.inventory.FurnaceExtractEvent(((ServerPlayerBridge) context.getSecond().getLeft()).bridge$getBukkitEntity(),
                CraftBlock.at(level, context.getFirst()), org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(context.getSecond().getMiddle()),
                context.getSecond().getRight(), experience);
        } else {
            event = new org.bukkit.event.block.BlockExpEvent(CraftBlock.at(level, context.getFirst()), experience);
        }
        Bukkit.getPluginManager().callEvent(event);
        return event.getExpToDrop();
    }

    private static boolean usingSignItem;
    private static boolean placingSign;
    private static BlockPos placedSignPos;
    private static PendingSignEditor pendingSignEditor;


    public static InteractionResult useSignItem(UseOnContext context, com.llamalad7.mixinextras.injector.wrapoperation.Operation<InteractionResult> original) {
        if (!(context.getItemInHand().getItem() instanceof net.minecraft.world.item.SignItem)) return original.call(context);
        usingSignItem = true;
        pendingSignEditor = null;
        try {
            InteractionResult result = original.call(context);
            finishSignPlacement(result);
            return result;
        } finally {
            usingSignItem = false;
            pendingSignEditor = null;
        }
    }

    public static InteractionResult placeSign(net.minecraft.world.item.context.BlockPlaceContext context, com.llamalad7.mixinextras.injector.wrapoperation.Operation<InteractionResult> original) {
        if (!(context.getItemInHand().getItem() instanceof net.minecraft.world.item.SignItem)) return original.call(context);
        placingSign = true;
        try {
            InteractionResult result = original.call(context);
            if (!usingSignItem) finishSignPlacement(result);
            return result;
        } finally {
            placingSign = false;
            placedSignPos = null;
            if (!usingSignItem) pendingSignEditor = null;
        }
    }

    public static boolean tagSign(BlockPos pos, Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack stack,
                              com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original, net.minecraft.world.item.BlockItem item, BlockState state) {
        placedSignPos = pos.immutable();
        try {
            return original.call(item, pos, level, player, stack, state);
        } finally {
            placedSignPos = null;
        }
    }

    public static void deferSignEditor(net.minecraft.world.level.block.SignBlock block, net.minecraft.world.entity.player.Player player, net.minecraft.world.level.block.entity.SignBlockEntity sign, boolean front,
                             com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original, BlockPos pos, Level level, net.minecraft.world.item.ItemStack stack) {
        if (placingSign && pos.equals(placedSignPos)) {
            pendingSignEditor = new PendingSignEditor(level, pos.immutable(), player, front, original);
        } else {
            original.call(block, player, sign, front);
        }
    }

    private static void finishSignPlacement(InteractionResult result) {
        PendingSignEditor editor = pendingSignEditor;
        pendingSignEditor = null;
        if (editor != null && result instanceof InteractionResult.Success success && success.wasItemInteraction()) {
            editor.open();
        }
    }

    private record PendingSignEditor(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, boolean front, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        private void open() {
            if (!level.isClientSide() && player != null
                && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign
                && level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.SignBlock block) {
                ((io.izzel.arclight.common.bridge.core.world.level.block.SignBlockEditBridge) block).bridge$openTextEdit(player, sign, front,
                    org.bukkit.event.player.PlayerSignOpenEvent.Cause.PLACE, original);
            }
        }
    }
    public static final class SlimeSplit {
        private final net.minecraft.world.entity.Mob source;
        private final List<net.minecraft.world.entity.Mob> children = new java.util.ArrayList<>();
        private final List<Runnable> admissions = new java.util.ArrayList<>();
        public SlimeSplit(net.minecraft.world.entity.Mob source) {
            this.source = source;
        }
        public static SlimeSplit current(net.minecraft.world.entity.Mob source) {
            SlimeSplit scope = ArclightCaptures.slimeSplit;
            return scope != null && scope.source == source ? scope : null;
        }
        public static boolean allow(net.minecraft.world.entity.Mob source, List<? extends net.minecraft.world.entity.Mob> children) {
            return !children.isEmpty() && !CraftEventFactory.callEntityTransformEvent(source,
                new java.util.ArrayList<LivingEntity>(children), org.bukkit.event.entity.EntityTransformEvent.TransformReason.SPLIT).isCancelled();
        }
        public void finish() {
            if (allow(source, children)) {
                for (Runnable admission : admissions) {
                    ((WorldBridge) source.level()).bridge$pushAddEntityReason(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SLIME_SPLIT);
                    try { admission.run(); }
                    finally { ((WorldBridge) source.level()).bridge$pushAddEntityReason(null); }
                }
            }
            children.clear();
            admissions.clear();
        }
        public <T extends net.minecraft.world.entity.Mob> Conversion<T> conversion(net.minecraft.world.entity.ConversionParams.AfterConversion<T> delegate) {
            return new Conversion<>(this, delegate);
        }
        public static final class Conversion<T extends net.minecraft.world.entity.Mob> implements net.minecraft.world.entity.ConversionParams.AfterConversion<T> {
            private final SlimeSplit scope;
            private final net.minecraft.world.entity.ConversionParams.AfterConversion<T> delegate;
            private T child;
            private Runnable admission;
            private Conversion(SlimeSplit scope, net.minecraft.world.entity.ConversionParams.AfterConversion<T> delegate) {
                this.scope = scope; this.delegate = delegate;
            }
            @Override public void finalizeConversion(T child) {
                this.child = child;
                delegate.finalizeConversion(child);
            }
            public boolean defer(net.minecraft.world.entity.Mob source, net.minecraft.world.entity.Entity entity, Runnable admission) {
                if (source != scope.source || entity != child || this.admission != null) return false;
                this.admission = admission;
                return true;
            }
            public void complete(T result) {
                if (result != null && result == child && admission != null) {
                    scope.children.add(result);
                    scope.admissions.add(admission);
                }
            }
        }
    }
}
