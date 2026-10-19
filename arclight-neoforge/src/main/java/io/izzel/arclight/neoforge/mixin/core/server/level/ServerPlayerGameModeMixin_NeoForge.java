package io.izzel.arclight.neoforge.mixin.core.server.level;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerGameModeBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.Action;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin_NeoForge implements ServerPlayerGameModeBridge {

    @Shadow protected ServerLevel level;

    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "destroyBlock", remap = true, at = @At(value = "INVOKE", remap = false, target = "Lnet/neoforged/neoforge/common/CommonHooks;fireBlockBreak(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/GameType;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/neoforged/neoforge/event/level/block/BreakBlockEvent;"))
    private void arclight$beforePrimaryEventFired(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ArclightCaptures.captureNextBlockBreakEventAsPrimaryEvent();
    }

    @Inject(method = "destroyBlock", remap = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, remap = false, target = "Lnet/neoforged/neoforge/common/CommonHooks;fireBlockBreak(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/GameType;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/neoforged/neoforge/event/level/block/BreakBlockEvent;"))
    private void arclight$handleSecondaryBlockBreakEvents(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ArclightCaptures.BlockBreakEventContext breakEventContext = ArclightCaptures.popSecondaryBlockBreakEvent();
        while (breakEventContext != null) {
            Block block = breakEventContext.getEvent().getBlock();
            bridge$handleBlockDrop(breakEventContext, new BlockPos(block.getX(), block.getY(), block.getZ()));
            breakEventContext = ArclightCaptures.popSecondaryBlockBreakEvent();
        }
    }

    // The common Bukkit event fires at the spectator FIELD, before Neo's first-use call.
    // A mod's TriState.FALSE still prevents the invocation, and Bukkit item DENY
    // must not suppress a later legitimate item ALLOW when the block was denied.
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;onItemUseFirst(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult arclight$denyFirstItemUse(ItemStack stack, UseOnContext context,
                                                        Operation<InteractionResult> original, @Share(value = "arclight$denyItem", namespace = "arclight:useItemOn") LocalBooleanRef deniedItem) {
        return deniedItem.get() ? InteractionResult.PASS : original.call(stack, context);
    }

    @Decorate(method = "handleBlockBreakAction", at = @At(value = "INVOKE", remap = false, target = "Lnet/neoforged/neoforge/common/CommonHooks;onLeftClickBlock(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;)Lnet/neoforged/neoforge/event/entity/player/PlayerInteractEvent$LeftClickBlock;"))
    private PlayerInteractEvent.LeftClickBlock arclight$callInteractIfCancelled(Player player, BlockPos pos, Direction face, ServerboundPlayerActionPacket.Action action) throws Throwable {
        final var event = (PlayerInteractEvent.LeftClickBlock) DecorationOps.callsite().invoke(player, pos, face, action);
        // NeoForge doesn't care about the item is being used or not, only whether it's cancelled
        if (event.isCanceled()) {
            // Arclight: we need to fire it anyway since they should at least know about it
            switch (action) {
                case START_DESTROY_BLOCK -> {
                    ArclightCaptures.cancelNextPlayerInteract();
                    CraftEventFactory.callPlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, pos, face, player.getInventory().getSelectedItem(), true, InteractionHand.MAIN_HAND, null);
                }
                // Arclight: Returning directly from the method is really a rude decision, don't know how to handle now...
                case ABORT_DESTROY_BLOCK -> CraftEventFactory.callBlockDamageAbortEvent(this.player, pos, player.getInventory().getSelectedItem());
            }

            this.player.connection.send(new ClientboundBlockUpdatePacket(this.level, pos));
            BlockEntity blockEntity = this.level.getBlockEntity(pos);
            if (blockEntity != null) {
                // FIXME: Oops, this might be null!
                this.player.connection.send(blockEntity.getUpdatePacket());
            }
        }
        return event;
    }
}
