package io.izzel.arclight.common.mixin.core.server.management;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.server.management.PlayerInteractionManagerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin implements PlayerInteractionManagerBridge {

    // @formatter:off
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;
    @Shadow public abstract boolean isCreative();
    @Shadow private GameType gameModeForPlayer;
    // @formatter:on

    public boolean interactResult = false;
    public boolean firedInteract = false;
    public BlockPos interactPosition;
    public InteractionHand interactHand;
    public ItemStack interactItemStack;

    @Inject(method = "changeGameModeForPlayer", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;setGameModeForPlayer(Lnet/minecraft/world/level/GameType;Lnet/minecraft/world/level/GameType;)V"))
    private void arclight$gameModeEvent(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
        PlayerGameModeChangeEvent event = new PlayerGameModeChangeEvent(((ServerPlayerEntityBridge) player).bridge$getBukkitEntity(), GameMode.getByValue(gameType.getId()));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "handleBlockBreakAction", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;mayInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;)Z")))
    private void arclight$mayNotInteractEvent(ServerGamePacketListenerImpl instance, Packet<?> packet, BlockPos blockPos, ServerboundPlayerActionPacket.Action action, Direction direction) throws Throwable {
        CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, blockPos, direction, this.player.getInventory().getSelected(), InteractionHand.MAIN_HAND);
        DecorationOps.callsite().invoke(instance, packet);
        BlockEntity blockEntity = this.level.getBlockEntity(blockPos);
        if (blockEntity != null) {
            this.player.connection.send(blockEntity.getUpdatePacket());
        }
    }

    @Decorate(method = "handleBlockBreakAction", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;isCreative()Z"))
    private void arclight$interactEvent(BlockPos blockPos, ServerboundPlayerActionPacket.Action action, Direction direction,
                                        @Local(allocate = "playerInteractEvent") PlayerInteractEvent event) throws Throwable {
        event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, blockPos, direction, this.player.getInventory().getSelected(), InteractionHand.MAIN_HAND);
        if (event.isCancelled()) {
            this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, blockPos));
            BlockEntity blockEntity = this.level.getBlockEntity(blockPos);
            if (blockEntity != null) {
                this.player.connection.send(blockEntity.getUpdatePacket());
            }
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.blackhole().invoke();
    }

    @Decorate(method = "handleBlockBreakAction", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"))
    private boolean arclight$playerInteractCancelled(BlockState instance, BlockPos blockPos, ServerboundPlayerActionPacket.Action action, Direction direction,
                                                     @Local(allocate = "playerInteractEvent") PlayerInteractEvent event) throws Throwable {
        boolean result = false;
        if (event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) {
            BlockState data = this.level.getBlockState(blockPos);
            if (data.getBlock() instanceof DoorBlock) {
                boolean bottom = data.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
                this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, blockPos));
                this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, bottom ? blockPos.above() : blockPos.below()));
            } else if (data.getBlock() instanceof TrapDoorBlock) {
                this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, blockPos));
            }
            result = true;
        } else {
            result = (boolean) DecorationOps.callsite().invoke(instance);
        }
        return result;
    }

    @Decorate(method = "handleBlockBreakAction", inject = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"))
    private void arclight$blockDamageEvent(BlockPos blockPos, ServerboundPlayerActionPacket.Action action, Direction direction,
                                           @Local(ordinal = -1) float f,
                                           @Local(allocate = "playerInteractEvent") PlayerInteractEvent event) throws Throwable {
        if (event.useItemInHand() == Event.Result.DENY) {
            if (f > 1.0f) {
                this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, blockPos));
            }
            return;
        }
        BlockDamageEvent blockEvent = CraftEventFactory.callBlockDamageEvent(this.player, blockPos, this.player.getInventory().getSelected(), f >= 1.0f);
        if (blockEvent.isCancelled()) {
            this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, blockPos));
            return;
        }
        if (blockEvent.getInstaBreak()) {
            f = 2.0f;
        }
        DecorationOps.blackhole().invoke(f);
    }

    @Inject(method = "handleBlockBreakAction", at = @At(value = "CONSTANT", args = "stringValue=aborted destroying"))
    private void arclight$abortBlockBreak(BlockPos blockPos, ServerboundPlayerActionPacket.Action action, Direction direction, int i, int j, CallbackInfo ci) {
        CraftEventFactory.callBlockDamageAbortEvent(this.player, blockPos, this.player.getInventory().getSelected());
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    public void arclight$resetBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ArclightCaptures.BlockBreakEventContext breakEventContext = ArclightCaptures.popPrimaryBlockBreakEvent();

        if (breakEventContext != null) {
            bridge$handleBlockDrop(breakEventContext, pos);
        }
    }

    @Inject(method = {"tick", "destroyAndAck"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z"))
    public void arclight$clearCaptures(CallbackInfo ci) {
        // clear the event stack in case that interrupted events are left here unhandled
        // it should be a new event capture session each time destroyBlock is called from these two contexts
        ArclightCaptures.clearBlockBreakEventContexts();
    }

    @Override
    public void bridge$handleBlockDrop(ArclightCaptures.BlockBreakEventContext breakEventContext, BlockPos pos) {
        BlockBreakEvent breakEvent = breakEventContext.getEvent();
        List<ItemEntity> blockDrops = breakEventContext.getBlockDrops();
        org.bukkit.block.BlockState state = breakEventContext.getBlockBreakPlayerState();

        if (blockDrops != null && (breakEvent == null || breakEvent.isDropItems())) {
            CraftBlock craftBlock = CraftBlock.at(this.level, pos);
            CraftEventFactory.handleBlockDropItemEvent(craftBlock, state, this.player, blockDrops);
        }
    }

    @Override
    public boolean bridge$isFiredInteract() {
        return firedInteract;
    }

    @Override
    public void bridge$setFiredInteract(boolean b) {
        this.firedInteract = b;
    }

    @Override
    public boolean bridge$getInteractResult() {
        return interactResult;
    }

    @Override
    public void bridge$setInteractResult(boolean b) {
        this.interactResult = b;
    }

    @Override
    public BlockPos bridge$getInteractPosition() {
        return interactPosition;
    }

    @Override
    public InteractionHand bridge$getInteractHand() {
        return interactHand;
    }

    @Override
    public ItemStack bridge$getInteractItemStack() {
        return interactItemStack;
    }

    @Inject(method = "useItemOn", cancellable = true, at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, ordinal = 0, target = "Lnet/minecraft/server/level/ServerPlayerGameMode;gameModeForPlayer:Lnet/minecraft/world/level/GameType;"))
    private void arclight$rightClickBlock(ServerPlayer playerIn, Level worldIn, ItemStack stackIn, InteractionHand handIn, BlockHitResult blockRaytraceResultIn, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPos blockpos = blockRaytraceResultIn.getBlockPos();
        BlockState blockstate = worldIn.getBlockState(blockpos);
        boolean cancelledBlock = false;
        if (this.gameModeForPlayer == GameType.SPECTATOR) {
            MenuProvider provider = blockstate.getMenuProvider(worldIn, blockpos);
            cancelledBlock = !(provider instanceof MenuProvider);
        }
        if (playerIn.getCooldowns().isOnCooldown(stackIn.getItem())) {
            cancelledBlock = true;
        }
        PlayerInteractEvent bukkitEvent = CraftEventFactory.callPlayerInteractEvent(playerIn, Action.RIGHT_CLICK_BLOCK, blockpos, blockRaytraceResultIn.getDirection(), stackIn, cancelledBlock, handIn, blockRaytraceResultIn.getLocation());
        bridge$setFiredInteract(true);
        bridge$setInteractResult(bukkitEvent.useItemInHand() == Event.Result.DENY);
        interactPosition = blockpos.immutable();
        interactHand = handIn;
        interactItemStack = stackIn.copy();
        if (bukkitEvent.useInteractedBlock() == Event.Result.DENY) {
            if (blockstate.getBlock() instanceof DoorBlock) {
                boolean bottom = blockstate.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
                playerIn.connection.send(new ClientboundBlockUpdatePacket(this.level, bottom ? blockpos.above() : blockpos.below()));
            } else if (blockstate.getBlock() instanceof CakeBlock) {
                ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().sendHealthUpdate();
            } else if (stackIn.getItem() instanceof DoubleHighBlockItem) {
                // send a correcting update to the client, as it already placed the upper half of the bisected item
                playerIn.connection.send(new ClientboundBlockUpdatePacket(level, blockpos.relative(blockRaytraceResultIn.getDirection()).above()));
                // send a correcting update to the client for the block above as well, this because of replaceable blocks (such as grass, sea grass etc)
                playerIn.connection.send(new ClientboundBlockUpdatePacket(level, blockpos.above()));
            }
            ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().updateInventory();
            cir.setReturnValue((bukkitEvent.useItemInHand() != Event.Result.ALLOW) ? InteractionResult.SUCCESS : InteractionResult.PASS);
        }
    }

    @Decorate(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;isOnCooldown(Lnet/minecraft/world/item/Item;)Z"))
    private boolean arclight$useInteractResult(ItemCooldowns instance, Item item) throws Throwable {
        var result = (boolean) DecorationOps.callsite().invoke(instance, item);
        DecorationOps.blackhole().invoke(result);
        return interactResult;
    }
}
