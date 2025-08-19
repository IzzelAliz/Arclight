package io.izzel.arclight.forge.mixin.core.server.management;

import io.izzel.arclight.common.bridge.core.server.management.PlayerInteractionManagerBridge;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.Action;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin_Forge implements PlayerInteractionManagerBridge {

    @Shadow @Final protected ServerPlayer player;

    @Shadow protected ServerLevel level;

    @Inject(method = "destroyBlock", remap = true, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/common/ForgeHooks;onBlockBreakEvent(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/GameType;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;)I"))
    public void arclight$beforePrimaryEventFired(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ArclightCaptures.captureNextBlockBreakEventAsPrimaryEvent();
    }

    @Inject(method = "destroyBlock", remap = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, remap = false, target = "Lnet/minecraftforge/common/ForgeHooks;onBlockBreakEvent(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/GameType;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;)I"))
    public void arclight$handleSecondaryBlockBreakEvents(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ArclightCaptures.BlockBreakEventContext breakEventContext = ArclightCaptures.popSecondaryBlockBreakEvent();
        while (breakEventContext != null) {
            Block block = breakEventContext.getEvent().getBlock();
            bridge$handleBlockDrop(breakEventContext, new BlockPos(block.getX(), block.getY(), block.getZ()));
            breakEventContext = ArclightCaptures.popSecondaryBlockBreakEvent();
        }
    }

    @Decorate(method = "handleBlockBreakAction", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/event/ForgeEventFactory;onLeftClickBlock(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;)Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$LeftClickBlock;"))
    private PlayerInteractEvent.LeftClickBlock arclight$callInteractIfCancelled(Player player, BlockPos pos, Direction face, ServerboundPlayerActionPacket.Action action) throws Throwable {
        final var event = (PlayerInteractEvent.LeftClickBlock) DecorationOps.callsite().invoke(player, pos, face, action);
        if (event.isCanceled() || (!player.isCreative() && event.getResult().isDenied())) {
            // Arclight: we need to fire it anyway since they should at least know about it
            switch (action) {
                case START_DESTROY_BLOCK -> {
                    ArclightCaptures.cancelNextPlayerInteract();
                    CraftEventFactory.callPlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, pos, face, player.getInventory().getSelected(), true, InteractionHand.MAIN_HAND, null);
                }
                // Arclight: Returning directly from the method is really a rude decision, don't know how to handle now...
                case ABORT_DESTROY_BLOCK -> CraftEventFactory.callBlockDamageAbortEvent(this.player, pos, player.getInventory().getSelected());
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
