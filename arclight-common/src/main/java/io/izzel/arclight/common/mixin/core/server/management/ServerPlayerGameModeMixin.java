package io.izzel.arclight.common.mixin.core.server.management;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.server.management.PlayerInteractionManagerBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockBreakAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin implements PlayerInteractionManagerBridge {

    // @formatter:off
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;
    @Shadow public abstract boolean isCreative();
    @Shadow private GameType gameModeForPlayer;
    @Shadow private int destroyProgressStart;
    @Shadow private int gameTicks;
    @Shadow private boolean isDestroyingBlock;
    @Shadow private BlockPos destroyPos;
    @Shadow private int lastSentState;
    @Shadow private boolean hasDelayedDestroy;
    @Shadow private BlockPos delayedDestroyPos;
    @Shadow private int delayedTickStart;
    @Shadow public abstract void destroyAndAck(BlockPos p_229860_1_, ServerboundPlayerActionPacket.Action p_229860_2_, String p_229860_3_);
    // @formatter:on

    public boolean interactResult = false;
    public boolean firedInteract = false;

    @Inject(method = "changeGameModeForPlayer", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;setGameModeForPlayer(Lnet/minecraft/world/level/GameType;Lnet/minecraft/world/level/GameType;)V"))
    private void arclight$gameModeEvent(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
        PlayerGameModeChangeEvent event = new PlayerGameModeChangeEvent(((ServerPlayerEntityBridge) player).bridge$getBukkitEntity(), GameMode.getByValue(gameType.getId()));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleBlockBreakAction(BlockPos blockPos, ServerboundPlayerActionPacket.Action action, Direction direction, int i) {
        double d0 = this.player.getX() - (blockPos.getX() + 0.5);
        double d2 = this.player.getY() - (blockPos.getY() + 0.5) + 1.5;
        double d3 = this.player.getZ() - (blockPos.getZ() + 0.5);
        double d4 = d0 * d0 + d2 * d2 + d3 * d3;
        double dist = player.getAttribute(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()).getValue() + 1;
        dist *= dist;
        net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock forgeEvent = net.minecraftforge.common.ForgeHooks.onLeftClickBlock(player, blockPos, direction);
        if (forgeEvent.isCanceled() || (!this.isCreative() && forgeEvent.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.DENY)) { // Restore block and te data
            player.connection.send(new ClientboundBlockBreakAckPacket(blockPos, level.getBlockState(blockPos), action, false, "mod canceled"));
            level.sendBlockUpdated(blockPos, level.getBlockState(blockPos), level.getBlockState(blockPos), 3);
            return;
        }
        if (d4 > dist) {
            this.player.connection.send(new ClientboundBlockBreakAckPacket(blockPos, this.level.getBlockState(blockPos), action, false, "too far"));
        } else if (blockPos.getY() >= i) {
            this.player.connection.send(new ClientboundBlockBreakAckPacket(blockPos, this.level.getBlockState(blockPos), action, false, "too high"));
        } else if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            if (!this.level.mayInteract(this.player, blockPos)) {
                CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, blockPos, direction, this.player.getInventory().getSelected(), InteractionHand.MAIN_HAND);
                this.player.connection.send(new ClientboundBlockBreakAckPacket(blockPos, this.level.getBlockState(blockPos), action, false, "may not interact"));
                BlockEntity tileentity = this.level.getBlockEntity(blockPos);
                if (tileentity != null) {
                    this.player.connection.send(tileentity.getUpdatePacket());
                }
                return;
            }
            PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, blockPos, direction, this.player.getInventory().getSelected(), InteractionHand.MAIN_HAND);
            if (event.isCancelled()) {
                this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, blockPos));
                BlockEntity tileentity2 = this.level.getBlockEntity(blockPos);
                if (tileentity2 != null) {
                    this.player.connection.send(tileentity2.getUpdatePacket());
                }
                return;
            }
            if (this.isCreative()) {
                this.destroyAndAck(blockPos, action, "creative destroy");
                return;
            }
            if (this.player.blockActionRestricted(this.level, blockPos, this.gameModeForPlayer)) {
                this.player.connection.send(new ClientboundBlockBreakAckPacket(blockPos, this.level.getBlockState(blockPos), action, false, "block action restricted"));
                return;
            }
            this.destroyProgressStart = this.gameTicks;
            float f = 1.0f;
            BlockState iblockdata = this.level.getBlockState(blockPos);
            if (event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) {
                BlockState data = this.level.getBlockState(blockPos);
                if (data.getBlock() instanceof DoorBlock) {
                    boolean bottom = data.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
                    this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, blockPos));
                    this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, bottom ? blockPos.above() : blockPos.below()));
                } else if (data.getBlock() instanceof TrapDoorBlock) {
                    this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, blockPos));
                }
            } else if (!iblockdata.isAir()) {
                if (forgeEvent.getUseBlock() != net.minecraftforge.eventbus.api.Event.Result.DENY) {
                    iblockdata.attack(this.level, blockPos, this.player);
                }
                f = iblockdata.getDestroyProgress(this.player, this.player.level, blockPos);
            }
            if (event.useItemInHand() == Event.Result.DENY) {
                if (f > 1.0f) {
                    this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, blockPos));
                }
                return;
            }
            BlockDamageEvent blockEvent = CraftEventFactory.callBlockDamageEvent(this.player, blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.player.getInventory().getSelected(), f >= 1.0f);
            if (blockEvent.isCancelled()) {
                this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, blockPos));
                return;
            }
            if (blockEvent.getInstaBreak()) {
                f = 2.0f;
            }
            if (!iblockdata.isAir() && f >= 1.0f) {
                this.destroyAndAck(blockPos, action, "insta mine");
            } else {
                if (this.isDestroyingBlock) {
                    this.player.connection.send(new ClientboundBlockBreakAckPacket(this.destroyPos, this.level.getBlockState(this.destroyPos), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, false, "abort destroying since another started (client insta mine, server disagreed)"));
                }
                this.isDestroyingBlock = true;
                this.destroyPos = blockPos;
                int j = (int) (f * 10.0f);
                this.level.destroyBlockProgress(this.player.getId(), blockPos, j);
                this.player.connection.send(new ClientboundBlockBreakAckPacket(blockPos, this.level.getBlockState(blockPos), action, true, "actual start of destroying"));
                this.lastSentState = j;
            }
        } else if (action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
            if (blockPos.equals(this.destroyPos)) {
                int k = this.gameTicks - this.destroyProgressStart;
                BlockState iblockdata = this.level.getBlockState(blockPos);
                if (!iblockdata.isAir()) {
                    float f2 = iblockdata.getDestroyProgress(this.player, this.player.level, blockPos) * (k + 1);
                    if (f2 >= 0.7f) {
                        this.isDestroyingBlock = false;
                        this.level.destroyBlockProgress(this.player.getId(), blockPos, -1);
                        this.destroyAndAck(blockPos, action, "destroyed");
                        return;
                    }
                    if (!this.hasDelayedDestroy) {
                        this.isDestroyingBlock = false;
                        this.hasDelayedDestroy = true;
                        this.delayedDestroyPos = blockPos;
                        this.delayedTickStart = this.destroyProgressStart;
                    }
                }
            }
            this.player.connection.send(new ClientboundBlockBreakAckPacket(blockPos, this.level.getBlockState(blockPos), action, true, "stopped destroying"));
        } else if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
            this.isDestroyingBlock = false;
            if (!Objects.equals(this.destroyPos, blockPos)) {
                ArclightMod.LOGGER.debug("Mismatch in destroy block pos: " + this.destroyPos + " " + blockPos);
                this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                this.player.connection.send(new ClientboundBlockBreakAckPacket(this.destroyPos, this.level.getBlockState(this.destroyPos), action, true, "aborted mismatched destroying"));
            }
            this.level.destroyBlockProgress(this.player.getId(), blockPos, -1);
            this.player.connection.send(new ClientboundBlockBreakAckPacket(blockPos, this.level.getBlockState(blockPos), action, true, "aborted destroying"));
        }
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    public void arclight$resetBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        List<ItemEntity> blockDrops = ArclightCaptures.getBlockDrops();
        org.bukkit.block.BlockState state = ArclightCaptures.getBlockBreakPlayerState();
        BlockBreakEvent breakEvent = ArclightCaptures.resetBlockBreakPlayer();
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

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public InteractionResult useItemOn(ServerPlayer playerIn, Level worldIn, ItemStack stackIn, InteractionHand handIn, BlockHitResult blockRaytraceResultIn) {
        BlockPos blockpos = blockRaytraceResultIn.getBlockPos();
        BlockState blockstate = worldIn.getBlockState(blockpos);
        InteractionResult resultType = InteractionResult.PASS;
        boolean cancelledBlock = false;
        if (this.gameModeForPlayer == GameType.SPECTATOR) {
            MenuProvider provider = blockstate.getMenuProvider(worldIn, blockpos);
            cancelledBlock = !(provider instanceof MenuProvider);
        }
        if (playerIn.getCooldowns().isOnCooldown(stackIn.getItem())) {
            cancelledBlock = true;
        }

        PlayerInteractEvent bukkitEvent = CraftEventFactory.callPlayerInteractEvent(playerIn, Action.RIGHT_CLICK_BLOCK, blockpos, blockRaytraceResultIn.getDirection(), stackIn, cancelledBlock, handIn);
        bridge$setFiredInteract(true);
        bridge$setInteractResult(bukkitEvent.useItemInHand() == Event.Result.DENY);
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
            resultType = ((bukkitEvent.useItemInHand() != Event.Result.ALLOW) ? InteractionResult.SUCCESS : InteractionResult.PASS);
        } else if (this.gameModeForPlayer == GameType.SPECTATOR) {
            MenuProvider inamedcontainerprovider = blockstate.getMenuProvider(worldIn, blockpos);
            if (inamedcontainerprovider != null) {
                playerIn.openMenu(inamedcontainerprovider);
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.PASS;
            }
        } else {
            net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock(playerIn, handIn, blockpos, blockRaytraceResultIn);
            if (event.isCanceled()) return event.getCancellationResult();
            UseOnContext itemusecontext = new UseOnContext(playerIn, handIn, blockRaytraceResultIn);
            if (event.getUseItem() != net.minecraftforge.eventbus.api.Event.Result.DENY) {
                InteractionResult result = stackIn.onItemUseFirst(itemusecontext);
                if (result != InteractionResult.PASS) return result;
            }
            boolean flag = !playerIn.getMainHandItem().isEmpty() || !playerIn.getOffhandItem().isEmpty();
            boolean flag1 = (playerIn.isSecondaryUseActive() && flag) && !(playerIn.getMainHandItem().doesSneakBypassUse(worldIn, blockpos, playerIn) && playerIn.getOffhandItem().doesSneakBypassUse(worldIn, blockpos, playerIn));
            ItemStack itemstack = stackIn.copy();
            if (event.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || (event.getUseBlock() != net.minecraftforge.eventbus.api.Event.Result.DENY && !flag1)) {
                resultType = blockstate.use(worldIn, playerIn, handIn, blockRaytraceResultIn);
                if (resultType.consumesAction()) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(playerIn, blockpos, itemstack);
                    return resultType;
                }
            }
            if (event.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || (!stackIn.isEmpty() && resultType != InteractionResult.SUCCESS && !bridge$getInteractResult())) {
                if (event.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.DENY) {
                    return InteractionResult.PASS;
                }
                if (this.isCreative()) {
                    int i = stackIn.getCount();
                    resultType = stackIn.useOn(itemusecontext);
                    stackIn.setCount(i);
                } else {
                    resultType = stackIn.useOn(itemusecontext);
                }

                if (resultType.consumesAction()) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(playerIn, blockpos, itemstack);
                }
                return resultType;
            } else {
                return resultType;
            }
        }
        return resultType;
    }
}
