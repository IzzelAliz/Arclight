package io.izzel.arclight.common.mixin.core.network.play;

import com.google.common.base.Charsets;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.network.play.TimestampedPacket;
import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.server.management.PlayerInteractionManagerBridge;
import io.izzel.arclight.common.bridge.server.management.PlayerListBridge;
import io.izzel.arclight.common.bridge.tileentity.SignTileEntityBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.compat.AstralSorceryHooks;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import it.unimi.dsi.fastutil.ints.Int2ShortMap;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.play.client.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundAddMobPacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundContainerAckPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundContainerAckPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
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
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftSign;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v.util.LazyPlayerSet;
import org.bukkit.craftbukkit.v.util.Waitable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.util.Vector;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetHandlerMixin implements ServerPlayNetHandlerBridge {

    // @formatter:off
    @Shadow(aliases = {"server", "field_147367_d"}, remap = false) @Final private MinecraftServer minecraftServer;
    @Shadow public ServerPlayer player;
    @Shadow @Final public Connection connection;
    @Shadow public abstract void onDisconnect(Component reason);
    @Shadow private static boolean containsInvalidValues(ServerboundMoveVehiclePacket packetIn) { return false; }
    @Shadow private Entity lastVehicle;
    @Shadow private double vehicleFirstGoodX;
    @Shadow private double vehicleFirstGoodY;
    @Shadow private double vehicleFirstGoodZ;
    @Shadow protected abstract boolean isSingleplayerOwner();
    @Shadow private double vehicleLastGoodX;
    @Shadow private double vehicleLastGoodY;
    @Shadow private double vehicleLastGoodZ;
    @Shadow private boolean clientVehicleIsFloating;
    @Shadow private int receivedMovePacketCount;
    @Shadow private int knownMovePacketCount;
    @Shadow private Vec3 awaitingPositionFromClient;
    @Shadow private static boolean containsInvalidValues(ServerboundMovePlayerPacket packetIn) { return false; }
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
    @Shadow public abstract void send(Packet<?> packetIn);
    @Shadow private int chatSpamTickCount;
    @Shadow @Final private Int2ShortMap expectedAcks;
    @Shadow private int dropSpamTickCount;
    @Shadow protected abstract boolean noBlocksAround(Entity p_241162_1_);
    @Shadow protected abstract boolean isPlayerCollidingWithAnythingNew(LevelReader p_241163_1_, AABB p_241163_2_);
    // @formatter:on

    private static final int SURVIVAL_PLACE_DISTANCE_SQUARED = 6 * 6;
    private static final int CREATIVE_PLACE_DISTANCE_SQUARED = 7 * 7;
    private CraftServer server;
    public boolean processedDisconnect;
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

    public CraftPlayer getPlayer() {
        return (this.player == null) ? null : ((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity();
    }

    @Override
    public boolean bridge$processedDisconnect() {
        return this.processedDisconnect;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(MinecraftServer server, Connection networkManagerIn, ServerPlayer playerIn, CallbackInfo ci) {
        this.server = ((CraftServer) Bukkit.getServer());
        allowedPlayerTicks = 1;
        dropCount = 0;
        lastPosX = Double.MAX_VALUE;
        lastPosY = Double.MAX_VALUE;
        lastPosZ = Double.MAX_VALUE;
        lastPitch = Float.MAX_VALUE;
        lastYaw = Float.MAX_VALUE;
        justTeleported = false;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void disconnect(Component textComponent) {
        this.disconnect(CraftChatMessage.fromComponent(textComponent));
    }

    public void disconnect(String s) {
        if (this.processedDisconnect) {
            return;
        }
        String leaveMessage = ChatFormatting.YELLOW + this.player.getScoreboardName() + " left the game.";
        PlayerKickEvent event = new PlayerKickEvent(this.server.getPlayer(this.player), s, leaveMessage);
        if (this.server.getServer().isRunning()) {
            this.server.getPluginManager().callEvent(event);
        }
        if (event.isCancelled()) {
            return;
        }
        s = event.getReason();
        Component textComponent = CraftChatMessage.fromString(s, true)[0];
        this.connection.send(new ClientboundDisconnectPacket(textComponent), future -> this.connection.disconnect(textComponent));
        this.onDisconnect(textComponent);
        this.connection.setReadOnly();
        this.minecraftServer.executeBlocking(this.connection::handleDisconnection);
    }

    @Override
    public void bridge$disconnect(String str) {
        disconnect(str);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleMoveVehicle(final ServerboundMoveVehiclePacket packetplayinvehiclemove) {
        PacketUtils.ensureRunningOnSameThread(packetplayinvehiclemove, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
        if (containsInvalidValues(packetplayinvehiclemove)) {
            this.disconnect(new TranslatableComponent("multiplayer.disconnect.invalid_vehicle_movement"));
        } else {
            Entity entity = this.player.getRootVehicle();
            if (entity != this.player && entity.getControllingPassenger() == this.player && entity == this.lastVehicle) {
                ServerLevel worldserver = this.player.getLevel();
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
                if (this.player.abilities.flying) {
                    speed = this.player.abilities.flyingSpeed * 20.0f;
                } else {
                    speed = this.player.abilities.walkingSpeed * 10.0f;
                }
                speed *= 2.0;
                if (d11 - d10 > Math.max(100.0, Math.pow(10.0f * i * speed, 2.0)) && !this.isSingleplayerOwner()) {
                    LOGGER.warn("{} (vehicle of {}) moved too quickly! {},{},{}", entity.getName().getString(), this.player.getName().getString(), d7, d8, d9);
                    this.connection.send(new ClientboundMoveVehiclePacket(entity));
                    return;
                }
                boolean flag = worldserver.noCollision(entity, entity.getBoundingBox().deflate(0.0625));
                d7 = d4 - this.vehicleLastGoodX;
                d8 = d5 - this.vehicleLastGoodY - 1.0E-6;
                d9 = d6 - this.vehicleLastGoodZ;
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
                entity.absMoveTo(d4, d5, d6, f, f2);
                this.player.absMoveTo(d4, d5, d6, this.player.yRot, this.player.xRot);
                boolean flag3 = worldserver.noCollision(entity, entity.getBoundingBox().deflate(0.0625));
                if (flag && (flag2 || !flag3)) {
                    entity.absMoveTo(d0, d2, d3, f, f2);
                    this.player.absMoveTo(d0, d2, d3, this.player.yRot, this.player.xRot);
                    this.connection.send(new ClientboundMoveVehiclePacket(entity));
                    return;
                }
                Player player = this.getPlayer();
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
                    if (from.getX() != Double.MAX_VALUE) {
                        Location oldTo = to.clone();
                        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
                        this.server.getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            this.bridge$teleport(from);
                            return;
                        }
                        if (!oldTo.equals(event.getTo()) && !event.isCancelled()) {
                            ((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                            return;
                        }
                        if (!from.equals(this.getPlayer().getLocation()) && this.justTeleported) {
                            this.justTeleported = false;
                            return;
                        }
                    }
                }
                this.player.getLevel().getChunkSource().move(this.player);
                this.player.checkMovementStatistics(this.player.getX() - d0, this.player.getY() - d2, this.player.getZ() - d3);
                this.clientVehicleIsFloating = d12 >= -0.03125 && !this.minecraftServer.isFlightAllowed() && this.noBlocksAround(entity);
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
            this.player.getLevel().getChunkSource().move(this.player);
        }
    }

    @Inject(method = "handleAcceptTeleportPacket", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;awaitingTeleport:I"))
    private void arclight$confirm(ServerboundAcceptTeleportationPacket packetIn, CallbackInfo ci) {
        if (this.awaitingPositionFromClient == null) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSelectTrade", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/MerchantMenu;setSelectionHint(I)V"))
    private void arclight$tradeSelect(ServerboundSelectTradePacket packetIn, CallbackInfo ci, int i, AbstractContainerMenu container) {
        CraftEventFactory.callTradeSelectEvent(this.player, i, (MerchantMenu) container);
    }

    @Inject(method = "handleEditBook", cancellable = true, at = @At("HEAD"))
    private void arclight$editBookSpam(ServerboundEditBookPacket packetIn, CallbackInfo ci) {
        if (this.lastBookTick == 0) {
            this.lastBookTick = ArclightConstants.currentTick - 20;
        }
        if (this.lastBookTick + 20 > ArclightConstants.currentTick) {
            PacketUtils.ensureRunningOnSameThread(packetIn, (ServerGamePacketListenerImpl) (Object) this, this.minecraftServer);
            this.disconnect("Book edited too quickly!");
            return;
        }
        this.lastBookTick = ArclightConstants.currentTick;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void updateBookContents(List<String> p_244536_1_, int p_244536_2_) {
        ItemStack itemstack = this.player.inventory.getItem(p_244536_2_);
        if (itemstack.getItem() == Items.WRITABLE_BOOK) {
            ListTag listnbt = new ListTag();
            p_244536_1_.stream().map(StringTag::valueOf).forEach(listnbt::add);
            ItemStack old = itemstack.copy();
            itemstack.addTagElement("pages", listnbt);
            CraftEventFactory.handleEditBookEvent(player, p_244536_2_, old, itemstack);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void signBook(String p_244534_1_, List<String> p_244534_2_, int p_244534_3_) {
        ItemStack itemstack = this.player.inventory.getItem(p_244534_3_);
        if (itemstack.getItem() == Items.WRITABLE_BOOK) {
            ItemStack itemstack1 = new ItemStack(Items.WRITTEN_BOOK);
            CompoundTag compoundnbt = itemstack.getTag();
            if (compoundnbt != null) {
                itemstack1.setTag(compoundnbt.copy());
            }

            itemstack1.addTagElement("author", StringTag.valueOf(this.player.getName().getString()));
            itemstack1.addTagElement("title", StringTag.valueOf(p_244534_1_));
            ListTag listnbt = new ListTag();

            for (String s : p_244534_2_) {
                Component itextcomponent = new TextComponent(s);
                String s1 = Component.Serializer.toJson(itextcomponent);
                listnbt.add(StringTag.valueOf(s1));
            }

            itemstack1.addTagElement("pages", listnbt);
            this.player.inventory.setItem(p_244534_3_, CraftEventFactory.handleEditBookEvent(player, p_244534_3_, itemstack, itemstack1));
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleMovePlayer(ServerboundMovePlayerPacket packetplayinflying) {
        PacketUtils.ensureRunningOnSameThread(packetplayinflying, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
        if (containsInvalidValues(packetplayinflying)) {
            this.disconnect(new TranslatableComponent("multiplayer.disconnect.invalid_player_movement"));
        } else {
            ServerLevel worldserver = this.player.getLevel();
            if (!this.player.wonGame && !((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
                if (this.tickCount == 0) {
                    this.resetPosition();
                }
                if (this.awaitingPositionFromClient != null) {
                    if (this.tickCount - this.awaitingTeleportTime > 20) {
                        this.awaitingTeleportTime = this.tickCount;
                        this.teleport(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.yRot, this.player.xRot);
                    }
                    this.allowedPlayerTicks = 20;
                } else {
                    this.awaitingTeleportTime = this.tickCount;
                    if (this.player.isPassenger()) {
                        this.player.absMoveTo(this.player.getX(), this.player.getY(), this.player.getZ(), packetplayinflying.getYRot(this.player.yRot), packetplayinflying.getXRot(this.player.xRot));
                        this.player.getLevel().getChunkSource().move(this.player);
                        this.allowedPlayerTicks = 20;
                    } else {
                        double prevX = this.player.getX();
                        double prevY = this.player.getY();
                        double prevZ = this.player.getZ();
                        float prevYaw = this.player.yRot;
                        float prevPitch = this.player.xRot;
                        double d0 = this.player.getX();
                        double d1 = this.player.getY();
                        double d2 = this.player.getZ();
                        double d3 = this.player.getY();
                        double d4 = packetplayinflying.getX(this.player.getX());
                        double d5 = packetplayinflying.getY(this.player.getY());
                        double d6 = packetplayinflying.getZ(this.player.getZ());
                        float f = packetplayinflying.getYRot(this.player.yRot);
                        float f1 = packetplayinflying.getXRot(this.player.xRot);
                        double d7 = d4 - this.firstGoodX;
                        double d8 = d5 - this.firstGoodY;
                        double d9 = d6 - this.firstGoodZ;
                        double d10 = this.player.getDeltaMovement().lengthSqr();
                        double d11 = d7 * d7 + d8 * d8 + d9 * d9;
                        if (this.player.isSleeping()) {
                            if (d11 > 1.0) {
                                this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), packetplayinflying.getYRot(this.player.yRot), packetplayinflying.getXRot(this.player.xRot));
                            }
                        } else {
                            boolean flag;
                            ++this.receivedMovePacketCount;
                            int i = this.receivedMovePacketCount - this.knownMovePacketCount;
                            this.allowedPlayerTicks = (int) ((long) this.allowedPlayerTicks + (System.currentTimeMillis() / 50L - (long) this.lastTick));
                            this.allowedPlayerTicks = Math.max(this.allowedPlayerTicks, 1);
                            this.lastTick = (int) (System.currentTimeMillis() / 50L);
                            if (i > Math.max(this.allowedPlayerTicks, 5)) {
                                LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)", this.player.getName().getString(), i);
                                i = 1;
                            }
                            this.allowedPlayerTicks = packetplayinflying.hasRot || d11 > 0.0 ? --this.allowedPlayerTicks : 20;
                            double speed = this.player.abilities.flying ? (double) (this.player.abilities.flyingSpeed * 20.0f) : (double) (this.player.abilities.walkingSpeed * 10.0f);
                            if (!(this.player.isChangingDimension() || this.player.getLevel().getGameRules().getBoolean(GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK) && this.player.isFallFlying())) {
                                float f2;
                                float f3 = f2 = this.player.isFallFlying() ? 300.0f : 100.0f;
                                if (d11 - d10 > Math.max(f2, Math.pow(SpigotConfig.movedTooQuicklyMultiplier * (double) i * speed, 2.0)) && !this.isSingleplayerOwner()) {
                                    LOGGER.warn("{} moved too quickly! {},{},{}", this.player.getName().getString(), d7, d8, d9);
                                    this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.yRot, this.player.xRot);
                                    return;
                                }
                            }
                            AABB axisalignedbb = this.player.getBoundingBox();
                            d7 = d4 - this.lastGoodX;
                            d8 = d5 - this.lastGoodY;
                            d9 = d6 - this.lastGoodZ;
                            boolean bl = flag = d8 > 0.0;
                            if (this.player.isOnGround() && !packetplayinflying.isOnGround() && flag) {
                                this.player.jumpFromGround();
                            }
                            this.player.move(MoverType.PLAYER, new Vec3(d7, d8, d9));
                            this.player.setOnGround(packetplayinflying.isOnGround());
                            double d12 = d8;
                            d7 = d4 - this.player.getX();
                            d8 = d5 - this.player.getY();
                            if (d8 > -0.5 || d8 < 0.5) {
                                d8 = 0.0;
                            }
                            d9 = d6 - this.player.getZ();
                            d11 = d7 * d7 + d8 * d8 + d9 * d9;
                            boolean flag1 = false;
                            if (!this.player.isChangingDimension() && d11 > SpigotConfig.movedWronglyThreshold && !this.player.isSleeping() && !this.player.gameMode.isCreative() && this.player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                                flag1 = true;
                                LOGGER.warn("{} moved wrongly!", this.player.getName().getString());
                            }
                            this.player.absMoveTo(d4, d5, d6, f, f1);
                            if (!this.player.noPhysics && !this.player.isSleeping() && (flag1 && worldserver.noCollision(this.player, axisalignedbb) || this.isPlayerCollidingWithAnythingNew(worldserver, axisalignedbb))) {
                                this.teleport(d0, d1, d2, f, f1);
                            } else {
                                this.player.absMoveTo(prevX, prevY, prevZ, prevYaw, prevPitch);
                                CraftPlayer player = this.getPlayer();
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
                                if ((delta > 0.00390625 || deltaAngle > 10.0f) && !((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
                                    this.lastPosX = to.getX();
                                    this.lastPosY = to.getY();
                                    this.lastPosZ = to.getZ();
                                    this.lastYaw = to.getYaw();
                                    this.lastPitch = to.getPitch();
                                    if (from.getX() != Double.MAX_VALUE) {
                                        Location oldTo = to.clone();
                                        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
                                        this.server.getPluginManager().callEvent(event);
                                        if (event.isCancelled()) {
                                            this.teleport(from);
                                            return;
                                        }
                                        if (!oldTo.equals(event.getTo()) && !event.isCancelled()) {
                                            ((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                                            return;
                                        }
                                        if (!from.equals(this.getPlayer().getLocation()) && this.justTeleported) {
                                            this.justTeleported = false;
                                            return;
                                        }
                                    }
                                }
                                this.player.absMoveTo(d4, d5, d6, f, f1);
                                this.clientIsFloating = d12 >= -0.03125 && this.player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR && !this.minecraftServer.isFlightAllowed() && !this.player.abilities.mayfly && !this.player.hasEffect(MobEffects.LEVITATION) && !this.player.isFallFlying() && this.noBlocksAround(this.player) && !this.player.isAutoSpinAttack();
                                this.player.getLevel().getChunkSource().move(this.player);
                                this.player.doCheckFallDamage(this.player.getY() - d3, packetplayinflying.isOnGround());
                                if (flag) {
                                    this.player.fallDistance = 0.0f;
                                }
                                this.player.checkMovementStatistics(this.player.getX() - d0, this.player.getY() - d1, this.player.getZ() - d2);
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

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handlePlayerAction(ServerboundPlayerActionPacket packetplayinblockdig) {
        PacketUtils.ensureRunningOnSameThread(packetplayinblockdig, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        BlockPos blockposition = packetplayinblockdig.getPos();
        this.player.resetLastActionTime();
        ServerboundPlayerActionPacket.Action packetplayinblockdig_enumplayerdigtype = packetplayinblockdig.getAction();
        switch (packetplayinblockdig_enumplayerdigtype) {
            case SWAP_ITEM_WITH_OFFHAND: {
                if (!this.player.isSpectator()) {
                    ItemStack itemstack = this.player.getItemInHand(InteractionHand.OFF_HAND);
                    CraftItemStack mainHand = CraftItemStack.asCraftMirror(itemstack);
                    CraftItemStack offHand = CraftItemStack.asCraftMirror(this.player.getItemInHand(InteractionHand.MAIN_HAND));
                    PlayerSwapHandItemsEvent swapItemsEvent = new PlayerSwapHandItemsEvent(this.getPlayer(), mainHand.clone(), offHand.clone());
                    this.server.getPluginManager().callEvent(swapItemsEvent);
                    if (swapItemsEvent.isCancelled()) {
                        return;
                    }
                    if (swapItemsEvent.getOffHandItem().equals(offHand)) {
                        this.player.setItemInHand(InteractionHand.OFF_HAND, this.player.getItemInHand(InteractionHand.MAIN_HAND));
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
                            this.disconnect("You dropped your items too quickly (Hacking?)");
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
                this.player.gameMode.handleBlockBreakAction(blockposition, packetplayinblockdig_enumplayerdigtype, packetplayinblockdig.getDirection(), this.minecraftServer.getMaxBuildHeight());
                return;
            }
            default: {
                throw new IllegalArgumentException("Invalid player action");
            }
        }
    }

    @Inject(method = "handleUseItemOn", cancellable = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/level/ServerPlayer;getLevel()Lnet/minecraft/server/level/ServerLevel;"))
    private void arclight$frozenUseItem(ServerboundUseItemOnPacket packetIn, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
        if (!this.checkLimit(((TimestampedPacket) packetIn).bridge$timestamp())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private void arclight$checkDistance(ServerboundUseItemOnPacket packetIn, CallbackInfo ci) {
        this.player.releaseUsingItem();
    }

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

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleUseItem(ServerboundUseItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        if (!this.checkLimit(((TimestampedPacket) packet).bridge$timestamp())) {
            return;
        }
        ServerLevel worldserver = this.player.getLevel();
        InteractionHand enumhand = packet.getHand();
        ItemStack itemstack = this.player.getItemInHand(enumhand);
        this.player.resetLastActionTime();
        if (!itemstack.isEmpty()) {
            float f1 = this.player.xRot;
            float f2 = this.player.yRot;
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
            double d4 = (this.player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) ? 5.0 : 4.5;
            Vec3 vec3d2 = vec3d.add(f7 * d4, f6 * d4, f8 * d4);
            BlockHitResult movingobjectposition = this.player.level.clip(new ClipContext(vec3d, vec3d2, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.player));
            boolean cancelled;
            if (movingobjectposition == null || movingobjectposition.getType() != HitResult.Type.BLOCK) {
                PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.RIGHT_CLICK_AIR, itemstack, enumhand);
                cancelled = (event.useItemInHand() == Event.Result.DENY);
            } else if (((PlayerInteractionManagerBridge) this.player.gameMode).bridge$isFiredInteract()) {
                ((PlayerInteractionManagerBridge) this.player.gameMode).bridge$setFiredInteract(false);
                cancelled = ((PlayerInteractionManagerBridge) this.player.gameMode).bridge$getInteractResult();
            } else {
                BlockHitResult movingobjectpositionblock = movingobjectposition;
                PlayerInteractEvent event2 = CraftEventFactory.callPlayerInteractEvent(this.player, Action.RIGHT_CLICK_BLOCK, movingobjectpositionblock.getBlockPos(), movingobjectpositionblock.getDirection(), itemstack, true, enumhand);
                cancelled = (event2.useItemInHand() == Event.Result.DENY);
            }
            if (cancelled) {
                ((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity().updateInventory();
                return;
            }
            InteractionResult actionresulttype = this.player.gameMode.useItem(this.player, worldserver, itemstack, enumhand);
            if (actionresulttype.shouldSwing()) {
                this.player.swing(enumhand, true);
            }
        }
    }

    @Inject(method = "handleTeleportToEntityPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V"))
    private void arclight$spectateTeleport(ServerboundTeleportToEntityPacket packetIn, CallbackInfo ci) {
        ((ServerPlayerEntityBridge) this.player).bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause.SPECTATE);
    }

    @Inject(method = "handleResourcePackResponse", at = @At("HEAD"))
    private void arclight$handleResourcePackStatus(ServerboundResourcePackPacket packetIn, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packetIn, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
        this.server.getPluginManager().callEvent(new PlayerResourcePackStatusEvent(this.getPlayer(), PlayerResourcePackStatusEvent.Status.values()[packetIn.action.ordinal()]));
    }

    @Inject(method = "onDisconnect", cancellable = true, at = @At("HEAD"))
    private void arclight$returnIfProcessed(Component reason, CallbackInfo ci) {
        if (processedDisconnect) {
            ci.cancel();
        } else {
            processedDisconnect = true;
        }
    }

    @Redirect(method = "onDisconnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V"))
    public void arclight$captureQuit(PlayerList playerList, Component p_232641_1_, ChatType p_232641_2_, UUID p_232641_3_) {
        // do nothing
    }

    @Inject(method = "onDisconnect", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/players/PlayerList;remove(Lnet/minecraft/server/level/ServerPlayer;)V"))
    public void arclight$processQuit(Component reason, CallbackInfo ci) {
        String quitMessage = ArclightCaptures.getQuitMessage();
        if (quitMessage != null && quitMessage.length() > 0) {
            ((PlayerListBridge) this.minecraftServer.getPlayerList()).bridge$sendMessage(CraftChatMessage.fromString(quitMessage));
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V", cancellable = true, at = @At("HEAD"))
    private void arclight$updateCompassTarget(Packet<?> packetIn, GenericFutureListener<? extends Future<? super Void>> futureListeners, CallbackInfo ci) {
        if (packetIn == null || processedDisconnect) {
            ci.cancel();
            return;
        }
        if (packetIn instanceof ClientboundSetDefaultSpawnPositionPacket) {
            ClientboundSetDefaultSpawnPositionPacket packet6 = (ClientboundSetDefaultSpawnPositionPacket) packetIn;
            ((ServerPlayerEntityBridge) this.player).bridge$setCompassTarget(new Location(this.getPlayer().getWorld(), packet6.pos.getX(), packet6.pos.getY(), packet6.pos.getZ()));
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        if (packet.getSlot() >= 0 && packet.getSlot() < net.minecraft.world.entity.player.Inventory.getSelectionSize()) {
            PlayerItemHeldEvent event = new PlayerItemHeldEvent(this.getPlayer(), this.player.inventory.selected, packet.getSlot());
            this.server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                this.send(new ClientboundSetCarriedItemPacket(this.player.inventory.selected));
                this.player.resetLastActionTime();
                return;
            }
            if (this.player.inventory.selected != packet.getSlot() && this.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                this.player.stopUsingItem();
            }
            this.player.inventory.selected = packet.getSlot();
            this.player.resetLastActionTime();
        } else {
            LOGGER.warn("{} tried to set an invalid carried item", this.player.getName().getString());
            this.disconnect("Invalid hotbar selection (Hacking?)");
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleChat(ServerboundChatPacket packet) {
        if (this.minecraftServer.isStopped()) {
            return;
        }
        boolean isSync = packet.getMessage().startsWith("/");
        if (packet.getMessage().startsWith("/")) {
            PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
        }
        if (this.player.removed || this.player.getChatVisibility() == ChatVisiblity.HIDDEN) {
            this.send(new ClientboundChatPacket((new TranslatableComponent("chat.cannotSend")).withStyle(ChatFormatting.RED), ChatType.SYSTEM, Util.NIL_UUID));
        } else {
            this.player.resetLastActionTime();
            String s = org.apache.commons.lang3.StringUtils.normalizeSpace(packet.getMessage());
            for (int i = 0; i < s.length(); ++i) {
                if (!SharedConstants.isAllowedChatCharacter(s.charAt(i))) {
                    if (!isSync) {
                        class Disconnect extends Waitable {

                            @Override
                            protected Object evaluate() {
                                disconnect(new TranslatableComponent("multiplayer.disconnect.illegal_characters"));
                                return null;
                            }
                        }
                        Waitable waitable = new Disconnect();
                        ((MinecraftServerBridge) this.minecraftServer).bridge$queuedProcess(waitable);
                        try {
                            waitable.get();
                            return;
                        } catch (InterruptedException e3) {
                            Thread.currentThread().interrupt();
                            return;
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    this.disconnect(new TranslatableComponent("multiplayer.disconnect.illegal_characters"));
                    return;
                }
            }
            if (isSync) {
                try {
                    this.server.playerCommandState = true;
                    this.handleCommand(s);
                } finally {
                    this.server.playerCommandState = false;
                }
                this.server.playerCommandState = false;
            } else if (s.isEmpty()) {
                LOGGER.warn(this.player.getScoreboardName() + " tried to send an empty message");
            } else if (this.getPlayer().isConversing()) {
                String conversationInput = s;
                ((MinecraftServerBridge) this.minecraftServer).bridge$queuedProcess(() -> this.getPlayer().acceptConversationInput(conversationInput));
            } else if (this.player.getChatVisibility() == ChatVisiblity.SYSTEM) {
                this.send(new ClientboundChatPacket((new TranslatableComponent("chat.cannotSend")).withStyle(ChatFormatting.RED), ChatType.SYSTEM, Util.NIL_UUID));
            } else {
                this.chat(s, true);
            }
            this.chatSpamTickCount += 20;
            if (this.chatSpamTickCount > 200 && !this.minecraftServer.getPlayerList().isOp(this.player.getGameProfile())) {
                if (!isSync) {
                    class Disconnect2 extends Waitable {

                        @Override
                        protected Object evaluate() {
                            disconnect(new TranslatableComponent("disconnect.spam"));
                            return null;
                        }
                    }
                    Waitable waitable2 = new Disconnect2();
                    ((MinecraftServerBridge) this.minecraftServer).bridge$queuedProcess(waitable2);
                    try {
                        waitable2.get();
                        return;
                    } catch (InterruptedException e4) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (ExecutionException e2) {
                        throw new RuntimeException(e2);
                    }
                }
                this.disconnect(new TranslatableComponent("disconnect.spam"));
            }
        }
    }

    public void chat(String s, boolean async) {
        if (s.isEmpty() || this.player.getChatVisibility() == ChatVisiblity.HIDDEN) {
            return;
        }
        ServerGamePacketListenerImpl handler = (ServerGamePacketListenerImpl) (Object) this;
        if (!async && s.startsWith("/")) {
            this.handleCommand(s);
        } else if (this.player.getChatVisibility() != ChatVisiblity.SYSTEM) {
            Player thisPlayer = this.getPlayer();
            AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(async, thisPlayer, s, new LazyPlayerSet(this.minecraftServer));
            this.server.getPluginManager().callEvent(event);
            if (PlayerChatEvent.getHandlerList().getRegisteredListeners().length != 0) {
                PlayerChatEvent queueEvent = new PlayerChatEvent(thisPlayer, event.getMessage(), event.getFormat(), event.getRecipients());
                queueEvent.setCancelled(event.isCancelled());
                class SyncChat extends Waitable {

                    @Override
                    protected Object evaluate() {
                        Bukkit.getPluginManager().callEvent(queueEvent);
                        if (queueEvent.isCancelled()) {
                            return null;
                        }
                        String message = String.format(queueEvent.getFormat(), queueEvent.getPlayer().getDisplayName(), queueEvent.getMessage());
                        Component component = ForgeHooks.onServerChatEvent(handler, queueEvent.getMessage(), ForgeHooks.newChatWithLinks(message));
                        if (component == null) return null;
                        Bukkit.getConsoleSender().sendMessage(CraftChatMessage.fromComponent(component));
                        if (((LazyPlayerSet) queueEvent.getRecipients()).isLazy()) {
                            for (ServerPlayer player : minecraftServer.getPlayerList().players) {
                                ((ServerPlayerEntityBridge) player).bridge$sendMessage(component, thisPlayer.getUniqueId());
                            }
                        } else {
                            for (Player player2 : queueEvent.getRecipients()) {
                                ((ServerPlayerEntityBridge) ((CraftPlayer) player2).getHandle()).bridge$sendMessage(component, thisPlayer.getUniqueId());
                            }
                        }
                        return null;
                    }
                }
                Waitable waitable = new SyncChat();
                if (async) {
                    ((MinecraftServerBridge) minecraftServer).bridge$queuedProcess(waitable);
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
            Component chatWithLinks = ForgeHooks.newChatWithLinks(s);
            class ForgeChat extends Waitable<Void> {

                @Override
                protected Void evaluate() {
                    // this is called on main thread
                    Component component = ForgeHooks.onServerChatEvent(handler, event.getMessage(), chatWithLinks);
                    if (component == null) return null;
                    Bukkit.getConsoleSender().sendMessage(CraftChatMessage.fromComponent(component));
                    if (((LazyPlayerSet) event.getRecipients()).isLazy()) {
                        for (ServerPlayer recipient : minecraftServer.getPlayerList().players) {
                            ((ServerPlayerEntityBridge) recipient).bridge$sendMessage(component, thisPlayer.getUniqueId());
                        }
                    } else {
                        for (Player recipient2 : event.getRecipients()) {
                            ((ServerPlayerEntityBridge) ((CraftPlayer) recipient2).getHandle()).bridge$sendMessage(component, thisPlayer.getUniqueId());
                        }
                    }
                    return null;
                }
            }
            Waitable<Void> waitable = new ForgeChat();
            if (async) {
                ((MinecraftServerBridge) minecraftServer).bridge$queuedProcess(waitable);
            } else {
                waitable.run();
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void handleCommand(String s) {
        if (SpigotConfig.logCommands) {
            LOGGER.info(this.player.getScoreboardName() + " issued server command: " + s);
        }
        CraftPlayer player = this.getPlayer();
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, s, new LazyPlayerSet(this.minecraftServer));
        this.server.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        try {
            this.server.dispatchCommand(event.getPlayer(), event.getMessage().substring(1));
        } catch (CommandRuntimeException ex) {
            player.sendMessage(ChatColor.RED + "An internal error occurred while attempting to perform this command");
            java.util.logging.Logger.getLogger(ServerGamePacketListenerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleAnimate(ServerboundSwingPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        this.player.resetLastActionTime();
        float f1 = this.player.xRot;
        float f2 = this.player.yRot;
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
        double d4 = (this.player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) ? 5.0 : 4.5;
        Vec3 vec3d2 = vec3d.add(f7 * d4, f6 * d4, f8 * d4);
        HitResult result = this.player.level.clip(new ClipContext(vec3d, vec3d2, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.player));
        if (result == null || result.getType() != HitResult.Type.BLOCK) {
            CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_AIR, this.player.inventory.getSelected(), InteractionHand.MAIN_HAND);
        }
        PlayerAnimationEvent event = new PlayerAnimationEvent(this.getPlayer());
        this.server.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        this.player.swing(packet.getHand());
    }

    @Inject(method = "handlePlayerCommand", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"))
    private void arclight$toggleAction(ServerboundPlayerCommandPacket packetIn, CallbackInfo ci) {
        if (this.player.removed) {
            ci.cancel();
            return;
        }
        if (packetIn.getAction() == ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY || packetIn.getAction() == ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY) {
            PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this.getPlayer(), packetIn.getAction() == ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY);
            this.server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        } else if (packetIn.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING || packetIn.getAction() == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING) {
            PlayerToggleSprintEvent e2 = new PlayerToggleSprintEvent(this.getPlayer(), packetIn.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING);
            this.server.getPluginManager().callEvent(e2);
            if (e2.isCancelled()) {
                ci.cancel();
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleInteract(final ServerboundInteractPacket packetIn) {
        PacketUtils.ensureRunningOnSameThread(packetIn, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        final ServerLevel world = this.player.getLevel();
        final Entity entity = packetIn.getTarget(world);
        if (entity == player && !player.isSpectator()) {
            disconnect("Cannot interact with self!");
            return;
        }
        this.player.resetLastActionTime();
        this.player.setShiftKeyDown(packetIn.isUsingSecondaryAction());
        if (entity != null) {
            double d0 = 36.0D;
            if (this.player.distanceToSqr(entity) < 36.0D) {
                InteractionHand hand = packetIn.getHand();
                ItemStack itemstack = hand != null ? this.player.getItemInHand(hand).copy() : ItemStack.EMPTY;
                Optional<InteractionResult> optional = Optional.empty();

                final ItemStack itemInHand = this.player.getItemInHand((packetIn.getHand() == null) ? InteractionHand.MAIN_HAND : packetIn.getHand());
                if (packetIn.getAction() == ServerboundInteractPacket.Action.INTERACT || packetIn.getAction() == ServerboundInteractPacket.Action.INTERACT_AT) {
                    final boolean triggerLeashUpdate = itemInHand != null && itemInHand.getItem() == Items.LEAD && entity instanceof Mob;
                    final Item origItem = (this.player.inventory.getSelected() == null) ? null : this.player.inventory.getSelected().getItem();
                    PlayerInteractEntityEvent event;
                    if (packetIn.getAction() == ServerboundInteractPacket.Action.INTERACT) {
                        event = new PlayerInteractEntityEvent(this.getPlayer(), ((EntityBridge) entity).bridge$getBukkitEntity(), (packetIn.getHand() == InteractionHand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
                    } else {
                        final Vec3 target = packetIn.getLocation();
                        event = new PlayerInteractAtEntityEvent(this.getPlayer(), ((EntityBridge) entity).bridge$getBukkitEntity(), new Vector(target.x, target.y, target.z), (packetIn.getHand() == InteractionHand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
                    }
                    this.server.getPluginManager().callEvent(event);
                    if (entity instanceof AbstractFish && origItem != null && origItem.asItem() == Items.WATER_BUCKET && (event.isCancelled() || this.player.inventory.getSelected() == null || this.player.inventory.getSelected().getItem() != origItem)) {
                        this.send(new ClientboundAddMobPacket((LivingEntity) entity));
                        this.player.refreshContainer(this.player.containerMenu);
                    }
                    if (triggerLeashUpdate && (event.isCancelled() || this.player.inventory.getSelected() == null || this.player.inventory.getSelected().getItem() != origItem)) {
                        this.send(new ClientboundSetEntityLinkPacket(entity, ((Mob) entity).getLeashHolder()));
                    }
                    if (event.isCancelled() || this.player.inventory.getSelected() == null || this.player.inventory.getSelected().getItem() != origItem) {
                        this.send(new ClientboundSetEntityDataPacket(entity.getId(), entity.getEntityData(), true));
                    }
                    if (event.isCancelled()) {
                        return;
                    }
                }
                if (packetIn.getAction() == ServerboundInteractPacket.Action.INTERACT) {
                    optional = Optional.of(this.player.interactOn(entity, hand));
                    if (!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
                        this.player.refreshContainer(this.player.containerMenu);
                    }
                } else if (packetIn.getAction() == ServerboundInteractPacket.Action.INTERACT_AT) {
                    if (net.minecraftforge.common.ForgeHooks.onInteractEntityAt(player, entity, packetIn.getLocation(), hand) != null)
                        return;
                    optional = Optional.of(entity.interactAt(this.player, packetIn.getLocation(), hand));
                    if (!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
                        this.player.refreshContainer(this.player.containerMenu);
                    }
                } else if (packetIn.getAction() == ServerboundInteractPacket.Action.ATTACK) {
                    if ((entity instanceof ItemEntity && AstralSorceryHooks.notInteractable(entity)) || entity instanceof ExperienceOrb || entity instanceof AbstractArrow || (entity == this.player && !this.player.isSpectator())) {
                        this.disconnect(new TranslatableComponent("multiplayer.disconnect.invalid_entity_attacked"));
                        LOGGER.warn("Player {} tried to attack an invalid entity", this.player.getName().getString());
                        return;
                    }
                    this.player.attack(entity);
                    if (!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
                        this.player.refreshContainer(this.player.containerMenu);
                    }
                }
                if (optional.isPresent() && optional.get().consumesAction()) {
                    CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(this.player, itemstack, entity);
                    if (optional.get().shouldSwing()) {
                        this.player.swing(hand, true);
                    }
                }
            }
        }
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
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        this.player.resetLastActionTime();
        if (this.player.containerMenu.containerId == packet.getContainerId() && this.player.containerMenu.isSynched(this.player) && this.player.containerMenu.stillValid(this.player)) {
            boolean cancelled = this.player.isSpectator();
            if (packet.getSlotNum() < -1 && packet.getSlotNum() != -999) {
                return;
            }
            ArclightCaptures.captureContainerOwner(this.player);
            InventoryView inventory = ((ContainerBridge) this.player.containerMenu).bridge$getBukkitView();
            ArclightCaptures.resetContainerOwner();
            InventoryType.SlotType type = inventory.getSlotType(packet.getSlotNum());
            org.bukkit.event.inventory.ClickType click = org.bukkit.event.inventory.ClickType.UNKNOWN;
            InventoryAction action = InventoryAction.UNKNOWN;
            ItemStack itemstack = ItemStack.EMPTY;
            switch (packet.getClickType()) {
                case PICKUP: {
                    if (packet.getButtonNum() == 0) {
                        click = org.bukkit.event.inventory.ClickType.LEFT;
                    } else if (packet.getButtonNum() == 1) {
                        click = org.bukkit.event.inventory.ClickType.RIGHT;
                    }
                    if (packet.getButtonNum() != 0 && packet.getButtonNum() != 1) {
                        break;
                    }
                    action = InventoryAction.NOTHING;
                    if (packet.getSlotNum() == -999) {
                        if (!this.player.inventory.getCarried().isEmpty()) {
                            action = ((packet.getButtonNum() == 0) ? InventoryAction.DROP_ALL_CURSOR : InventoryAction.DROP_ONE_CURSOR);
                            break;
                        }
                        break;
                    } else {
                        if (packet.getSlotNum() < 0) {
                            action = InventoryAction.NOTHING;
                            break;
                        }
                        Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                        if (slot == null) {
                            break;
                        }
                        ItemStack clickedItem = slot.getItem();
                        ItemStack cursor = this.player.inventory.getCarried();
                        if (clickedItem.isEmpty()) {
                            if (!cursor.isEmpty()) {
                                action = ((packet.getButtonNum() == 0) ? InventoryAction.PLACE_ALL : InventoryAction.PLACE_ONE);
                                break;
                            }
                            break;
                        } else {
                            if (!slot.mayPickup(this.player)) {
                                break;
                            }
                            if (cursor.isEmpty()) {
                                action = ((packet.getButtonNum() == 0) ? InventoryAction.PICKUP_ALL : InventoryAction.PICKUP_HALF);
                                break;
                            }
                            if (slot.mayPlace(cursor)) {
                                if (clickedItem.sameItem(cursor) && ItemStack.tagMatches(clickedItem, cursor)) {
                                    int toPlace = (packet.getButtonNum() == 0) ? cursor.getCount() : 1;
                                    toPlace = Math.min(toPlace, clickedItem.getMaxStackSize() - clickedItem.getCount());
                                    toPlace = Math.min(toPlace, slot.container.getMaxStackSize() - clickedItem.getCount());
                                    if (toPlace == 1) {
                                        action = InventoryAction.PLACE_ONE;
                                        break;
                                    }
                                    if (toPlace == cursor.getCount()) {
                                        action = InventoryAction.PLACE_ALL;
                                        break;
                                    }
                                    if (toPlace < 0) {
                                        action = ((toPlace != -1) ? InventoryAction.PICKUP_SOME : InventoryAction.PICKUP_ONE);
                                        break;
                                    }
                                    if (toPlace != 0) {
                                        action = InventoryAction.PLACE_SOME;
                                        break;
                                    }
                                    break;
                                } else {
                                    if (cursor.getCount() <= slot.getMaxStackSize()) {
                                        action = InventoryAction.SWAP_WITH_CURSOR;
                                        break;
                                    }
                                    break;
                                }
                            } else {
                                if (cursor.getItem() == clickedItem.getItem() && ItemStack.tagMatches(cursor, clickedItem) && clickedItem.getCount() >= 0 && clickedItem.getCount() + cursor.getCount() <= cursor.getMaxStackSize()) {
                                    action = InventoryAction.PICKUP_ALL;
                                    break;
                                }
                                break;
                            }
                        }
                    }
                }
                case QUICK_MOVE: {
                    if (packet.getButtonNum() == 0) {
                        click = org.bukkit.event.inventory.ClickType.SHIFT_LEFT;
                    } else if (packet.getButtonNum() == 1) {
                        click = org.bukkit.event.inventory.ClickType.SHIFT_RIGHT;
                    }
                    if (packet.getButtonNum() != 0 && packet.getButtonNum() != 1) {
                        break;
                    }
                    if (packet.getSlotNum() < 0) {
                        action = InventoryAction.NOTHING;
                        break;
                    }
                    Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                    if (slot != null && slot.mayPickup(this.player) && slot.hasItem()) {
                        action = InventoryAction.MOVE_TO_OTHER_INVENTORY;
                        break;
                    }
                    action = InventoryAction.NOTHING;
                    break;
                }
                case SWAP: {
                    if ((packet.getButtonNum() < 0 || packet.getButtonNum() >= 9) && packet.getButtonNum() != 40) {
                        break;
                    }
                    click = packet.getButtonNum() == 40 ? ClickType.SWAP_OFFHAND : ClickType.NUMBER_KEY;
                    Slot clickedSlot = this.player.containerMenu.getSlot(packet.getSlotNum());
                    if (!clickedSlot.mayPickup(this.player)) {
                        action = InventoryAction.NOTHING;
                        break;
                    }
                    ItemStack hotbar = this.player.inventory.getItem(packet.getButtonNum());
                    boolean canCleanSwap = hotbar.isEmpty() || (clickedSlot.container == this.player.inventory && clickedSlot.mayPlace(hotbar));
                    if (clickedSlot.hasItem()) {
                        if (canCleanSwap) {
                            action = InventoryAction.HOTBAR_SWAP;
                            break;
                        }
                        action = InventoryAction.HOTBAR_MOVE_AND_READD;
                        break;
                    } else {
                        if (!clickedSlot.hasItem() && !hotbar.isEmpty() && clickedSlot.mayPlace(hotbar)) {
                            action = InventoryAction.HOTBAR_SWAP;
                            break;
                        }
                        action = InventoryAction.NOTHING;
                        break;
                    }
                }
                case CLONE: {
                    if (packet.getButtonNum() != 2) {
                        click = org.bukkit.event.inventory.ClickType.UNKNOWN;
                        action = InventoryAction.UNKNOWN;
                        break;
                    }
                    click = org.bukkit.event.inventory.ClickType.MIDDLE;
                    if (packet.getSlotNum() < 0) {
                        action = InventoryAction.NOTHING;
                        break;
                    }
                    Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                    if (slot != null && slot.hasItem() && this.player.abilities.instabuild && this.player.inventory.getCarried().isEmpty()) {
                        action = InventoryAction.CLONE_STACK;
                        break;
                    }
                    action = InventoryAction.NOTHING;
                    break;
                }
                case THROW: {
                    if (packet.getSlotNum() < 0) {
                        click = org.bukkit.event.inventory.ClickType.LEFT;
                        if (packet.getButtonNum() == 1) {
                            click = org.bukkit.event.inventory.ClickType.RIGHT;
                        }
                        action = InventoryAction.NOTHING;
                        break;
                    }
                    if (packet.getButtonNum() == 0) {
                        click = org.bukkit.event.inventory.ClickType.DROP;
                        Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                        if (slot != null && slot.hasItem() && slot.mayPickup(this.player) && !slot.getItem().isEmpty() && slot.getItem().getItem() != Item.byBlock(Blocks.AIR)) {
                            action = InventoryAction.DROP_ONE_SLOT;
                            break;
                        }
                        action = InventoryAction.NOTHING;
                        break;
                    } else {
                        if (packet.getButtonNum() != 1) {
                            break;
                        }
                        click = org.bukkit.event.inventory.ClickType.CONTROL_DROP;
                        Slot slot = this.player.containerMenu.getSlot(packet.getSlotNum());
                        if (slot != null && slot.hasItem() && slot.mayPickup(this.player) && !slot.getItem().isEmpty() && slot.getItem().getItem() != Item.byBlock(Blocks.AIR)) {
                            action = InventoryAction.DROP_ALL_SLOT;
                            break;
                        }
                        action = InventoryAction.NOTHING;
                        break;
                    }
                }
                case QUICK_CRAFT: {
                    itemstack = this.player.containerMenu.clicked(packet.getSlotNum(), packet.getButtonNum(), packet.getClickType(), this.player);
                    break;
                }
                case PICKUP_ALL: {
                    click = org.bukkit.event.inventory.ClickType.DOUBLE_CLICK;
                    action = InventoryAction.NOTHING;
                    if (packet.getSlotNum() < 0 || this.player.inventory.getCarried().isEmpty()) {
                        break;
                    }
                    ItemStack cursor2 = this.player.inventory.getCarried();
                    action = InventoryAction.NOTHING;
                    if (inventory.getTopInventory().contains(CraftMagicNumbers.getMaterial(cursor2.getItem())) || inventory.getBottomInventory().contains(CraftMagicNumbers.getMaterial(cursor2.getItem()))) {
                        action = InventoryAction.COLLECT_TO_CURSOR;
                        break;
                    }
                    break;
                }
            }
            if (packet.getClickType() != net.minecraft.world.inventory.ClickType.QUICK_CRAFT) {
                InventoryClickEvent event;
                if (click == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
                    event = new InventoryClickEvent(inventory, type, packet.getSlotNum(), click, action, packet.getButtonNum());
                } else {
                    event = new InventoryClickEvent(inventory, type, packet.getSlotNum(), click, action);
                }
                Inventory top = inventory.getTopInventory();
                if (packet.getSlotNum() == 0 && top instanceof org.bukkit.inventory.CraftingInventory) {
                    Recipe recipe = ((org.bukkit.inventory.CraftingInventory) top).getRecipe();
                    if (recipe != null) {
                        if (click == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
                            event = new CraftItemEvent(recipe, inventory, type, packet.getSlotNum(), click, action, packet.getButtonNum());
                        } else {
                            event = new CraftItemEvent(recipe, inventory, type, packet.getSlotNum(), click, action);
                        }
                    }
                }
                if (packet.getSlotNum() == 2 && top instanceof SmithingInventory) {
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
                AbstractContainerMenu oldContainer = this.player.containerMenu;
                this.server.getPluginManager().callEvent(event);
                if (this.player.containerMenu != oldContainer) {
                    return;
                }
                switch (event.getResult()) {
                    case DEFAULT:
                    case ALLOW: {
                        itemstack = this.player.containerMenu.clicked(packet.getSlotNum(), packet.getButtonNum(), packet.getClickType(), this.player);
                        break;
                    }
                    case DENY: {
                        switch (action) {
                            case PICKUP_ALL:
                            case MOVE_TO_OTHER_INVENTORY:
                            case HOTBAR_MOVE_AND_READD:
                            case HOTBAR_SWAP:
                            case COLLECT_TO_CURSOR:
                            case UNKNOWN: {
                                this.player.refreshContainer(this.player.containerMenu);
                                break;
                            }
                            case PICKUP_SOME:
                            case PICKUP_HALF:
                            case PICKUP_ONE:
                            case PLACE_ALL:
                            case PLACE_SOME:
                            case PLACE_ONE:
                            case SWAP_WITH_CURSOR: {
                                this.player.connection.send(new ClientboundContainerSetSlotPacket(-1, -1, this.player.inventory.getCarried()));
                                this.player.connection.send(new ClientboundContainerSetSlotPacket(this.player.containerMenu.containerId, packet.getSlotNum(), this.player.containerMenu.getSlot(packet.getSlotNum()).getItem()));
                                break;
                            }
                            case DROP_ALL_SLOT:
                            case DROP_ONE_SLOT: {
                                this.player.connection.send(new ClientboundContainerSetSlotPacket(this.player.containerMenu.containerId, packet.getSlotNum(), this.player.containerMenu.getSlot(packet.getSlotNum()).getItem()));
                                break;
                            }
                            case DROP_ALL_CURSOR:
                            case DROP_ONE_CURSOR:
                            case CLONE_STACK: {
                                this.player.connection.send(new ClientboundContainerSetSlotPacket(-1, -1, this.player.inventory.getCarried()));
                                break;
                            }
                        }
                        return;
                    }
                }
                if (event instanceof CraftItemEvent || event instanceof SmithItemEvent) {
                    this.player.refreshContainer(this.player.containerMenu);
                }
            }
            if (ItemStack.matches(packet.getItem(), itemstack)) {
                this.player.connection.send(new ClientboundContainerAckPacket(packet.getContainerId(), packet.getUid(), true));
                this.player.ignoreSlotUpdateHack = true;
                this.player.containerMenu.broadcastChanges();
                this.player.broadcastCarriedItem();
                this.player.ignoreSlotUpdateHack = false;
            } else {
                this.expectedAcks.put(this.player.containerMenu.containerId, packet.getUid());
                this.player.connection.send(new ClientboundContainerAckPacket(packet.getContainerId(), packet.getUid(), false));
                this.player.containerMenu.setSynched(this.player, false);
                NonNullList<ItemStack> nonnulllist1 = NonNullList.create();
                for (int j = 0; j < this.player.containerMenu.slots.size(); ++j) {
                    ItemStack itemstack2 = this.player.containerMenu.slots.get(j).getItem();
                    nonnulllist1.add(itemstack2.isEmpty() ? ItemStack.EMPTY : itemstack2);
                }
                this.player.refreshContainer(this.player.containerMenu, nonnulllist1);
            }
        }
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
        PacketUtils.ensureRunningOnSameThread(packetplayinsetcreativeslot, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
        if (this.player.gameMode.isCreative()) {
            final boolean flag = packetplayinsetcreativeslot.getSlotNum() < 0;
            ItemStack itemstack = packetplayinsetcreativeslot.getItem();
            final CompoundTag nbttagcompound = itemstack.getTagElement("BlockEntityTag");
            if (!itemstack.isEmpty() && nbttagcompound != null && nbttagcompound.contains("x") && nbttagcompound.contains("y") && nbttagcompound.contains("z")) {
                final BlockPos blockposition = new BlockPos(nbttagcompound.getInt("x"), nbttagcompound.getInt("y"), nbttagcompound.getInt("z"));
                final BlockEntity tileentity = this.player.level.getBlockEntity(blockposition);
                if (tileentity != null) {
                    final CompoundTag nbttagcompound2 = tileentity.save(new CompoundTag());
                    nbttagcompound2.remove("x");
                    nbttagcompound2.remove("y");
                    nbttagcompound2.remove("z");
                    itemstack.addTagElement("BlockEntityTag", nbttagcompound2);
                }
            }
            final boolean flag2 = packetplayinsetcreativeslot.getSlotNum() >= 1 && packetplayinsetcreativeslot.getSlotNum() <= 45;
            boolean flag3 = itemstack.isEmpty() || (itemstack.getDamageValue() >= 0 && itemstack.getCount() <= 64 && !itemstack.isEmpty());
            if (flag || (flag2 && !ItemStack.matches(this.player.inventoryMenu.getSlot(packetplayinsetcreativeslot.getSlotNum()).getItem(), packetplayinsetcreativeslot.getItem()))) {
                final InventoryView inventory = ((ContainerBridge) this.player.inventoryMenu).bridge$getBukkitView();
                final org.bukkit.inventory.ItemStack item = CraftItemStack.asBukkitCopy(packetplayinsetcreativeslot.getItem());
                InventoryType.SlotType type = InventoryType.SlotType.QUICKBAR;
                if (flag) {
                    type = InventoryType.SlotType.OUTSIDE;
                } else if (packetplayinsetcreativeslot.getSlotNum() < 36) {
                    if (packetplayinsetcreativeslot.getSlotNum() >= 5 && packetplayinsetcreativeslot.getSlotNum() < 9) {
                        type = InventoryType.SlotType.ARMOR;
                    } else {
                        type = InventoryType.SlotType.CONTAINER;
                    }
                }
                final InventoryCreativeEvent event = new InventoryCreativeEvent(inventory, type, flag ? -999 : packetplayinsetcreativeslot.getSlotNum(), item);
                this.server.getPluginManager().callEvent(event);
                itemstack = CraftItemStack.asNMSCopy(event.getCursor());
                switch (event.getResult()) {
                    case ALLOW: {
                        flag3 = true;
                    }
                    case DENY: {
                        if (packetplayinsetcreativeslot.getSlotNum() >= 0) {
                            this.player.connection.send(new ClientboundContainerSetSlotPacket(this.player.inventoryMenu.containerId, packetplayinsetcreativeslot.getSlotNum(), this.player.inventoryMenu.getSlot(packetplayinsetcreativeslot.getSlotNum()).getItem()));
                            this.player.connection.send(new ClientboundContainerSetSlotPacket(-1, -1, ItemStack.EMPTY));
                        }
                        return;
                    }
                }
            }
            if (flag2 && flag3) {
                if (itemstack.isEmpty()) {
                    this.player.inventoryMenu.setItem(packetplayinsetcreativeslot.getSlotNum(), ItemStack.EMPTY);
                } else {
                    this.player.inventoryMenu.setItem(packetplayinsetcreativeslot.getSlotNum(), itemstack);
                }
                this.player.inventoryMenu.setSynched(this.player, true);
                this.player.inventoryMenu.broadcastChanges();
            } else if (flag && flag3 && this.dropSpamTickCount < 200) {
                this.dropSpamTickCount += 20;
                this.player.drop(itemstack, true);
            }
        }
    }

    @Inject(method = "handleContainerAck", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"))
    private void arclight$noTransaction(ServerboundContainerAckPacket packetIn, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "updateSignText", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"))
    private void arclight$noSignEdit(ServerboundSignUpdatePacket p_244542_1_, List<String> p_244542_2_, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
    }

    private Component[] arclight$lines;

    @Inject(method = "updateSignText", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SignBlockEntity;isEditable()Z"))
    public void arclight$onSignChangePre(ServerboundSignUpdatePacket p_244542_1_, List<String> p_244542_2_, CallbackInfo ci) {
        String[] lines = p_244542_2_.toArray(new String[0]);
        Player player = ((CraftServer) Bukkit.getServer()).getPlayer(this.player);
        CraftBlock block = CraftBlock.at(this.player.level, p_244542_1_.getPos());
        String[] bukkitLines = new String[lines.length];
        for (int i = 0; i < lines.length; i++) {
            bukkitLines[i] = ChatFormatting.stripFormatting(
                new TextComponent(
                    Objects.requireNonNull(
                        ChatFormatting.stripFormatting(lines[i])
                    )
                ).getString()
            );
        }
        SignChangeEvent event = new SignChangeEvent(block, player, bukkitLines);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            for (int i = 0; i < lines.length; i++) {
                arclight$lines = CraftSign.sanitizeLines(event.getLines());
            }
        }
    }

    @Redirect(method = "updateSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SignBlockEntity;setMessage(ILnet/minecraft/network/chat/Component;)V"))
    public void arclight$onSignChangePost(SignBlockEntity signTileEntity, int line, Component signText) {
        if (arclight$lines != null) {
            signTileEntity.setMessage(line, arclight$lines[line]);
            if (line == arclight$lines.length - 1) {
                arclight$lines = null;
                ((SignTileEntityBridge) signTileEntity).bridge$setEditable(false);
            }
        }
    }

    @Inject(method = "handleKeepAlive", at = @At("HEAD"))
    private void arclight$syncKeepAlive(ServerboundKeepAlivePacket packetIn, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packetIn, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this, this.player.getLevel());
        if (this.player.abilities.mayfly && this.player.abilities.flying != packet.isFlying()) {
            PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(this.server.getPlayer(this.player), packet.isFlying());
            this.server.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                this.player.abilities.flying = packet.isFlying();
            } else {
                this.player.onUpdateAbilities();
            }
        }
    }

    private static final ResourceLocation CUSTOM_REGISTER = new ResourceLocation("register");
    private static final ResourceLocation CUSTOM_UNREGISTER = new ResourceLocation("unregister");

    @Inject(method = "handleCustomPayload", cancellable = true, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/fml/network/NetworkHooks;onCustomPayload(Lnet/minecraftforge/fml/network/ICustomPacket;Lnet/minecraft/network/Connection;)Z"))
    private void arclight$customPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (packet.identifier.equals(CUSTOM_REGISTER)) {
            try {
                String channels = packet.data.toString(Charsets.UTF_8);
                for (String channel : channels.split("\0")) {
                    if (!StringUtil.isNullOrEmpty(channel)) {
                        this.getPlayer().addChannel(channel);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Couldn't register custom payload", ex);
                this.disconnect("Invalid payload REGISTER!");
                ci.cancel();
            }
        } else if (packet.identifier.equals(CUSTOM_UNREGISTER)) {
            try {
                final String channels = packet.data.toString(Charsets.UTF_8);
                for (String channel : channels.split("\0")) {
                    if (!StringUtil.isNullOrEmpty(channel)) {
                        this.getPlayer().removeChannel(channel);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Couldn't unregister custom payload", ex);
                this.disconnect("Invalid payload UNREGISTER!");
                ci.cancel();
            }
        } else {
            try {
                int readerIndex = packet.data.readerIndex();
                final byte[] data = new byte[packet.data.readableBytes()];
                packet.data.readBytes(data);
                this.server.getMessenger().dispatchIncomingMessage(((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity(), packet.identifier.toString(), data);
                packet.data.readerIndex(readerIndex);
            } catch (Exception ex) {
                LOGGER.error("Couldn't dispatch custom payload", ex);
                this.disconnect("Invalid custom payload!");
                ci.cancel();
            }
        }
    }

    public final boolean isDisconnected() {
        return !((ServerPlayerEntityBridge) this.player).bridge$isJoining() && !this.connection.isConnected();
    }

    @Override
    public boolean bridge$isDisconnected() {
        return this.isDisconnected();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void teleport(double x, double y, double z, float yaw, float pitch, Set<ClientboundPlayerPositionPacket.RelativeArgument> relativeSet) {
        PlayerTeleportEvent.TeleportCause cause = arclight$cause == null ? PlayerTeleportEvent.TeleportCause.UNKNOWN : arclight$cause;
        arclight$cause = null;
        Player player = this.getPlayer();
        Location from = player.getLocation();
        Location to = new Location(this.getPlayer().getWorld(), x, y, z, yaw, pitch);
        if (!from.equals(to)) {
            PlayerTeleportEvent event = new PlayerTeleportEvent(player, from.clone(), to.clone(), cause);
            this.server.getPluginManager().callEvent(event);
            if (event.isCancelled() || !to.equals(event.getTo())) {
                relativeSet.clear();
                to = (event.isCancelled() ? event.getFrom() : event.getTo());
                x = to.getX();
                y = to.getY();
                z = to.getZ();
                yaw = to.getYaw();
                pitch = to.getPitch();
            }
        }

        if (Float.isNaN(yaw)) {
            yaw = 0.0f;
        }
        if (Float.isNaN(pitch)) {
            pitch = 0.0f;
        }
        this.internalTeleport(x, y, z, yaw, pitch, relativeSet);
    }

    public void a(double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        this.a(d0, d1, d2, f, f1, Collections.emptySet(), cause);
    }

    public void a(double d0, double d1, double d2, float f, float f1, Set<ClientboundPlayerPositionPacket.RelativeArgument> set, PlayerTeleportEvent.TeleportCause cause) {
        bridge$pushTeleportCause(cause);
        this.teleport(d0, d1, d2, f, f1, set);
    }

    private void internalTeleport(double d0, double d1, double d2, float f, float f1, Set<ClientboundPlayerPositionPacket.RelativeArgument> set) {
        if (Float.isNaN(f)) {
            f = 0.0f;
        }
        if (Float.isNaN(f1)) {
            f1 = 0.0f;
        }
        this.justTeleported = true;
        double d3 = set.contains(ClientboundPlayerPositionPacket.RelativeArgument.X) ? this.player.getX() : 0.0;
        double d4 = set.contains(ClientboundPlayerPositionPacket.RelativeArgument.Y) ? this.player.getY() : 0.0;
        double d5 = set.contains(ClientboundPlayerPositionPacket.RelativeArgument.Z) ? this.player.getZ() : 0.0;
        float f2 = set.contains(ClientboundPlayerPositionPacket.RelativeArgument.Y_ROT) ? this.player.yRot : 0.0f;
        float f3 = set.contains(ClientboundPlayerPositionPacket.RelativeArgument.X_ROT) ? this.player.xRot : 0.0f;
        this.awaitingPositionFromClient = new Vec3(d0, d1, d2);
        if (++this.awaitingTeleport == Integer.MAX_VALUE) {
            this.awaitingTeleport = 0;
        }
        this.lastPosX = this.awaitingPositionFromClient.x;
        this.lastPosY = this.awaitingPositionFromClient.y;
        this.lastPosZ = this.awaitingPositionFromClient.z;
        this.lastYaw = f;
        this.lastPitch = f1;
        this.awaitingTeleportTime = this.tickCount;
        this.player.absMoveTo(d0, d1, d2, f, f1);
        this.player.connection.send(new ClientboundPlayerPositionPacket(d0 - d3, d1 - d4, d2 - d5, f - f2, f1 - f3, set, this.awaitingTeleport));
    }

    public void teleport(Location dest) {
        this.internalTeleport(dest.getX(), dest.getY(), dest.getZ(), dest.getYaw(), dest.getPitch(), Collections.emptySet());
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
}
