package io.izzel.arclight.common.mixin.core.network;

import com.mojang.brigadier.ParseResults;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.core.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.network.play.TimestampedPacket;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.server.management.PlayerInteractionManagerBridge;
import io.izzel.arclight.common.bridge.core.server.management.PlayerListBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.server.RunnableInPlace;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.FutureChain;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandException;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftItemType;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.craftbukkit.v.util.LazyPlayerSet;
import org.bukkit.craftbukkit.v.util.Waitable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.SmithingInventory;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.logging.Level;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetHandlerMixin extends ServerCommonPacketListenerImplMixin implements ServerPlayNetHandlerBridge {

    // @formatter:off
    @Shadow public ServerPlayer player;
    @Shadow private Entity lastVehicle;
    @Shadow private double vehicleFirstGoodX;
    @Shadow private double vehicleFirstGoodY;
    @Shadow private double vehicleFirstGoodZ;
    @Shadow private double vehicleLastGoodX;
    @Shadow private double vehicleLastGoodY;
    @Shadow private double vehicleLastGoodZ;
    @Shadow private boolean clientVehicleIsFloating;
    @Shadow private int receivedMovePacketCount;
    @Shadow private int knownMovePacketCount;
    @Shadow private Vec3 awaitingPositionFromClient;
    @Shadow private int tickCount;
    @Shadow public abstract void resetPosition();
    @Shadow private int awaitingTeleportTime;
    @Shadow public abstract void teleport(double x, double y, double z, float yaw, float pitch);
    @Shadow private double firstGoodX;
    @Shadow private double firstGoodY;
    @Shadow private double firstGoodZ;
    @Shadow @Final private static Logger LOGGER;
    @Shadow private double lastGoodX;
    @Shadow private double lastGoodY;
    @Shadow private double lastGoodZ;
    @Shadow private boolean clientIsFloating;
    @Shadow private int awaitingTeleport;
    @Shadow private int chatSpamTickCount;
    @Shadow private int dropSpamTickCount;
    @Shadow protected abstract boolean noBlocksAround(Entity p_241162_1_);
    @Shadow private static double clampHorizontal(double p_143610_) { return 0; }
    @Shadow private static double clampVertical(double p_143654_) { return 0; }
    @Shadow private static boolean containsInvalidValues(double p_143664_, double p_143665_, double p_143666_, float p_143667_, float p_143668_) { return false; }
    @Shadow @Final @Mutable private FutureChain chatMessageChain;
    @Shadow public abstract void ackBlockChangesUpTo(int p_215202_);
    @Shadow private static boolean isChatMessageIllegal(String p_215215_) { return false; }
    @Shadow protected abstract CompletableFuture<FilteredText> filterTextPacket(String p_243213_);
    @Shadow protected abstract ParseResults<CommandSourceStack> parseCommand(String p_242938_);
    @Shadow protected abstract void detectRateSpam();
    @Shadow protected abstract PlayerChatMessage getSignedMessage(ServerboundChatPacket p_251061_, LastSeenMessages p_250566_) throws SignedMessageChain.DecodeException;
    @Shadow protected abstract void handleMessageDecodeFailure(SignedMessageChain.DecodeException p_252068_);
    @Shadow protected abstract boolean isPlayerCollidingWithAnythingNew(LevelReader p_289008_, AABB p_288986_, double p_288990_, double p_288991_, double p_288967_);
    @Shadow protected abstract boolean updateAwaitingTeleport();
    @Shadow protected abstract Filterable<String> filterableFromOutgoing(FilteredText filteredText);
    @Shadow protected abstract Optional<LastSeenMessages> unpackAndApplyLastSeen(LastSeenMessages.Update update);
    @Shadow protected abstract void tryHandleChat(String string, Runnable runnable);
    @Shadow protected abstract <S> Map<String, PlayerChatMessage> collectSignedArguments(ServerboundChatCommandSignedPacket serverboundChatCommandSignedPacket, SignableCommand<S> signableCommand, LastSeenMessages lastSeenMessages) throws SignedMessageChain.DecodeException;
    @Shadow public abstract void sendDisguisedChatMessage(Component component, ChatType.Bound bound);
    @Shadow public abstract void teleport(double d, double e, double f, float g, float h, Set<RelativeMovement> set);
    // @formatter:on

    private static final int SURVIVAL_PLACE_DISTANCE_SQUARED = 6 * 6;
    private static final int CREATIVE_PLACE_DISTANCE_SQUARED = 7 * 7;
    private int allowedPlayerTicks;
    private int dropCount;
    private int lastTick;
    private volatile int lastBookTick;
    private int lastDropTick;

    private double lastPosX;
    private double lastPosY;
    private double lastPosZ;
    private float lastPitch;
    private float lastYaw;
    private boolean justTeleported;
    private boolean hasMoved;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(MinecraftServer server, Connection networkManagerIn, ServerPlayer playerIn, CommonListenerCookie cookie, CallbackInfo ci) {
        allowedPlayerTicks = 1;
        dropCount = 0;
        lastPosX = Double.MAX_VALUE;
        lastPosY = Double.MAX_VALUE;
        lastPosZ = Double.MAX_VALUE;
        lastPitch = Float.MAX_VALUE;
        lastYaw = Float.MAX_VALUE;
        justTeleported = false;
        this.chatMessageChain = new FutureChain(ArclightServer.getChatExecutor());
        bridge$setPlayer(playerIn);
    }

    @Inject(method = "onDisconnect", cancellable = true, at = @At("HEAD"))
    private void arclight$returnIfProcessed(DisconnectionDetails disconnectionDetails, CallbackInfo ci) {
        if (processedDisconnect) {
            ci.cancel();
        } else {
            processedDisconnect = true;
        }
    }

    @Redirect(method = "removePlayerFromWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    public void arclight$captureQuit(PlayerList instance, Component p_240618_, boolean p_240644_) {
        // do nothing
    }

    @Inject(method = "removePlayerFromWorld", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/players/PlayerList;remove(Lnet/minecraft/server/level/ServerPlayer;)V"))
    public void arclight$processQuit(CallbackInfo ci) {
        String quitMessage = ArclightCaptures.getQuitMessage();
        if (quitMessage != null && quitMessage.length() > 0) {
            ((PlayerListBridge) this.server.getPlayerList()).bridge$sendMessage(CraftChatMessage.fromString(quitMessage));
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleMoveVehicle(final ServerboundMoveVehiclePacket packetplayinvehiclemove) {
        PacketUtils.ensureRunningOnSameThread(packetplayinvehiclemove, (ServerGamePacketListenerImpl) (Object) this, this.player.serverLevel());
        if (containsInvalidValues(packetplayinvehiclemove.getX(), packetplayinvehiclemove.getY(), packetplayinvehiclemove.getZ(), packetplayinvehiclemove.getYRot(), packetplayinvehiclemove.getXRot())) {
            this.disconnect(Component.translatable("multiplayer.disconnect.invalid_vehicle_movement"));
        } else if (!this.updateAwaitingTeleport()) {
            Entity entity = this.player.getRootVehicle();
            if (entity != this.player && entity.getControllingPassenger() == this.player && entity == this.lastVehicle) {
                ServerLevel worldserver = this.player.serverLevel();

                // CraftBukkit - store current player position
                double prevX = player.getX();
                double prevY = player.getY();
                double prevZ = player.getZ();
                float prevYaw = player.getYRot();
                float prevPitch = player.getXRot();
                // CraftBukkit end

                double d0 = entity.getX();
                double d2 = entity.getY();
                double d3 = entity.getZ();
                double d4 = packetplayinvehiclemove.getX();
                double d5 = packetplayinvehiclemove.getY();
                double d6 = packetplayinvehiclemove.getZ();
                float f = packetplayinvehiclemove.getYRot();
                float f2 = packetplayinvehiclemove.getXRot();
                double d7 = d4 - this.vehicleFirstGoodX;
                double d8 = d5 - this.vehicleFirstGoodY;
                double d9 = d6 - this.vehicleFirstGoodZ;
                double d10 = entity.getDeltaMovement().lengthSqr();
                double d11 = d7 * d7 + d8 * d8 + d9 * d9;
                this.allowedPlayerTicks += (int) (System.currentTimeMillis() / 50L - this.lastTick);
                this.allowedPlayerTicks = Math.max(this.allowedPlayerTicks, 1);
                this.lastTick = (int) (System.currentTimeMillis() / 50L);
                ++this.receivedMovePacketCount;
                int i = this.receivedMovePacketCount - this.knownMovePacketCount;
                if (i > Math.max(this.allowedPlayerTicks, 5)) {
                    LOGGER.debug(this.player.getScoreboardName() + " is sending move packets too frequently (" + i + " packets since last tick)");
                    i = 1;
                }
                if (d11 > 0.0) {
                    --this.allowedPlayerTicks;
                } else {
                    this.allowedPlayerTicks = 20;
                }
                double speed;
                if (this.player.getAbilities().flying) {
                    speed = this.player.getAbilities().flyingSpeed * 20.0f;
                } else {
                    speed = this.player.getAbilities().walkingSpeed * 10.0f;
                }
                speed *= 2.0;
                if (d11 - d10 > Math.max(100.0, Math.pow(10.0f * i * speed, 2.0)) && !this.isSingleplayerOwner()) {
                    LOGGER.warn("{} (vehicle of {}) moved too quickly! {},{},{}", entity.getName().getString(), this.player.getName().getString(), d7, d8, d9);
                    this.send(new ClientboundMoveVehiclePacket(entity));
                    return;
                }
                boolean flag = worldserver.noCollision(entity, entity.getBoundingBox().deflate(0.0625));
                d7 = d4 - this.vehicleLastGoodX;
                d8 = d5 - this.vehicleLastGoodY - 1.0E-6;
                d9 = d6 - this.vehicleLastGoodZ;
                boolean flag1 = entity.verticalCollisionBelow;

                if (entity instanceof LivingEntity entityliving) {
                    if (entityliving.onClimbable()) {
                        entityliving.resetFallDistance();
                    }
                }

                entity.move(MoverType.PLAYER, new Vec3(d7, d8, d9));
                double d12 = d8;
                d7 = d4 - entity.getX();
                d8 = d5 - entity.getY();
                if (d8 > -0.5 || d8 < 0.5) {
                    d8 = 0.0;
                }
                d9 = d6 - entity.getZ();
                d11 = d7 * d7 + d8 * d8 + d9 * d9;
                boolean flag2 = false;
                if (d11 > SpigotConfig.movedWronglyThreshold) {
                    flag2 = true;
                    LOGGER.warn("{} (vehicle of {}) moved wrongly! {}", entity.getName().getString(), this.player.getName().getString(), Math.sqrt(d11));
                }
                Location curPos = this.getCraftPlayer().getLocation();
                entity.absMoveTo(d4, d5, d6, f, f2);
                this.player.absMoveTo(d4, d5, d6, this.player.getYRot(), this.player.getXRot());
                boolean flag3 = worldserver.noCollision(entity, entity.getBoundingBox().deflate(0.0625));
                if (flag && (flag2 || !flag3)) {
                    entity.absMoveTo(d0, d2, d3, f, f2);
                    this.player.absMoveTo(d0, d2, d3, this.player.getYRot(), this.player.getXRot());
                    this.send(new ClientboundMoveVehiclePacket(entity));
                    return;
                }
                Player player = this.getCraftPlayer();
                if (!hasMoved) {
                    this.lastPosX = prevX;
                    this.lastPosY = prevY;
                    this.lastPosZ = prevZ;
                    this.lastYaw = prevYaw;
                    this.lastPitch = prevPitch;
                    this.hasMoved = true;
                }
                Location from = new Location(player.getWorld(), this.lastPosX, this.lastPosY, this.lastPosZ, this.lastYaw, this.lastPitch);
                Location to = player.getLocation().clone();
                to.setX(packetplayinvehiclemove.getX());
                to.setY(packetplayinvehiclemove.getY());
                to.setZ(packetplayinvehiclemove.getZ());
                to.setYaw(packetplayinvehiclemove.getYRot());
                to.setPitch(packetplayinvehiclemove.getXRot());
                double delta = Math.pow(this.lastPosX - to.getX(), 2.0) + Math.pow(this.lastPosY - to.getY(), 2.0) + Math.pow(this.lastPosZ - to.getZ(), 2.0);
                float deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());
                if ((delta > 0.00390625 || deltaAngle > 10.0f) && !((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
                    this.lastPosX = to.getX();
                    this.lastPosY = to.getY();
                    this.lastPosZ = to.getZ();
                    this.lastYaw = to.getYaw();
                    this.lastPitch = to.getPitch();
                    Location oldTo = to.clone();
                    PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
                    this.cserver.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        this.bridge$teleport(from);
                        return;
                    }
                    if (!oldTo.equals(event.getTo()) && !event.isCancelled()) {
                        ((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                        return;
                    }
                    if (!from.equals(this.getCraftPlayer().getLocation()) && this.justTeleported) {
                        this.justTeleported = false;
                        return;
                    }
                }
                this.player.serverLevel().getChunkSource().move(this.player);
                Vec3 vec3d = new Vec3(entity.getX() - d0, entity.getY() - d2, entity.getZ() - d3);
                this.player.setKnownMovement(vec3d);
                this.player.checkMovementStatistics(vec3d.x, vec3d.y, vec3d.z);
                this.clientVehicleIsFloating = d11 >= -0.03125D && !flag1 && !this.server.isFlightAllowed() && !entity.isNoGravity() && this.noBlocksAround(entity);
                this.vehicleLastGoodX = entity.getX();
                this.vehicleLastGoodY = entity.getY();
                this.vehicleLastGoodZ = entity.getZ();
            }
        }
    }

    @Inject(method = "handleAcceptTeleportPacket",
        at = @At(value = "FIELD", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;awaitingPositionFromClient:Lnet/minecraft/world/phys/Vec3;"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isChangingDimension()Z")))
    private void arclight$updateLoc(ServerboundAcceptTeleportationPacket packetIn, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) this.player).bridge$isValid()) {
            this.player.serverLevel().getChunkSource().move(this.player);
        }
    }

    @Inject(method = "handleAcceptTeleportPacket", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;awaitingTeleport:I"))
    private void arclight$confirm(ServerboundAcceptTeleportationPacket packetIn, CallbackInfo ci) {
        if (this.awaitingPositionFromClient == null) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSelectTrade", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/MerchantMenu;setSelectionHint(I)V"))
    private void arclight$tradeSelect(ServerboundSelectTradePacket serverboundSelectTradePacket, CallbackInfo ci, int i) {
        var event = CraftEventFactory.callTradeSelectEvent(this.player, i, (MerchantMenu) this.player.containerMenu);
        if (event.isCancelled()) {
            ((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity().updateInventory();
            ci.cancel();
        }
    }

    @Inject(method = "handleEditBook", at = @At("HEAD"))
    private void arclight$editBookSpam(ServerboundEditBookPacket packetIn, CallbackInfo ci) {
        if (this.lastBookTick == 0) {
            this.lastBookTick = ArclightConstants.currentTick - 20;
        }
        if (this.lastBookTick + 20 > ArclightConstants.currentTick) {
            this.bridge$disconnect("Book edited too quickly!");
            return;
        }
        this.lastBookTick = ArclightConstants.currentTick;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void updateBookContents(List<FilteredText> list, int slot) {
        ItemStack handItem = this.player.getInventory().getItem(slot);
        ItemStack itemStack = handItem.copy();
        if (itemStack.is(Items.WRITABLE_BOOK)) {
            List<Filterable<String>> list2 = list.stream().map(this::filterableFromOutgoing).toList();
            itemStack.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(list2));
            CraftEventFactory.handleEditBookEvent(player, slot, handItem, itemStack); // CraftBukkit
        }
    }

    @Decorate(method = "signBook", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$editBookEvent(Inventory instance, int i, ItemStack stack, FilteredText text, List<FilteredText> list, int slot, @Local(ordinal = 0) ItemStack handStack) throws Throwable {
        CraftEventFactory.handleEditBookEvent(player, i, handStack, stack);
        DecorationOps.callsite().invoke(instance, i, handStack);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleMovePlayer(ServerboundMovePlayerPacket packetplayinflying) {
        PacketUtils.ensureRunningOnSameThread(packetplayinflying, (ServerGamePacketListenerImpl) (Object) this, this.player.serverLevel());
        if (containsInvalidValues(packetplayinflying.getX(0.0D), packetplayinflying.getY(0.0D), packetplayinflying.getZ(0.0D), packetplayinflying.getYRot(0.0F), packetplayinflying.getXRot(0.0F))) {
            this.disconnect(Component.translatable("multiplayer.disconnect.invalid_player_movement"));
        } else {
            ServerLevel worldserver = this.player.serverLevel();
            if (!this.player.wonGame && !((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
                if (this.tickCount == 0) {
                    this.resetPosition();
                }
                if (!this.updateAwaitingTeleport()) {
                    this.awaitingTeleportTime = this.tickCount;
                    double d0 = clampHorizontal(packetplayinflying.getX(this.player.getX()));
                    double d1 = clampVertical(packetplayinflying.getY(this.player.getY()));
                    double d2 = clampHorizontal(packetplayinflying.getZ(this.player.getZ()));
                    float f = Mth.wrapDegrees(packetplayinflying.getYRot(this.player.getYRot()));
                    float f1 = Mth.wrapDegrees(packetplayinflying.getXRot(this.player.getXRot()));

                    if (this.player.isPassenger()) {
                        this.player.absMoveTo(this.player.getX(), this.player.getY(), this.player.getZ(), f, f1);
                        this.player.serverLevel().getChunkSource().move(this.player);
                        this.allowedPlayerTicks = 20; // CraftBukkit
                    } else {
                        // CraftBukkit - Make sure the move is valid but then reset it for plugins to modify
                        double prevX = player.getX();
                        double prevY = player.getY();
                        double prevZ = player.getZ();
                        float prevYaw = player.getYRot();
                        float prevPitch = player.getXRot();
                        // CraftBukkit end
                        double d3 = this.player.getX();
                        double d4 = this.player.getY();
                        double d5 = this.player.getZ();
                        double d6 = this.player.getY();
                        double d7 = d0 - this.firstGoodX;
                        double d8 = d1 - this.firstGoodY;
                        double d9 = d2 - this.firstGoodZ;
                        double d10 = this.player.getDeltaMovement().lengthSqr();
                        double d11 = d7 * d7 + d8 * d8 + d9 * d9;

                        if (this.player.isSleeping()) {
                            if (d11 > 1.0D) {
                                this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), f, f1);
                            }

                        } else {
                            boolean fallFlying = this.player.isFallFlying();
                            if (worldserver.tickRateManager().runsNormally()) {
                                ++this.receivedMovePacketCount;
                                int i = this.receivedMovePacketCount - this.knownMovePacketCount;

                                // CraftBukkit start - handle custom speeds and skipped ticks
                                this.allowedPlayerTicks += (System.currentTimeMillis() / 50) - this.lastTick;
                                this.allowedPlayerTicks = Math.max(this.allowedPlayerTicks, 1);
                                this.lastTick = (int) (System.currentTimeMillis() / 50);

                                if (i > Math.max(this.allowedPlayerTicks, 5)) {
                                    LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)", this.player.getName().getString(), i);
                                    i = 1;
                                }

                                if (packetplayinflying.hasRot || d11 > 0) {
                                    allowedPlayerTicks -= 1;
                                } else {
                                    allowedPlayerTicks = 20;
                                }
                                double speed;
                                if (player.getAbilities().flying) {
                                    speed = player.getAbilities().flyingSpeed * 20f;
                                } else {
                                    speed = player.getAbilities().walkingSpeed * 10f;
                                }

                                if (!this.player.isChangingDimension() && (!this.player.serverLevel().getGameRules().getBoolean(GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK) || !this.player.isFallFlying())) {
                                    float f2 = this.player.isFallFlying() ? 300.0F : 100.0F;

                                    if (d11 - d10 > Math.max(f2, Math.pow((double) (org.spigotmc.SpigotConfig.movedTooQuicklyMultiplier * (float) i * speed), 2)) && !this.isSingleplayerOwner()) {
                                        // CraftBukkit end
                                        LOGGER.warn("{} moved too quickly! {},{},{}", this.player.getName().getString(), d7, d8, d9);
                                        this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot());
                                        return;
                                    }
                                }
                            }

                            AABB axisalignedbb = this.player.getBoundingBox();

                            d7 = d0 - this.lastGoodX;
                            d8 = d1 - this.lastGoodY;
                            d9 = d2 - this.lastGoodZ;
                            boolean flag = d8 > 0.0D;

                            if (this.player.onGround() && !packetplayinflying.isOnGround() && flag) {
                                this.player.jumpFromGround();
                            }

                            this.player.move(MoverType.PLAYER, new Vec3(d7, d8, d9));
                            this.player.onGround = packetplayinflying.isOnGround();
                            double d12 = d8;

                            d7 = d0 - this.player.getX();
                            d8 = d1 - this.player.getY();
                            if (d8 > -0.5D || d8 < 0.5D) {
                                d8 = 0.0D;
                            }

                            d9 = d2 - this.player.getZ();
                            d11 = d7 * d7 + d8 * d8 + d9 * d9;
                            boolean flag1 = false;

                            if (!this.player.isChangingDimension() && d11 > org.spigotmc.SpigotConfig.movedWronglyThreshold && !this.player.isSleeping() && !this.player.gameMode.isCreative() && this.player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) { // Spigot
                                flag1 = true;
                                LOGGER.warn("{} moved wrongly!", this.player.getName().getString());
                            }

                            if (!this.player.noPhysics && !this.player.isSleeping() && (flag1 && worldserver.noCollision(this.player, axisalignedbb) || this.isPlayerCollidingWithAnythingNew(worldserver, axisalignedbb, d0, d1, d2))) {
                                this.bridge$pushNoTeleportEvent();
                                this.teleport(d3, d4, d5, f, f1, Collections.emptySet()); // CraftBukkit - SPIGOT-1807: Don't call teleport event, when the client thinks the player is falling, because the chunks are not loaded on the client yet.
                                this.player.doCheckFallDamage(this.player.getX() - d3, this.player.getY() - d4, this.player.getZ() - d5, packetplayinflying.isOnGround());
                            } else {
                                // Reset to old location first
                                this.player.absMoveTo(prevX, prevY, prevZ, prevYaw, prevPitch);
                                CraftPlayer player = this.getCraftPlayer();
                                if (!this.hasMoved) {
                                    this.lastPosX = prevX;
                                    this.lastPosY = prevY;
                                    this.lastPosZ = prevZ;
                                    this.lastYaw = prevYaw;
                                    this.lastPitch = prevPitch;
                                    this.hasMoved = true;
                                }
                                Location from = new Location(player.getWorld(), this.lastPosX, this.lastPosY, this.lastPosZ, this.lastYaw, this.lastPitch);
                                Location to = player.getLocation().clone();
                                if (packetplayinflying.hasPos) {
                                    to.setX(packetplayinflying.x);
                                    to.setY(packetplayinflying.y);
                                    to.setZ(packetplayinflying.z);
                                }
                                if (packetplayinflying.hasRot) {
                                    to.setYaw(packetplayinflying.yRot);
                                    to.setPitch(packetplayinflying.xRot);
                                }
                                double delta = Math.pow(this.lastPosX - to.getX(), 2.0) + Math.pow(this.lastPosY - to.getY(), 2.0) + Math.pow(this.lastPosZ - to.getZ(), 2.0);
                                float deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());
                                if ((delta > 1f / 256 || deltaAngle > 10f) && !((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
                                    this.lastPosX = to.getX();
                                    this.lastPosY = to.getY();
                                    this.lastPosZ = to.getZ();
                                    this.lastYaw = to.getYaw();
                                    this.lastPitch = to.getPitch();
                                    Location oldTo = to.clone();
                                    PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
                                    this.cserver.getPluginManager().callEvent(event);
                                    if (event.isCancelled()) {
                                        this.teleport(from);
                                        return;
                                    }
                                    if (!oldTo.equals(event.getTo()) && !event.isCancelled()) {
                                        getCraftPlayer().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                                        return;
                                    }
                                    if (!from.equals(this.getCraftPlayer().getLocation()) && this.justTeleported) {
                                        this.justTeleported = false;
                                        return;
                                    }
                                }

                                this.player.absMoveTo(d0, d1, d2, f, f1); // Copied from above
                                boolean autoSpinAttack = this.player.isAutoSpinAttack();
                                this.clientIsFloating = d12 >= -0.03125D
                                    && this.player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR
                                    && !this.server.isFlightAllowed()
                                    && !(this.player.getAbilities().mayfly || ((ServerPlayerEntityBridge) this.player).bridge$platform$mayfly())
                                    && !this.player.hasEffect(MobEffects.LEVITATION)
                                    && !fallFlying
                                    && !autoSpinAttack
                                    && this.noBlocksAround(this.player);
                                // CraftBukkit end
                                this.player.serverLevel().getChunkSource().move(this.player);
                                var vec3 = new Vec3(this.player.getX() - d3, this.player.getY() - d4, this.player.getZ() - d5);
                                this.player.setOnGroundWithMovement(packetplayinflying.isOnGround(), vec3);
                                this.player.doCheckFallDamage(this.player.getX() - d3, this.player.getY() - d4, this.player.getZ() - d5, packetplayinflying.isOnGround());
                                this.player.setKnownMovement(vec3);
                                if (flag) {
                                    this.player.resetFallDistance();
                                }

                                if (packetplayinflying.isOnGround() || this.player.hasLandedInLiquid() || this.player.onClimbable() || this.player.isSpectator() || fallFlying || autoSpinAttack) {
                                    this.player.tryResetCurrentImpulseContext();
                                }

                                this.player.checkMovementStatistics(this.player.getX() - d3, this.player.getY() - d4, this.player.getZ() - d5);
                                this.lastGoodX = this.player.getX();
                                this.lastGoodY = this.player.getY();
                                this.lastGoodZ = this.player.getZ();
                            }
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "updateAwaitingTeleport", at = @At("RETURN"))
    private void arclight$setAllowedTicks(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            this.allowedPlayerTicks = 20;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handlePlayerAction(ServerboundPlayerActionPacket packetplayinblockdig) {
        PacketUtils.ensureRunningOnSameThread(packetplayinblockdig, (ServerGamePacketListenerImpl) (Object) this, this.player.serverLevel());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        BlockPos blockposition = packetplayinblockdig.getPos();
        this.player.resetLastActionTime();
        ServerboundPlayerActionPacket.Action packetplayinblockdig_enumplayerdigtype = packetplayinblockdig.getAction();
        switch (packetplayinblockdig_enumplayerdigtype) {
            case SWAP_ITEM_WITH_OFFHAND: {
                if (!this.player.isSpectator()) {
                    // BetterCombat mixin compatibility
                    // https://github.com/ZsoltMolnarrr/BetterCombat/blob/9090f08faf4a3e51256c8a7a13af94a80b6128c0/common/src/main/java/net/bettercombat/mixin/ServerPlayNetworkHandlerMixin.java
                    ItemStack offhandStack = this.player.getItemInHand(InteractionHand.OFF_HAND);
                    var result = bridge$platform$canSwapHandItems(this.player);
                    if (result._1) {
                        return;
                    }
                    ItemStack itemstack = result._2;
                    ItemStack originMainHand = result._3;
                    CraftItemStack mainHand = CraftItemStack.asCraftMirror(itemstack);
                    CraftItemStack offHand = CraftItemStack.asCraftMirror(originMainHand);
                    PlayerSwapHandItemsEvent swapItemsEvent = new PlayerSwapHandItemsEvent(this.getCraftPlayer(), mainHand.clone(), offHand.clone());
                    this.cserver.getPluginManager().callEvent(swapItemsEvent);
                    if (swapItemsEvent.isCancelled()) {
                        return;
                    }
                    if (swapItemsEvent.getOffHandItem().equals(offHand)) {
                        this.player.setItemInHand(InteractionHand.OFF_HAND, originMainHand);
                    } else {
                        this.player.setItemInHand(InteractionHand.OFF_HAND, CraftItemStack.asNMSCopy(swapItemsEvent.getOffHandItem()));
                    }
                    if (swapItemsEvent.getMainHandItem().equals(mainHand)) {
                        this.player.setItemInHand(InteractionHand.MAIN_HAND, itemstack);
                    } else {
                        this.player.setItemInHand(InteractionHand.MAIN_HAND, CraftItemStack.asNMSCopy(swapItemsEvent.getMainHandItem()));
                    }
                    this.player.stopUsingItem();
                }
                return;
            }
            case DROP_ITEM: {
                if (!this.player.isSpectator()) {
                    if (this.lastDropTick != ArclightConstants.currentTick) {
                        this.dropCount = 0;
                        this.lastDropTick = ArclightConstants.currentTick;
                    } else {
                        ++this.dropCount;
                        if (this.dropCount >= 20) {
                            LOGGER.warn(this.player.getScoreboardName() + " dropped their items too quickly!");
                            this.bridge$disconnect("You dropped your items too quickly (Hacking?)");
                            return;
                        }
                    }
                    this.player.drop(false);
                }
                return;
            }
            case DROP_ALL_ITEMS: {
                if (!this.player.isSpectator()) {
                    this.player.drop(true);
                }
                return;
            }
            case RELEASE_USE_ITEM: {
                this.player.releaseUsingItem();
                return;
            }
            case START_DESTROY_BLOCK:
            case ABORT_DESTROY_BLOCK:
            case STOP_DESTROY_BLOCK: {
                this.player.gameMode.handleBlockBreakAction(blockposition, packetplayinblockdig_enumplayerdigtype, packetplayinblockdig.getDirection(), this.player.level().getMaxBuildHeight(), packetplayinblockdig.getSequence());
                this.player.connection.ackBlockChangesUpTo(packetplayinblockdig.getSequence());
                return;
            }
            default: {
                throw new IllegalArgumentException("Invalid player action");
            }
        }
    }

    @Inject(method = "handleUseItemOn", cancellable = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/level/ServerPlayer;serverLevel()Lnet/minecraft/server/level/ServerLevel;"))
    private void arclight$frozenUseItem(ServerboundUseItemOnPacket packetIn, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
        if (!this.checkLimit(((TimestampedPacket) packetIn).bridge$timestamp())) {
            ci.cancel();
        }
    }

    // So, what is SPIGOT-4706 exactly?
    // @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    // private void arclight$checkDistance(ServerboundUseItemOnPacket packetIn, CallbackInfo ci) {
    //     this.player.stopUsingItem();
    // }

    private int limitedPackets;
    private long lastLimitedPacket = -1;

    private boolean checkLimit(long timestamp) {
        if (lastLimitedPacket != -1 && timestamp - lastLimitedPacket < 30 && limitedPackets++ >= 4) {
            return false;
        }

        if (lastLimitedPacket == -1 || timestamp - lastLimitedPacket >= 30) {
            lastLimitedPacket = timestamp;
            limitedPackets = 0;
            return true;
        }

        return true;
    }

    @Inject(method = "handleUseItem", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"))
    private void arclight$spamCheck(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            ci.cancel();
            return;
        }
        if (!this.checkLimit(((TimestampedPacket) packet).bridge$timestamp())) {
            ci.cancel();
            return;
        }
    }

    @Decorate(method = "handleUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItem(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult arclight$playerInteractEvent(ServerPlayerGameMode instance, ServerPlayer serverPlayer, net.minecraft.world.level.Level level, ItemStack itemStack, InteractionHand interactionHand, ServerboundUseItemPacket packet) throws Throwable {
        float f2 = Mth.wrapDegrees(packet.getYRot());
        float f1 = Mth.wrapDegrees(packet.getXRot());
        double d0 = this.player.getX();
        double d2 = this.player.getY() + this.player.getEyeHeight();
        double d3 = this.player.getZ();
        Vec3 vec3d = new Vec3(d0, d2, d3);
        float f3 = Mth.cos(-f2 * 0.017453292f - 3.1415927f);
        float f4 = Mth.sin(-f2 * 0.017453292f - 3.1415927f);
        float f5 = -Mth.cos(-f1 * 0.017453292f);
        float f6 = Mth.sin(-f1 * 0.017453292f);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d4 = this.player.blockInteractionRange();
        Vec3 vec3d2 = vec3d.add(f7 * d4, f6 * d4, f8 * d4);
        HitResult movingobjectposition = this.player.level().clip(new ClipContext(vec3d, vec3d2, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.player));
        boolean cancelled;
        if (movingobjectposition == null || movingobjectposition.getType() != HitResult.Type.BLOCK) {
            org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.RIGHT_CLICK_AIR, itemStack, interactionHand);
            cancelled = event.useItemInHand() == Event.Result.DENY;
        } else {
            BlockHitResult movingobjectpositionblock = (BlockHitResult) movingobjectposition;
            if (((PlayerInteractionManagerBridge) player.gameMode).bridge$isFiredInteract() && ((PlayerInteractionManagerBridge) player.gameMode).bridge$getInteractPosition().equals(movingobjectpositionblock.getBlockPos()) && ((PlayerInteractionManagerBridge) player.gameMode).bridge$getInteractHand() == interactionHand && ItemStack.isSameItemSameComponents(((PlayerInteractionManagerBridge) player.gameMode).bridge$getInteractItemStack(), itemStack)) {
                cancelled = ((PlayerInteractionManagerBridge) player.gameMode).bridge$getInteractResult();
            } else {
                org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, movingobjectpositionblock.getBlockPos(), movingobjectpositionblock.getDirection(), itemStack, true, interactionHand, movingobjectpositionblock.getLocation());
                cancelled = event.useItemInHand() == Event.Result.DENY;
            }
            ((PlayerInteractionManagerBridge) player.gameMode).bridge$setFiredInteract(false);
        }
        if (cancelled) {
            ((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity().updateInventory();
            return (InteractionResult) DecorationOps.cancel().invoke();
        }
        itemStack = this.player.getItemInHand(interactionHand); // Update in case it was changed in the event
        if (itemStack.isEmpty()) {
            return (InteractionResult) DecorationOps.cancel().invoke();
        }
        return (InteractionResult) DecorationOps.callsite().invoke(instance, serverPlayer, level, itemStack, interactionHand);
    }

    @Inject(method = "handleTeleportToEntityPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V"))
    private void arclight$spectateTeleport(ServerboundTeleportToEntityPacket packetIn, CallbackInfo ci) {
        ((ServerPlayerEntityBridge) this.player).bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause.SPECTATE);
    }

    @Inject(method = "handleSetCarriedItem", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"))
    private void arclight$carriedItemBlocked(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSetCarriedItem", cancellable = true, at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/world/entity/player/Inventory;selected:I"))
    private void arclight$itemHeldEvent(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        PlayerItemHeldEvent event = new PlayerItemHeldEvent(this.getCraftPlayer(), this.player.getInventory().selected, packet.getSlot());
        this.cserver.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.send(new ClientboundSetCarriedItemPacket(this.player.getInventory().selected));
            this.player.resetLastActionTime();
            ci.cancel();
        }
    }

    @Inject(method = "handleSetCarriedItem", at = @At(value = "INVOKE", shift = At.Shift.AFTER, remap = false, target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"))
    private void arclight$kickOutOfBoundClick(ServerboundSetCarriedItemPacket serverboundSetCarriedItemPacket, CallbackInfo ci) {
        this.bridge$disconnect("Invalid hotbar selection (Hacking?)");
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleChat(ServerboundChatPacket packet) {
        if (this.server.isStopped()) {
            return;
        }
        Optional<LastSeenMessages> optional = this.unpackAndApplyLastSeen(packet.lastSeenMessages());
        if (!optional.isEmpty()) {
            this.tryHandleChat(packet.message(), RunnableInPlace.wrap(() -> {
                PlayerChatMessage playerchatmessage;

                try {
                    playerchatmessage = this.getSignedMessage(packet, optional.get());
                } catch (SignedMessageChain.DecodeException e) {
                    this.handleMessageDecodeFailure(e);
                    return;
                }

                CompletableFuture<FilteredText> completablefuture = this.filterTextPacket(playerchatmessage.signedContent()).thenApplyAsync(Function.identity(), ArclightServer.getChatExecutor());
                var component = bridge$platform$onServerChatSubmitted(this.player, playerchatmessage.decoratedContent());

                this.chatMessageChain.append(completablefuture, (text) -> {
                        if (component == null) return;
                        PlayerChatMessage playerchatmessage1 = playerchatmessage.withUnsignedContent(component).filter(completablefuture.join().mask());

                        this.broadcastChatMessage(playerchatmessage1);
                    }
                );
            }));
        }
    }

    @Inject(method = "performSignedChatCommand", cancellable = true, at = @At("HEAD"))
    private void arclight$rejectIfDisconnectSigned(CallbackInfo ci) {
        if (this.player.hasDisconnected()) {
            ci.cancel();
        }
    }

    @Decorate(method = "performUnsignedChatCommand", inject = true, at = @At("HEAD"))
    private void arclight$commandPreprocessEvent(String s) throws Throwable {
        if (this.player.hasDisconnected()) {
            DecorationOps.cancel().invoke();
            return;
        }
        String command = "/" + s;
        LOGGER.info(this.player.getScoreboardName() + " issued server command: " + command);

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(getCraftPlayer(), command, new LazyPlayerSet(server));
        this.cserver.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }
        s = event.getMessage().substring(1);
        DecorationOps.blackhole().invoke(s);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void performSignedChatCommand(ServerboundChatCommandSignedPacket serverboundchatcommandsignedpacket, LastSeenMessages lastseenmessages) {
        // CraftBukkit start
        String command = "/" + serverboundchatcommandsignedpacket.command();
        LOGGER.info(this.player.getScoreboardName() + " issued server command: " + command);

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(getCraftPlayer(), command, new LazyPlayerSet(server));
        this.cserver.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }
        command = event.getMessage().substring(1);

        ParseResults<CommandSourceStack> parseresults = this.parseCommand(command);
        // CraftBukkit end

        Map map;

        try {
            map = (serverboundchatcommandsignedpacket.command().equals(command)) ? this.collectSignedArguments(serverboundchatcommandsignedpacket, SignableCommand.of(parseresults), lastseenmessages) : Collections.emptyMap(); // CraftBukkit
        } catch (SignedMessageChain.DecodeException signedmessagechain_a) {
            this.handleMessageDecodeFailure(signedmessagechain_a);
            return;
        }

        CommandSigningContext.SignedArguments commandsigningcontext_a = new CommandSigningContext.SignedArguments(map);

        parseresults = Commands.<CommandSourceStack>mapSource(parseresults, (commandlistenerwrapper) -> { // CraftBukkit - decompile error
            return commandlistenerwrapper.withSigningContext(commandsigningcontext_a, this.chatMessageChain);
        });
        this.server.getCommands().performCommand(parseresults, command); // CraftBukkit
    }

    @Inject(method = "tryHandleChat", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getChatVisibility()Lnet/minecraft/world/entity/player/ChatVisiblity;"))
    private void arclight$deadMenTellNoTales(String string, Runnable runnable, CallbackInfo ci) {
        if (this.player.isRemoved()) {
            this.send(new ClientboundSystemChatPacket(Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED), false));
            ci.cancel();
        }
    }

    public void chat(String s, PlayerChatMessage original, boolean async) {
        if (s.isEmpty() || this.player.getChatVisibility() == ChatVisiblity.HIDDEN) {
            return;
        }
        ServerGamePacketListenerImpl handler = (ServerGamePacketListenerImpl) (Object) this;
        var outgoing = OutgoingChatMessage.create(original);
        if (!async && s.startsWith("/")) {
            this.handleCommand(s);
        } else if (this.player.getChatVisibility() != ChatVisiblity.SYSTEM) {
            Player thisPlayer = this.getCraftPlayer();
            AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(async, thisPlayer, s, new LazyPlayerSet(this.server));
            String originalFormat = event.getFormat(), originalMessage = event.getMessage();
            this.cserver.getPluginManager().callEvent(event);
            if (PlayerChatEvent.getHandlerList().getRegisteredListeners().length != 0) {
                PlayerChatEvent queueEvent = new PlayerChatEvent(thisPlayer, event.getMessage(), event.getFormat(), event.getRecipients());
                queueEvent.setCancelled(event.isCancelled());
                class SyncChat extends Waitable<Object> {

                    @Override
                    protected Object evaluate() {
                        Bukkit.getPluginManager().callEvent(queueEvent);
                        if (queueEvent.isCancelled()) {
                            return null;
                        }
                        String message = String.format(queueEvent.getFormat(), queueEvent.getPlayer().getDisplayName(), queueEvent.getMessage());
                        if (((LazyPlayerSet) queueEvent.getRecipients()).isLazy()) {
                            if (!org.spigotmc.SpigotConfig.bungee && originalFormat.equals(queueEvent.getFormat()) && originalMessage.equals(queueEvent.getMessage()) && queueEvent.getPlayer().getName().equalsIgnoreCase(queueEvent.getPlayer().getDisplayName())) { // Spigot
                                server.getPlayerList().broadcastChatMessage(original, player, ChatType.bind(ChatType.CHAT, player));
                                return null;
                            }
                            for (ServerPlayer recipient : server.getPlayerList().players) {
                                ((ServerPlayerEntityBridge) recipient).bridge$getBukkitEntity().sendMessage(player.getUUID(), message);
                            }
                        } else {
                            for (Player player2 : queueEvent.getRecipients()) {
                                player2.sendMessage(thisPlayer.getUniqueId(), message);
                            }
                        }
                        Bukkit.getConsoleSender().sendMessage(message);
                        return null;
                    }
                }
                Waitable waitable = new SyncChat();
                if (async) {
                    ((MinecraftServerBridge) server).bridge$queuedProcess(waitable);
                } else {
                    waitable.run();
                }
                try {
                    waitable.get();
                    return;
                } catch (InterruptedException e2) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    throw new RuntimeException("Exception processing chat event", e.getCause());
                }
            }
            if (event.isCancelled()) {
                return;
            }

            s = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
            if (((LazyPlayerSet) event.getRecipients()).isLazy()) {
                if (!org.spigotmc.SpigotConfig.bungee && originalFormat.equals(event.getFormat()) && originalMessage.equals(event.getMessage()) && event.getPlayer().getName().equalsIgnoreCase(event.getPlayer().getDisplayName())) { // Spigot
                    server.getPlayerList().broadcastChatMessage(original, player, ChatType.bind(ChatType.CHAT, player));
                    return;
                }

                for (ServerPlayer recipient : server.getPlayerList().players) {
                    ((ServerPlayerEntityBridge) recipient).bridge$getBukkitEntity().sendMessage(player.getUUID(), s);
                }
            } else {
                for (Player recipient : event.getRecipients()) {
                    recipient.sendMessage(player.getUUID(), s);
                }
            }
            Bukkit.getConsoleSender().sendMessage(s);
        }
    }

    private void handleCommand(String s) {
        if (SpigotConfig.logCommands) {
            LOGGER.info(this.player.getScoreboardName() + " issued server command: " + s);
        }
        CraftPlayer player = this.getCraftPlayer();
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, s, new LazyPlayerSet(this.server));
        this.cserver.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        try {
            this.cserver.dispatchCommand(event.getPlayer(), event.getMessage().substring(1));
        } catch (CommandException ex) {
            player.sendMessage(ChatColor.RED + "An internal error occurred while attempting to perform this command");
            java.util.logging.Logger.getLogger(ServerGamePacketListenerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void broadcastChatMessage(PlayerChatMessage playerchatmessage) {
        String s = playerchatmessage.signedContent();
        if (s.isEmpty()) {
            LOGGER.warn(this.player.getScoreboardName() + " tried to send an empty message");
        } else if (getCraftPlayer().isConversing()) {
            final String conversationInput = s;
            ((MinecraftServerBridge) this.server).bridge$queuedProcess(() -> getCraftPlayer().acceptConversationInput(conversationInput));
        } else if (this.player.getChatVisibility() == ChatVisiblity.SYSTEM) { // Re-add "Command Only" flag check
            this.send(new ClientboundSystemChatPacket(Component.translatable("chat.cannotSend").withStyle(ChatFormatting.RED), false));
        } else {
            this.chat(s, playerchatmessage, true);
        }
        // this.server.getPlayerList().broadcastChatMessage(playerchatmessage, this.player, ChatMessageType.bind(ChatMessageType.CHAT, (Entity) this.player));
        this.detectRateSpam();
    }

    @Inject(method = "sendPlayerChatMessage", cancellable = true, at = @At("HEAD"))
    private void arclight$cantSee(PlayerChatMessage playerChatMessage, ChatType.Bound bound, CallbackInfo ci) {
        if (!getCraftPlayer().canSeePlayer(playerChatMessage.link().sender())) {
            sendDisguisedChatMessage(playerChatMessage.decoratedContent(), bound);
            ci.cancel();
        }
    }

    @Inject(method = "handleAnimate", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"))
    private void arclight$interactCheck(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleAnimate", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private void arclight$animateEvents(ServerboundSwingPacket packet, CallbackInfo ci) {
        float f1 = this.player.getXRot();
        float f2 = this.player.getYRot();
        double d0 = this.player.getX();
        double d2 = this.player.getY() + this.player.getEyeHeight();
        double d3 = this.player.getZ();
        double d4 = ((PlayerEntityBridge) this.player).bridge$platform$getBlockReach();
        var origin = new Location(this.player.level().bridge$getWorld(), d0, d2, d3, f1, f2);
        var result = this.player.level().bridge$getWorld().rayTrace(origin, origin.getDirection(), d4, org.bukkit.FluidCollisionMode.NEVER, false, 0.1, entity -> {
            Entity handle = ((CraftEntity) entity).getHandle();
            return handle != this.player && ((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity().canSee(entity) && !handle.isSpectator() && handle.isPickable() && !handle.isPassengerOfSameVehicle(player);
        });
        if (result == null) {
            CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_AIR, this.player.getInventory().getSelected(), InteractionHand.MAIN_HAND);
        }
        PlayerAnimationEvent event = new PlayerAnimationEvent(this.getCraftPlayer(), packet.getHand() == InteractionHand.MAIN_HAND ? PlayerAnimationType.ARM_SWING : PlayerAnimationType.OFF_ARM_SWING);
        this.cserver.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerCommand", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"))
    private void arclight$toggleAction(ServerboundPlayerCommandPacket packetIn, CallbackInfo ci) {
        if (this.player.isRemoved()) {
            ci.cancel();
            return;
        }
        if (packetIn.getAction() == ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY || packetIn.getAction() == ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY) {
            PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this.getCraftPlayer(), packetIn.getAction() == ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY);
            this.cserver.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        } else if (packetIn.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING || packetIn.getAction() == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING) {
            PlayerToggleSprintEvent e2 = new PlayerToggleSprintEvent(this.getCraftPlayer(), packetIn.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING);
            this.cserver.getPluginManager().callEvent(e2);
            if (e2.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleInteract", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"))
    private void arclight$interactCheck(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            ci.cancel();
            return;
        }
        final ServerLevel world = this.player.serverLevel();
        final Entity entity = packet.getTarget(world);
        if (entity == player && !player.isSpectator()) {
            bridge$disconnect("Cannot interact with self!");
            ci.cancel();
        }
    }

    @Inject(method = "handleClientCommand", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/players/PlayerList;respawn(Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/world/entity/Entity$RemovalReason;)Lnet/minecraft/server/level/ServerPlayer;"))
    private void arclight$respawnChangeDim(ServerboundClientCommandPacket packet, CallbackInfo ci) {
        ((PlayerListBridge) this.server.getPlayerList()).bridge$pushRespawnCause(PlayerRespawnEvent.RespawnReason.END_PORTAL);
    }

    @Inject(method = "handleClientCommand", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/players/PlayerList;respawn(Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/world/entity/Entity$RemovalReason;)Lnet/minecraft/server/level/ServerPlayer;"))
    private void arclight$respawnKilled(ServerboundClientCommandPacket packet, CallbackInfo ci) {
        ((PlayerListBridge) this.server.getPlayerList()).bridge$pushRespawnCause(PlayerRespawnEvent.RespawnReason.DEATH);
    }

    @Inject(method = "handleContainerClose", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;doCloseContainer()V"))
    private void arclight$invClose(ServerboundContainerClosePacket packetIn, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
        // CraftEventFactory.handleInventoryCloseEvent(this.player); handled in ServerPlayerEntity#closeContainer
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleContainerClick(ServerboundContainerClickPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this, this.player.serverLevel());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        this.player.resetLastActionTime();
        if (this.player.containerMenu.containerId == packet.getContainerId() && this.player.containerMenu.stillValid(this.player)) { // CraftBukkit
            boolean cancelled = this.player.isSpectator(); // CraftBukkit - see below if
            if (false/*this.player.isSpectator()*/) { // CraftBukkit
                this.player.containerMenu.sendAllDataToRemote();
            } else if (!this.player.containerMenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
            } else {
                boolean flag = packet.getStateId() != this.player.containerMenu.getStateId();

                this.player.containerMenu.suppressRemoteUpdates();
                // CraftBukkit start - Call InventoryClickEvent
                if (packet.getSlotNum() < -1 && packet.getSlotNum() != -999) {
                    return;
                }

                ArclightCaptures.captureContainerOwner(this.player);
                InventoryView inventory = ((ContainerBridge) this.player.containerMenu).bridge$getBukkitView();
                ArclightCaptures.resetContainerOwner();
                InventoryType.SlotType type = inventory.getSlotType(packet.getSlotNum());

                InventoryClickEvent event;
                ClickType click = ClickType.UNKNOWN;
                InventoryAction action = InventoryAction.UNKNOWN;

                ItemStack itemstack = ItemStack.EMPTY;

                switch (packet.getClickType()) {
                    case PICKUP:
                        if (packet.getButtonNum() == 0) {
                            click = ClickType.LEFT;
                        } else if (packet.getButtonNum() == 1) {
                            click = ClickType.RIGHT;
                        }
                        if (packet.getButtonNum() == 0 || packet.getButtonNum() == 1) {
                            action = InventoryAction.NOTHING; // Don't want to repeat ourselves
                            if (packet.getSlotNum() == -999) {
                                if (!player.containerMenu.getCarried().isEmpty()) {
                                    action = packet.getButtonNum() == 0 ? InventoryAction.DROP_ALL_CURSOR : InventoryAction.DROP_ONE_CURSOR;
                                }
                            } else if (packet.getSlotNum() < 0) {
                                action = InventoryAction.NOTHING;
                            } else {
                                Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                                if (slot != null) {
                                    ItemStack clickedItem = slot.getItem();
                                    ItemStack cursor = player.containerMenu.getCarried();
                                    if (clickedItem.isEmpty()) {
                                        if (!cursor.isEmpty()) {
                                            action = packet.getButtonNum() == 0 ? InventoryAction.PLACE_ALL : InventoryAction.PLACE_ONE;
                                        }
                                    } else if (slot.mayPickup(player)) {
                                        if (cursor.isEmpty()) {
                                            action = packet.getButtonNum() == 0 ? InventoryAction.PICKUP_ALL : InventoryAction.PICKUP_HALF;
                                        } else if (slot.mayPlace(cursor)) {
                                            if (ItemStack.isSameItemSameComponents(clickedItem, cursor)) {
                                                int toPlace = packet.getButtonNum() == 0 ? cursor.getCount() : 1;
                                                toPlace = Math.min(toPlace, clickedItem.getMaxStackSize() - clickedItem.getCount());
                                                toPlace = Math.min(toPlace, slot.container.getMaxStackSize() - clickedItem.getCount());
                                                if (toPlace == 1) {
                                                    action = InventoryAction.PLACE_ONE;
                                                } else if (toPlace == cursor.getCount()) {
                                                    action = InventoryAction.PLACE_ALL;
                                                } else if (toPlace < 0) {
                                                    action = toPlace != -1 ? InventoryAction.PICKUP_SOME : InventoryAction.PICKUP_ONE; // this happens with oversized stacks
                                                } else if (toPlace != 0) {
                                                    action = InventoryAction.PLACE_SOME;
                                                }
                                            } else if (cursor.getCount() <= slot.getMaxStackSize()) {
                                                action = InventoryAction.SWAP_WITH_CURSOR;
                                            }
                                        } else if (ItemStack.isSameItemSameComponents(cursor, clickedItem)) {
                                            if (clickedItem.getCount() >= 0) {
                                                if (clickedItem.getCount() + cursor.getCount() <= cursor.getMaxStackSize()) {
                                                    // As of 1.5, this is result slots only
                                                    action = InventoryAction.PICKUP_ALL;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    // TODO check on updates
                    case QUICK_MOVE:
                        if (packet.getButtonNum() == 0) {
                            click = ClickType.SHIFT_LEFT;
                        } else if (packet.getButtonNum() == 1) {
                            click = ClickType.SHIFT_RIGHT;
                        }
                        if (packet.getButtonNum() == 0 || packet.getButtonNum() == 1) {
                            if (packet.getSlotNum() < 0) {
                                action = InventoryAction.NOTHING;
                            } else {
                                Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                                if (slot != null && slot.mayPickup(this.player) && slot.hasItem()) {
                                    action = InventoryAction.MOVE_TO_OTHER_INVENTORY;
                                } else {
                                    action = InventoryAction.NOTHING;
                                }
                            }
                        }
                        break;
                    case SWAP:
                        if ((packet.getButtonNum() >= 0 && packet.getButtonNum() < 9) || packet.getButtonNum() == 40) {
                            click = (packet.getButtonNum() == 40) ? ClickType.SWAP_OFFHAND : ClickType.NUMBER_KEY;
                            Slot clickedSlot = this.player.containerMenu.getSlot(packet.getSlotNum());
                            if (clickedSlot.mayPickup(player)) {
                                ItemStack hotbar = this.player.getInventory().getItem(packet.getButtonNum());
                                boolean canCleanSwap = hotbar.isEmpty() || (clickedSlot.container == player.getInventory() && clickedSlot.mayPlace(hotbar)); // the slot will accept the hotbar item
                                if (clickedSlot.hasItem()) {
                                    if (canCleanSwap) {
                                        action = InventoryAction.HOTBAR_SWAP;
                                    } else {
                                        action = InventoryAction.HOTBAR_MOVE_AND_READD;
                                    }
                                } else if (!clickedSlot.hasItem() && !hotbar.isEmpty() && clickedSlot.mayPlace(hotbar)) {
                                    action = InventoryAction.HOTBAR_SWAP;
                                } else {
                                    action = InventoryAction.NOTHING;
                                }
                            } else {
                                action = InventoryAction.NOTHING;
                            }
                        }
                        break;
                    case CLONE:
                        if (packet.getButtonNum() == 2) {
                            click = ClickType.MIDDLE;
                            if (packet.getSlotNum() < 0) {
                                action = InventoryAction.NOTHING;
                            } else {
                                Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                                if (slot != null && slot.hasItem() && player.getAbilities().instabuild && player.containerMenu.getCarried().isEmpty()) {
                                    action = InventoryAction.CLONE_STACK;
                                } else {
                                    action = InventoryAction.NOTHING;
                                }
                            }
                        } else {
                            click = ClickType.UNKNOWN;
                            action = InventoryAction.UNKNOWN;
                        }
                        break;
                    case THROW:
                        if (packet.getSlotNum() >= 0) {
                            if (packet.getButtonNum() == 0) {
                                click = ClickType.DROP;
                                Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                                if (slot != null && slot.hasItem() && slot.mayPickup(player) && !slot.getItem().isEmpty() && slot.getItem().getItem() != Item.byBlock(Blocks.AIR)) {
                                    action = InventoryAction.DROP_ONE_SLOT;
                                } else {
                                    action = InventoryAction.NOTHING;
                                }
                            } else if (packet.getButtonNum() == 1) {
                                click = ClickType.CONTROL_DROP;
                                Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                                if (slot != null && slot.hasItem() && slot.mayPickup(player) && !slot.getItem().isEmpty() && slot.getItem().getItem() != Item.byBlock(Blocks.AIR)) {
                                    action = InventoryAction.DROP_ALL_SLOT;
                                } else {
                                    action = InventoryAction.NOTHING;
                                }
                            }
                        } else {
                            // Sane default (because this happens when they are holding nothing. Don't ask why.)
                            click = ClickType.LEFT;
                            if (packet.getButtonNum() == 1) {
                                click = ClickType.RIGHT;
                            }
                            action = InventoryAction.NOTHING;
                        }
                        break;
                    case QUICK_CRAFT:
                        this.player.containerMenu.clicked(packet.getSlotNum(), packet.getButtonNum(), packet.getClickType(), this.player);
                        break;
                    case PICKUP_ALL:
                        click = ClickType.DOUBLE_CLICK;
                        action = InventoryAction.NOTHING;
                        if (packet.getSlotNum() >= 0 && !this.player.containerMenu.getCarried().isEmpty()) {
                            ItemStack cursor = this.player.containerMenu.getCarried();
                            action = InventoryAction.NOTHING;
                            // Quick check for if we have any of the item
                            if (inventory.getTopInventory().contains(CraftItemType.minecraftToBukkit(cursor.getItem())) || inventory.getBottomInventory().contains(CraftItemType.minecraftToBukkit(cursor.getItem()))) {
                                action = InventoryAction.COLLECT_TO_CURSOR;
                            }
                        }
                        break;
                    default:
                        break;
                }

                if (packet.getClickType() != net.minecraft.world.inventory.ClickType.QUICK_CRAFT) {
                    if (click == ClickType.NUMBER_KEY) {
                        event = new InventoryClickEvent(inventory, type, packet.getSlotNum(), click, action, packet.getButtonNum());
                    } else {
                        event = new InventoryClickEvent(inventory, type, packet.getSlotNum(), click, action);
                    }

                    org.bukkit.inventory.Inventory top = inventory.getTopInventory();
                    if (packet.getSlotNum() == 0 && top instanceof CraftingInventory) {
                        org.bukkit.inventory.Recipe recipe = ((CraftingInventory) top).getRecipe();
                        if (recipe != null) {
                            if (click == ClickType.NUMBER_KEY) {
                                event = new CraftItemEvent(recipe, inventory, type, packet.getSlotNum(), click, action, packet.getButtonNum());
                            } else {
                                event = new CraftItemEvent(recipe, inventory, type, packet.getSlotNum(), click, action);
                            }
                        }
                    }

                    if (packet.getSlotNum() == 3 && top instanceof SmithingInventory) {
                        org.bukkit.inventory.ItemStack result = ((SmithingInventory) top).getResult();
                        if (result != null) {
                            if (click == ClickType.NUMBER_KEY) {
                                event = new SmithItemEvent(inventory, type, packet.getSlotNum(), click, action, packet.getButtonNum());
                            } else {
                                event = new SmithItemEvent(inventory, type, packet.getSlotNum(), click, action);
                            }
                        }
                    }

                    event.setCancelled(cancelled);
                    AbstractContainerMenu oldContainer = this.player.containerMenu; // SPIGOT-1224
                    cserver.getPluginManager().callEvent(event);
                    if (this.player.containerMenu != oldContainer) {
                        return;
                    }

                    switch (event.getResult()) {
                        case ALLOW:
                        case DEFAULT:
                            this.player.containerMenu.clicked(packet.getSlotNum(), packet.getButtonNum(), packet.getClickType(), this.player);
                            break;
                        case DENY:
                            /* Needs enum constructor in InventoryAction
                            if (action.modifiesOtherSlots()) {

                            } else {
                                if (action.modifiesCursor()) {
                                    this.player.playerConnection.sendPacket(new Packet103SetSlot(-1, -1, this.player.inventory.getCarried()));
                                }
                                if (action.modifiesClicked()) {
                                    this.player.playerConnection.sendPacket(new Packet103SetSlot(this.player.activeContainer.windowId, packet102windowclick.slot, this.player.activeContainer.getSlot(packet102windowclick.slot).getItem()));
                                }
                            }*/
                            switch (action) {
                                // Modified other slots
                                case PICKUP_ALL:
                                case MOVE_TO_OTHER_INVENTORY:
                                case HOTBAR_MOVE_AND_READD:
                                case HOTBAR_SWAP:
                                case COLLECT_TO_CURSOR:
                                case UNKNOWN:
                                    this.player.containerMenu.sendAllDataToRemote();
                                    break;
                                // Modified cursor and clicked
                                case PICKUP_SOME:
                                case PICKUP_HALF:
                                case PICKUP_ONE:
                                case PLACE_ALL:
                                case PLACE_SOME:
                                case PLACE_ONE:
                                case SWAP_WITH_CURSOR:
                                    this.player.connection.send(new ClientboundContainerSetSlotPacket(-1, -1, this.player.inventoryMenu.incrementStateId(), this.player.containerMenu.getCarried()));
                                    this.player.connection.send(new ClientboundContainerSetSlotPacket(this.player.containerMenu.containerId, this.player.inventoryMenu.incrementStateId(), packet.getSlotNum(), this.player.containerMenu.getSlot(packet.getSlotNum()).getItem()));
                                    break;
                                // Modified clicked only
                                case DROP_ALL_SLOT:
                                case DROP_ONE_SLOT:
                                    this.player.connection.send(new ClientboundContainerSetSlotPacket(this.player.containerMenu.containerId, this.player.inventoryMenu.incrementStateId(), packet.getSlotNum(), this.player.containerMenu.getSlot(packet.getSlotNum()).getItem()));
                                    break;
                                // Modified cursor only
                                case DROP_ALL_CURSOR:
                                case DROP_ONE_CURSOR:
                                case CLONE_STACK:
                                    this.player.connection.send(new ClientboundContainerSetSlotPacket(-1, -1, this.player.inventoryMenu.incrementStateId(), this.player.containerMenu.getCarried()));
                                    break;
                                // Nothing
                                case NOTHING:
                                    break;
                            }
                    }

                    if (event instanceof CraftItemEvent || event instanceof SmithItemEvent) {
                        // Need to update the inventory on crafting to
                        // correctly support custom recipes
                        player.containerMenu.sendAllDataToRemote();
                    }
                }
                // CraftBukkit end

                for (var entry : Int2ObjectMaps.fastIterable(packet.getChangedSlots())) {
                    this.player.containerMenu.setRemoteSlotNoCopy(entry.getIntKey(), entry.getValue());
                }

                this.player.containerMenu.setRemoteCarried(packet.getCarriedItem());
                this.player.containerMenu.resumeRemoteUpdates();
                if (flag) {
                    this.player.containerMenu.broadcastFullState();
                } else {
                    this.player.containerMenu.broadcastChanges();
                }
            }
        }
    }

    @Inject(method = "handleRecipeBookChangeSettingsPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/stats/ServerRecipeBook;setBookSetting(Lnet/minecraft/world/inventory/RecipeBookType;ZZ)V"))
    private void arclight$recipeBookSettings(ServerboundRecipeBookChangeSettingsPacket packet, CallbackInfo ci) {
        CraftEventFactory.callRecipeBookSettingsEvent(this.player, packet.getBookType(), packet.isOpen(), packet.isFiltering());
    }

    @Redirect(method = "handlePlaceRecipe", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundPlaceRecipePacket;getRecipe()Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation arclight$recipeBookClick(ServerboundPlaceRecipePacket instance) {
        var location = instance.getRecipe();
        org.bukkit.inventory.Recipe recipe = this.cserver.getRecipe(CraftNamespacedKey.fromMinecraft(location));
        if (recipe == null) {
            return location;
        }
        var event = CraftEventFactory.callRecipeBookClickEvent(this.player, recipe, instance.isShiftDown());
        if (event.getRecipe() instanceof org.bukkit.Keyed keyed) {
            return CraftNamespacedKey.toMinecraft(keyed.getKey());
        }
        return location;
    }

    @Inject(method = "handleContainerButtonClick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"))
    private void arclight$noEnchant(ServerboundContainerButtonClickPacket packetIn, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleSetCreativeModeSlot(final ServerboundSetCreativeModeSlotPacket packetplayinsetcreativeslot) {
        PacketUtils.ensureRunningOnSameThread(packetplayinsetcreativeslot, (ServerGamePacketListenerImpl) (Object) this, this.player.serverLevel());
        if (this.player.gameMode.isCreative()) {
            final boolean flag = packetplayinsetcreativeslot.slotNum() < 0;
            ItemStack itemstack = packetplayinsetcreativeslot.itemStack();
            CustomData customdata = (CustomData) itemstack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);

            if (customdata.contains("x") && customdata.contains("y") && customdata.contains("z") && this.player.bridge$getBukkitEntity().hasPermission("minecraft.nbt.copy")) {
                BlockPos blockpos = BlockEntity.getPosFromTag(customdata.getUnsafe());
                if (this.player.level().isLoaded(blockpos)) {
                    BlockEntity blockentity = this.player.level().getBlockEntity(blockpos);
                    if (blockentity != null) {
                        blockentity.saveToItem(itemstack, this.player.level().registryAccess());
                    }
                }
            }
            final boolean flag2 = packetplayinsetcreativeslot.slotNum() >= 1 && packetplayinsetcreativeslot.slotNum() <= 45;
            boolean flag3 = itemstack.isEmpty() || (itemstack.getDamageValue() >= 0 && itemstack.getCount() <= 64 && !itemstack.isEmpty());
            if (flag || (flag2 && !ItemStack.matches(this.player.inventoryMenu.getSlot(packetplayinsetcreativeslot.slotNum()).getItem(), packetplayinsetcreativeslot.itemStack()))) {
                final InventoryView inventory = ((ContainerBridge) this.player.inventoryMenu).bridge$getBukkitView();
                final org.bukkit.inventory.ItemStack item = CraftItemStack.asBukkitCopy(packetplayinsetcreativeslot.itemStack());
                InventoryType.SlotType type = InventoryType.SlotType.QUICKBAR;
                if (flag) {
                    type = InventoryType.SlotType.OUTSIDE;
                } else if (packetplayinsetcreativeslot.slotNum() < 36) {
                    if (packetplayinsetcreativeslot.slotNum() >= 5 && packetplayinsetcreativeslot.slotNum() < 9) {
                        type = InventoryType.SlotType.ARMOR;
                    } else {
                        type = InventoryType.SlotType.CONTAINER;
                    }
                }
                final InventoryCreativeEvent event = new InventoryCreativeEvent(inventory, type, flag ? -999 : packetplayinsetcreativeslot.slotNum(), item);
                this.cserver.getPluginManager().callEvent(event);
                itemstack = CraftItemStack.asNMSCopy(event.getCursor());
                switch (event.getResult()) {
                    case ALLOW: {
                        flag3 = true;
                        break;
                    }
                    case DEFAULT:
                        break;
                    case DENY: {
                        if (packetplayinsetcreativeslot.slotNum() >= 0) {
                            this.player.connection.send(new ClientboundContainerSetSlotPacket(this.player.inventoryMenu.containerId, this.player.inventoryMenu.incrementStateId(), packetplayinsetcreativeslot.slotNum(), this.player.inventoryMenu.getSlot(packetplayinsetcreativeslot.slotNum()).getItem()));
                            this.player.connection.send(new ClientboundContainerSetSlotPacket(-1, this.player.inventoryMenu.incrementStateId(), -1, ItemStack.EMPTY));
                        }
                        return;
                    }
                }
            }
            if (flag2 && flag3) {
                this.player.inventoryMenu.getSlot(packetplayinsetcreativeslot.slotNum()).setByPlayer(itemstack);
                this.player.inventoryMenu.broadcastChanges();
            } else if (flag && flag3 && this.dropSpamTickCount < 200) {
                this.dropSpamTickCount += 20;
                this.player.drop(itemstack, true);
            }
        }
    }

    @Inject(method = "updateSignText", cancellable = true, at = @At("HEAD"))
    private void arclight$updateSignText(ServerboundSignUpdatePacket p_9923_, List<FilteredText> p_9924_, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this, this.player.serverLevel());
        if ((this.player.getAbilities().mayfly || ((ServerPlayerEntityBridge) this.player).bridge$platform$mayfly()) && this.player.getAbilities().flying != packet.isFlying()) {
            PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(getCraftPlayer(), packet.isFlying());
            this.cserver.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                this.player.getAbilities().flying = packet.isFlying();
            } else {
                this.player.onUpdateAbilities();
            }
        }
    }

    private transient boolean arclight$noTeleportEvent;
    private transient boolean arclight$teleportCancelled;

    @Decorate(method = "teleport(DDDFFLjava/util/Set;)V", inject = true, at = @At("HEAD"))
    private void arclight$teleportEvent(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> relativeSet) throws Throwable {
        PlayerTeleportEvent.TeleportCause cause = arclight$cause == null ? PlayerTeleportEvent.TeleportCause.UNKNOWN : arclight$cause;
        arclight$cause = null;
        Player player = this.getCraftPlayer();
        Location from = player.getLocation();
        Location to = new Location(this.getCraftPlayer().getWorld(), x, y, z, yaw, pitch);
        if (!arclight$noTeleportEvent && !from.equals(to)) {
            PlayerTeleportEvent event = new PlayerTeleportEvent(player, from.clone(), to.clone(), cause);
            this.cserver.getPluginManager().callEvent(event);
            if (event.isCancelled() || !to.equals(event.getTo())) {
                relativeSet.clear();
                to = (event.isCancelled() ? event.getFrom() : event.getTo());
                x = to.getX();
                y = to.getY();
                z = to.getZ();
                yaw = to.getYaw();
                pitch = to.getPitch();
            }
            arclight$teleportCancelled = event.isCancelled();
        } else {
            arclight$teleportCancelled = false;
        }
        arclight$noTeleportEvent = false;

        if (Float.isNaN(yaw)) {
            yaw = 0.0f;
        }
        if (Float.isNaN(pitch)) {
            pitch = 0.0f;
        }
        this.justTeleported = true;
        DecorationOps.blackhole().invoke(x, y, z, yaw, pitch);
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;awaitingTeleportTime:I"))
    private void arclight$storeLastPosition(double d, double e, double f, float yaw, float pitch, Set<RelativeMovement> set, CallbackInfo ci) {
        this.lastPosX = this.awaitingPositionFromClient.x;
        this.lastPosY = this.awaitingPositionFromClient.y;
        this.lastPosZ = this.awaitingPositionFromClient.z;
        this.lastYaw = yaw;
        this.lastPitch = pitch;
    }

    public void teleport(double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        this.teleport(d0, d1, d2, f, f1, Collections.emptySet(), cause);
    }

    public void teleport(double d0, double d1, double d2, float f, float f1, Set<RelativeMovement> set, PlayerTeleportEvent.TeleportCause cause) {
        bridge$pushTeleportCause(cause);
        this.teleport(d0, d1, d2, f, f1, set);
    }

    public void teleport(Location dest) {
        arclight$noTeleportEvent = true;
        this.teleport(dest.getX(), dest.getY(), dest.getZ(), dest.getYaw(), dest.getPitch(), Collections.emptySet());
        arclight$noTeleportEvent = false;
    }

    @Override
    public void bridge$pushNoTeleportEvent() {
        arclight$noTeleportEvent = true;
    }

    private transient PlayerTeleportEvent.TeleportCause arclight$cause;

    @Override
    public void bridge$pushTeleportCause(PlayerTeleportEvent.TeleportCause cause) {
        arclight$cause = cause;
    }

    @Override
    public void bridge$teleport(Location dest) {
        teleport(dest);
    }

    @Override
    public boolean bridge$teleportCancelled() {
        return arclight$teleportCancelled;
    }
}
