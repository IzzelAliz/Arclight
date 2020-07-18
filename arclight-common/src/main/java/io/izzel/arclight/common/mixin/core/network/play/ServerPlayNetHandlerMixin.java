package io.izzel.arclight.common.mixin.core.network.play;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.network.play.TimestampedPacket;
import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.server.management.PlayerInteractionManagerBridge;
import io.izzel.arclight.common.bridge.tileentity.SignTileEntityBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import it.unimi.dsi.fastutil.ints.Int2ShortMap;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.fish.AbstractFishEntity;
import net.minecraft.entity.player.ChatVisibility;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.MerchantContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.WritableBookItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SConfirmTransactionPacket;
import net.minecraft.network.play.server.SDisconnectPacket;
import net.minecraft.network.play.server.SEntityMetadataPacket;
import net.minecraft.network.play.server.SHeldItemChangePacket;
import net.minecraft.network.play.server.SMountEntityPacket;
import net.minecraft.network.play.server.SMoveVehiclePacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.network.play.server.SSetSlotPacket;
import net.minecraft.network.play.server.SSpawnMobPacket;
import net.minecraft.network.play.server.SSpawnPositionPacket;
import net.minecraft.potion.Effects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.server.ServerWorld;
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
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
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
import org.bukkit.util.Vector;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@Mixin(ServerPlayNetHandler.class)
public abstract class ServerPlayNetHandlerMixin implements ServerPlayNetHandlerBridge {

    // @formatter:off
    @Shadow(aliases = {"server", "field_147367_d"}, remap = false) @Final private MinecraftServer minecraftServer;
    @Shadow public ServerPlayerEntity player;
    @Shadow @Final public NetworkManager netManager;
    @Shadow public abstract void onDisconnect(ITextComponent reason);
    @Shadow private static boolean isMoveVehiclePacketInvalid(CMoveVehiclePacket packetIn) { return false; }
    @Shadow private Entity lowestRiddenEnt;
    @Shadow private double lowestRiddenX;
    @Shadow private double lowestRiddenY;
    @Shadow private double lowestRiddenZ;
    @Shadow protected abstract boolean func_217264_d();
    @Shadow private double lowestRiddenX1;
    @Shadow private double lowestRiddenY1;
    @Shadow private double lowestRiddenZ1;
    @Shadow private boolean vehicleFloating;
    @Shadow private int movePacketCounter;
    @Shadow private int lastMovePacketCounter;
    @Shadow private Vec3d targetPos;
    @Shadow private static boolean isMovePlayerPacketInvalid(CPlayerPacket packetIn) { return false; }
    @Shadow private int networkTickCount;
    @Shadow public abstract void captureCurrentPosition();
    @Shadow private int lastPositionUpdate;
    @Shadow public abstract void setPlayerLocation(double x, double y, double z, float yaw, float pitch);
    @Shadow private double firstGoodX;
    @Shadow private double firstGoodY;
    @Shadow private double firstGoodZ;
    @Shadow @Final private static Logger LOGGER;
    @Shadow protected abstract boolean isPlayerNotInBlock(IWorldReader p_223133_1_);
    @Shadow private double lastGoodX;
    @Shadow private double lastGoodY;
    @Shadow private double lastGoodZ;
    @Shadow private boolean floating;
    @Shadow private int teleportId;
    @Shadow public abstract void sendPacket(IPacket<?> packetIn);
    @Shadow private int chatSpamThresholdCount;
    @Shadow @Final private Int2ShortMap pendingTransactions;
    @Shadow private int itemDropThreshold;
    // @formatter:on

    private static final int SURVIVAL_PLACE_DISTANCE_SQUARED = 6 * 6;
    private static final int CREATIVE_PLACE_DISTANCE_SQUARED = 7 * 7;
    private CraftServer server;
    public boolean processedDisconnect;
    private int allowedPlayerTicks;
    private int dropCount;
    private int lastTick;
    private int lastBookTick;
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
    private void arclight$init(MinecraftServer server, NetworkManager networkManagerIn, ServerPlayerEntity playerIn, CallbackInfo ci) {
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
    public void disconnect(ITextComponent textComponent) {
        this.disconnect(CraftChatMessage.fromComponent(textComponent));
    }

    public void disconnect(String s) {
        if (this.processedDisconnect) {
            return;
        }
        String leaveMessage = TextFormatting.YELLOW + this.player.getScoreboardName() + " left the game.";
        PlayerKickEvent event = new PlayerKickEvent(this.server.getPlayer(this.player), s, leaveMessage);
        if (this.server.getServer().isServerRunning()) {
            this.server.getPluginManager().callEvent(event);
        }
        if (event.isCancelled()) {
            return;
        }
        s = event.getReason();
        StringTextComponent textComponent = new StringTextComponent(s);
        this.netManager.sendPacket(new SDisconnectPacket(textComponent), future -> this.netManager.closeChannel(textComponent));
        this.onDisconnect(textComponent);
        this.netManager.disableAutoRead();
        this.minecraftServer.runImmediately(this.netManager::handleDisconnection);
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
    public void processVehicleMove(final CMoveVehiclePacket packetplayinvehiclemove) {
        PacketThreadUtil.checkThreadAndEnqueue(packetplayinvehiclemove, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        if (isMoveVehiclePacketInvalid(packetplayinvehiclemove)) {
            this.disconnect(new TranslationTextComponent("multiplayer.disconnect.invalid_vehicle_movement"));
        } else {
            Entity entity = this.player.getLowestRidingEntity();
            if (entity != this.player && entity.getControllingPassenger() == this.player && entity == this.lowestRiddenEnt) {
                ServerWorld worldserver = this.player.getServerWorld();
                double d0 = entity.posX;
                double d2 = entity.posY;
                double d3 = entity.posZ;
                double d4 = packetplayinvehiclemove.getX();
                double d5 = packetplayinvehiclemove.getY();
                double d6 = packetplayinvehiclemove.getZ();
                float f = packetplayinvehiclemove.getYaw();
                float f2 = packetplayinvehiclemove.getPitch();
                double d7 = d4 - this.lowestRiddenX;
                double d8 = d5 - this.lowestRiddenY;
                double d9 = d6 - this.lowestRiddenZ;
                double d10 = entity.getMotion().lengthSquared();
                double d11 = d7 * d7 + d8 * d8 + d9 * d9;
                this.allowedPlayerTicks += (int) (System.currentTimeMillis() / 50L - this.lastTick);
                this.allowedPlayerTicks = Math.max(this.allowedPlayerTicks, 1);
                this.lastTick = (int) (System.currentTimeMillis() / 50L);
                ++this.movePacketCounter;
                int i = this.movePacketCounter - this.lastMovePacketCounter;
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
                if (this.player.abilities.isFlying) {
                    speed = this.player.abilities.flySpeed * 20.0f;
                } else {
                    speed = this.player.abilities.walkSpeed * 10.0f;
                }
                speed *= 2.0;
                if (d11 - d10 > Math.max(100.0, Math.pow(10.0f * i * speed, 2.0)) && !this.func_217264_d()) {
                    LOGGER.warn("{} (vehicle of {}) moved too quickly! {},{},{}", entity.getName().getString(), this.player.getName().getString(), d7, d8, d9);
                    this.netManager.sendPacket(new SMoveVehiclePacket(entity));
                    return;
                }
                boolean flag = this.bridge$worldNoCollision(worldserver, entity, entity.getBoundingBox().shrink(0.0625));
                d7 = d4 - this.lowestRiddenX1;
                d8 = d5 - this.lowestRiddenY1 - 1.0E-6;
                d9 = d6 - this.lowestRiddenZ1;
                entity.move(MoverType.PLAYER, new Vec3d(d7, d8, d9));
                double d12 = d8;
                d7 = d4 - entity.posX;
                d8 = d5 - entity.posY;
                if (d8 > -0.5 || d8 < 0.5) {
                    d8 = 0.0;
                }
                d9 = d6 - entity.posZ;
                d11 = d7 * d7 + d8 * d8 + d9 * d9;
                boolean flag2 = false;
                if (d11 > 0.0625) {
                    flag2 = true;
                    LOGGER.warn("{} moved wrongly!", entity.getName().getString());
                }
                entity.setPositionAndRotation(d4, d5, d6, f, f2);
                this.player.setPositionAndRotation(d4, d5, d6, this.player.rotationYaw, this.player.rotationPitch);
                boolean flag3 = this.bridge$worldNoCollision(worldserver, entity, entity.getBoundingBox().shrink(0.0625));
                if (flag && (flag2 || !flag3)) {
                    entity.setPositionAndRotation(d0, d2, d3, f, f2);
                    this.player.setPositionAndRotation(d0, d2, d3, this.player.rotationYaw, this.player.rotationPitch);
                    this.netManager.sendPacket(new SMoveVehiclePacket(entity));
                    return;
                }
                Player player = this.getPlayer();
                Location from = new Location(player.getWorld(), this.lastPosX, this.lastPosY, this.lastPosZ, this.lastYaw, this.lastPitch);
                Location to = player.getLocation().clone();
                to.setX(packetplayinvehiclemove.getX());
                to.setY(packetplayinvehiclemove.getY());
                to.setZ(packetplayinvehiclemove.getZ());
                to.setYaw(packetplayinvehiclemove.getYaw());
                to.setPitch(packetplayinvehiclemove.getPitch());
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
                this.player.getServerWorld().getChunkProvider().updatePlayerPosition(this.player);
                this.player.addMovementStat(this.player.posX - d0, this.player.posY - d2, this.player.posZ - d3);
                this.vehicleFloating = (d12 >= -0.03125 && !this.minecraftServer.isFlightAllowed() && !worldserver.checkBlockCollision(entity.getBoundingBox().grow(0.0625).expand(0.0, -0.55, 0.0)));
                this.lowestRiddenX1 = entity.posX;
                this.lowestRiddenY1 = entity.posY;
                this.lowestRiddenZ1 = entity.posZ;
            }
        }
    }

    @Inject(method = "processConfirmTeleport", at = @At(value = "FIELD", ordinal = 6, shift = At.Shift.AFTER, target = "Lnet/minecraft/network/play/ServerPlayNetHandler;targetPos:Lnet/minecraft/util/math/Vec3d;"))
    private void arclight$updateLoc(CConfirmTeleportPacket packetIn, CallbackInfo ci) {
        this.player.getServerWorld().getChunkProvider().updatePlayerPosition(this.player);
    }

    @Inject(method = "processConfirmTeleport", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/network/play/ServerPlayNetHandler;teleportId:I"))
    private void arclight$confirm(CConfirmTeleportPacket packetIn, CallbackInfo ci) {
        if (this.targetPos == null) {
            ci.cancel();
        }
    }

    @Inject(method = "processSelectTrade", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/container/MerchantContainer;setCurrentRecipeIndex(I)V"))
    private void arclight$tradeSelect(CSelectTradePacket packetIn, CallbackInfo ci, int i, Container container) {
        CraftEventFactory.callTradeSelectEvent(this.player, i, (MerchantContainer) container);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void processEditBook(CEditBookPacket packetplayinbedit) {
        PacketThreadUtil.checkThreadAndEnqueue(packetplayinbedit, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        if (lastBookTick == 0) lastBookTick = ArclightConstants.currentTick;
        if (this.lastBookTick + 20 > ArclightConstants.currentTick) {
            this.disconnect("Book edited too quickly!");
            return;
        }
        this.lastBookTick = ArclightConstants.currentTick;
        EquipmentSlotType enumitemslot = (packetplayinbedit.getHand() == Hand.MAIN_HAND) ? EquipmentSlotType.MAINHAND : EquipmentSlotType.OFFHAND;
        ItemStack itemstack = packetplayinbedit.getStack();
        if (!itemstack.isEmpty() && WritableBookItem.isNBTValid(itemstack.getTag())) {
            ItemStack itemstack2 = this.player.getHeldItem(packetplayinbedit.getHand());
            if (itemstack.getItem() == Items.WRITABLE_BOOK && itemstack2.getItem() == Items.WRITABLE_BOOK) {
                if (packetplayinbedit.shouldUpdateAll()) {
                    ItemStack itemstack3 = new ItemStack(Items.WRITTEN_BOOK);
                    CompoundNBT nbttagcompound = itemstack2.getTag();
                    if (nbttagcompound != null) {
                        itemstack3.setTag(nbttagcompound.copy());
                    }
                    itemstack3.setTagInfo("author", this.bridge$stringNbt(this.player.getName().getString()));
                    itemstack3.setTagInfo("title", this.bridge$stringNbt(itemstack.getTag().getString("title")));
                    ListNBT nbttaglist = itemstack.getTag().getList("pages", 8);
                    for (int i = 0; i < nbttaglist.size(); ++i) {
                        String s = nbttaglist.getString(i);
                        StringTextComponent chatcomponenttext = new StringTextComponent(s);
                        s = ITextComponent.Serializer.toJson(chatcomponenttext);
                        nbttaglist.set(i, this.bridge$stringNbt(s));
                    }
                    itemstack3.setTagInfo("pages", nbttaglist);
                    this.player.setHeldItem(packetplayinbedit.getHand(), CraftEventFactory.handleEditBookEvent(this.player, enumitemslot, itemstack2, itemstack3));
                } else {
                    ItemStack old = itemstack2.copy();
                    itemstack2.setTagInfo("pages", itemstack.getTag().getList("pages", 8));
                    CraftEventFactory.handleEditBookEvent(this.player, enumitemslot, old, itemstack2);
                }
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void processPlayer(final CPlayerPacket packetplayinflying) {
        PacketThreadUtil.checkThreadAndEnqueue(packetplayinflying, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        if (isMovePlayerPacketInvalid(packetplayinflying)) {
            this.disconnect(new TranslationTextComponent("multiplayer.disconnect.invalid_player_movement"));
        } else {
            final ServerWorld worldserver = this.minecraftServer.getWorld(this.player.dimension);
            if (!this.player.queuedEndExit && !((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
                if (this.networkTickCount == 0) {
                    this.captureCurrentPosition();
                }
                if (this.targetPos != null) {
                    if (this.networkTickCount - this.lastPositionUpdate > 20) {
                        this.lastPositionUpdate = this.networkTickCount;
                        this.setPlayerLocation(this.targetPos.x, this.targetPos.y, this.targetPos.z, this.player.rotationYaw, this.player.rotationPitch);
                    }
                    this.allowedPlayerTicks = 20;
                } else {
                    this.lastPositionUpdate = this.networkTickCount;
                    if (this.player.isPassenger()) {
                        this.player.setPositionAndRotation(this.player.posX, this.player.posY, this.player.posZ, packetplayinflying.getYaw(this.player.rotationYaw), packetplayinflying.getPitch(this.player.rotationPitch));
                        this.player.getServerWorld().getChunkProvider().updatePlayerPosition(this.player);
                        this.allowedPlayerTicks = 20;
                    } else {
                        final double prevX = this.player.posX;
                        final double prevY = this.player.posY;
                        final double prevZ = this.player.posZ;
                        final float prevYaw = this.player.rotationYaw;
                        final float prevPitch = this.player.rotationPitch;
                        final double d0 = this.player.posX;
                        final double d2 = this.player.posY;
                        final double d3 = this.player.posZ;
                        final double d4 = this.player.posY;
                        final double d5 = packetplayinflying.getX(this.player.posX);
                        final double d6 = packetplayinflying.getY(this.player.posY);
                        final double d7 = packetplayinflying.getZ(this.player.posZ);
                        final float f = packetplayinflying.getYaw(this.player.rotationYaw);
                        final float f2 = packetplayinflying.getPitch(this.player.rotationPitch);
                        double d8 = d5 - this.firstGoodX;
                        double d9 = d6 - this.firstGoodY;
                        double d10 = d7 - this.firstGoodZ;
                        final double d11 = this.player.getMotion().lengthSquared();
                        double d12 = d8 * d8 + d9 * d9 + d10 * d10;
                        if (this.player.isSleeping()) {
                            if (d12 > 1.0) {
                                this.setPlayerLocation(this.player.posX, this.player.posY, this.player.posZ, packetplayinflying.getYaw(this.player.rotationYaw), packetplayinflying.getPitch(this.player.rotationPitch));
                            }
                        } else {
                            ++this.movePacketCounter;
                            int i = this.movePacketCounter - this.lastMovePacketCounter;
                            this.allowedPlayerTicks += (int) (System.currentTimeMillis() / 50L - this.lastTick);
                            this.allowedPlayerTicks = Math.max(this.allowedPlayerTicks, 1);
                            this.lastTick = (int) (System.currentTimeMillis() / 50L);
                            if (i > Math.max(this.allowedPlayerTicks, 5)) {
                                LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)", this.player.getName().getString(), i);
                                i = 1;
                            }
                            if (packetplayinflying.rotating || d12 > 0.0) {
                                --this.allowedPlayerTicks;
                            } else {
                                this.allowedPlayerTicks = 20;
                            }
                            double speed;
                            if (this.player.abilities.isFlying) {
                                speed = this.player.abilities.flySpeed * 20.0f;
                            } else {
                                speed = this.player.abilities.walkSpeed * 10.0f;
                            }
                            if (!this.player.isInvulnerableDimensionChange() && (!this.player.getServerWorld().getGameRules().getBoolean(GameRules.DISABLE_ELYTRA_MOVEMENT_CHECK) || !this.player.isElytraFlying())) {
                                final float f3 = this.player.isElytraFlying() ? 300.0f : 100.0f;
                                if (d12 - d11 > Math.max(f3, Math.pow(10.0f * i * speed, 2.0)) && !this.func_217264_d()) {
                                    LOGGER.warn("{} moved too quickly! {},{},{}", this.player.getName().getString(), d8, d9, d10);
                                    this.setPlayerLocation(this.player.posX, this.player.posY, this.player.posZ, this.player.rotationYaw, this.player.rotationPitch);
                                    return;
                                }
                            }
                            final boolean flag = this.isPlayerNotInBlock(worldserver);
                            d8 = d5 - this.lastGoodX;
                            d9 = d6 - this.lastGoodY;
                            d10 = d7 - this.lastGoodZ;

                            if (d9 > 0.0D) {
                                this.player.fallDistance = 0.0F;
                            }

                            if (this.player.onGround && !packetplayinflying.isOnGround() && d9 > 0.0) {
                                this.player.jump();
                            }
                            this.player.move(MoverType.PLAYER, new Vec3d(d8, d9, d10));
                            this.player.onGround = packetplayinflying.isOnGround();
                            final double d13 = d9;
                            d8 = d5 - this.player.posX;
                            d9 = d6 - this.player.posY;
                            if (d9 > -0.5 || d9 < 0.5) {
                                d9 = 0.0;
                            }
                            d10 = d7 - this.player.posZ;
                            d12 = d8 * d8 + d9 * d9 + d10 * d10;
                            boolean flag2 = false;
                            if (!this.player.isInvulnerableDimensionChange() && d12 > 0.0625 && !this.player.isSleeping() && !this.player.interactionManager.isCreative() && this.player.interactionManager.getGameType() != GameType.SPECTATOR) {
                                flag2 = true;
                                LOGGER.warn("{} moved wrongly!", this.player.getName().getString());
                            }
                            this.player.setPositionAndRotation(d5, d6, d7, f, f2);
                            this.player.addMovementStat(this.player.posX - d0, this.player.posY - d2, this.player.posZ - d3);
                            if (!this.player.noClip && !this.player.isSleeping()) {
                                final boolean flag3 = this.isPlayerNotInBlock(worldserver);
                                if (flag && (flag2 || !flag3)) {
                                    this.setPlayerLocation(d0, d2, d3, f, f2);
                                    return;
                                }
                            }
                            this.player.setPositionAndRotation(prevX, prevY, prevZ, prevYaw, prevPitch);
                            final Player player = this.getPlayer();
                            final Location from = new Location(player.getWorld(), this.lastPosX, this.lastPosY, this.lastPosZ, this.lastYaw, this.lastPitch);
                            final Location to = player.getLocation().clone();
                            if (packetplayinflying.moving) {
                                to.setX(packetplayinflying.x);
                                to.setY(packetplayinflying.y);
                                to.setZ(packetplayinflying.z);
                            }
                            if (packetplayinflying.rotating) {
                                to.setYaw(packetplayinflying.yaw);
                                to.setPitch(packetplayinflying.pitch);
                            }
                            final double delta = Math.pow(this.lastPosX - to.getX(), 2.0) + Math.pow(this.lastPosY - to.getY(), 2.0) + Math.pow(this.lastPosZ - to.getZ(), 2.0);
                            final float deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());
                            if ((delta > 0.00390625 || deltaAngle > 10.0f) && !((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
                                this.lastPosX = to.getX();
                                this.lastPosY = to.getY();
                                this.lastPosZ = to.getZ();
                                this.lastYaw = to.getYaw();
                                this.lastPitch = to.getPitch();
                                if (from.getX() != Double.MAX_VALUE) {
                                    final Location oldTo = to.clone();
                                    final PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
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
                            this.player.setPositionAndRotation(d5, d6, d7, f, f2);

                            // this.floating = (d13 >= -0.03125 && this.player.interactionManager.getGameType() != GameType.SPECTATOR && !this.minecraftServer.isFlightAllowed() && !this.player.abilities.allowFlying && !this.player.isPotionActive(Effects.LEVITATION) && !this.player.isElytraFlying() && !worldserver.checkBlockCollision(this.player.getBoundingBox().grow(0.0625).expand(0.0, -0.55, 0.0)));
                            this.floating = (d13 >= -0.03125 && this.player.interactionManager.getGameType() != GameType.SPECTATOR && !this.minecraftServer.isFlightAllowed() && !this.player.abilities.allowFlying && !this.player.isPotionActive(Effects.LEVITATION) && !this.player.isElytraFlying() && !worldserver.checkBlockCollision(this.player.getBoundingBox().grow(0.0625).expand(0.0, -0.55, 0.0)) && !this.player.isSpinAttacking());

                            this.player.onGround = packetplayinflying.isOnGround();
                            this.player.getServerWorld().getChunkProvider().updatePlayerPosition(this.player);
                            this.player.handleFalling(this.player.posY - d4, packetplayinflying.isOnGround());
                            this.lastGoodX = this.player.posX;
                            this.lastGoodY = this.player.posY;
                            this.lastGoodZ = this.player.posZ;
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "onDisconnect", cancellable = true, at = @At("HEAD"))
    private void arclight$returnIfProcessed(ITextComponent reason, CallbackInfo ci) {
        if (processedDisconnect) {
            ci.cancel();
        } else {
            processedDisconnect = true;
        }
    }

    @Redirect(method = "onDisconnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/PlayerList;sendMessage(Lnet/minecraft/util/text/ITextComponent;)V"))
    public void arclight$captureQuit(PlayerList playerList, ITextComponent component) {
        // do nothing
    }

    @Inject(method = "onDisconnect", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/management/PlayerList;playerLoggedOut(Lnet/minecraft/entity/player/ServerPlayerEntity;)V"))
    public void arclight$processQuit(ITextComponent reason, CallbackInfo ci) {
        String quitMessage = ArclightCaptures.getQuitMessage();
        if (quitMessage != null) {
            for (ITextComponent component : CraftChatMessage.fromString(quitMessage)) {
                this.minecraftServer.getPlayerList().sendMessage(component, true);
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void processPlayerDigging(CPlayerDiggingPacket packetplayinblockdig) {
        PacketThreadUtil.checkThreadAndEnqueue(packetplayinblockdig, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        BlockPos blockposition = packetplayinblockdig.getPosition();
        this.player.markPlayerActive();
        CPlayerDiggingPacket.Action packetplayinblockdig_enumplayerdigtype = packetplayinblockdig.getAction();
        switch (packetplayinblockdig_enumplayerdigtype) {
            case SWAP_HELD_ITEMS: {
                if (!this.player.isSpectator()) {
                    ItemStack itemstack = this.player.getHeldItem(Hand.OFF_HAND);
                    CraftItemStack mainHand = CraftItemStack.asCraftMirror(itemstack);
                    CraftItemStack offHand = CraftItemStack.asCraftMirror(this.player.getHeldItem(Hand.MAIN_HAND));
                    PlayerSwapHandItemsEvent swapItemsEvent = new PlayerSwapHandItemsEvent(this.getPlayer(), mainHand.clone(), offHand.clone());
                    this.server.getPluginManager().callEvent(swapItemsEvent);
                    if (swapItemsEvent.isCancelled()) {
                        return;
                    }
                    if (swapItemsEvent.getOffHandItem().equals(offHand)) {
                        this.player.setHeldItem(Hand.OFF_HAND, this.player.getHeldItem(Hand.MAIN_HAND));
                    } else {
                        this.player.setHeldItem(Hand.OFF_HAND, CraftItemStack.asNMSCopy(swapItemsEvent.getOffHandItem()));
                    }
                    if (swapItemsEvent.getMainHandItem().equals(mainHand)) {
                        this.player.setHeldItem(Hand.MAIN_HAND, itemstack);
                    } else {
                        this.player.setHeldItem(Hand.MAIN_HAND, CraftItemStack.asNMSCopy(swapItemsEvent.getMainHandItem()));
                    }
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
                    this.bridge$dropItems(this.player, false);
                }
                return;
            }
            case DROP_ALL_ITEMS: {
                if (!this.player.isSpectator()) {
                    this.bridge$dropItems(this.player, true);
                }
                return;
            }
            case RELEASE_USE_ITEM: {
                this.player.stopActiveHand();
                return;
            }
            case START_DESTROY_BLOCK:
            case ABORT_DESTROY_BLOCK:
            case STOP_DESTROY_BLOCK: {
                this.player.interactionManager.func_225416_a(blockposition, packetplayinblockdig_enumplayerdigtype, packetplayinblockdig.getFacing(), this.minecraftServer.getBuildLimit());
                return;
            }
            default: {
                throw new IllegalArgumentException("Invalid player action");
            }
        }
    }

    @Inject(method = "processTryUseItemOnBlock", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/world/dimension/DimensionType;)Lnet/minecraft/world/server/ServerWorld;"))
    private void arclight$frozenUseItem(CPlayerTryUseItemOnBlockPacket packetIn, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
        if (!this.checkLimit(((TimestampedPacket) packetIn).bridge$timestamp())) {
            ci.cancel();
        }
    }

    @Inject(method = "processTryUseItemOnBlock", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/PlayerInteractionManager;func_219441_a(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;Lnet/minecraft/util/math/BlockRayTraceResult;)Lnet/minecraft/util/ActionResultType;"))
    private void arclight$checkDistance(CPlayerTryUseItemOnBlockPacket packetIn, CallbackInfo ci, ServerWorld serverWorld, Hand hand, ItemStack itemStack, BlockRayTraceResult rayTraceResult, BlockPos blockPos) {
        this.player.stopActiveHand();
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
    public void processTryUseItem(CPlayerTryUseItemPacket packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        if (!this.checkLimit(((TimestampedPacket) packet).bridge$timestamp())) {
            return;
        }
        ServerWorld worldserver = this.minecraftServer.getWorld(this.player.dimension);
        Hand enumhand = packet.getHand();
        ItemStack itemstack = this.player.getHeldItem(enumhand);
        this.player.markPlayerActive();
        if (!itemstack.isEmpty()) {
            float f1 = this.player.rotationPitch;
            float f2 = this.player.rotationYaw;
            double d0 = this.player.posX;
            double d2 = this.player.posY + this.player.getEyeHeight();
            double d3 = this.player.posZ;
            Vec3d vec3d = new Vec3d(d0, d2, d3);
            float f3 = MathHelper.cos(-f2 * 0.017453292f - 3.1415927f);
            float f4 = MathHelper.sin(-f2 * 0.017453292f - 3.1415927f);
            float f5 = -MathHelper.cos(-f1 * 0.017453292f);
            float f6 = MathHelper.sin(-f1 * 0.017453292f);
            float f7 = f4 * f5;
            float f8 = f3 * f5;
            double d4 = (this.player.interactionManager.getGameType() == GameType.CREATIVE) ? 5.0 : 4.5;
            Vec3d vec3d2 = vec3d.add(f7 * d4, f6 * d4, f8 * d4);
            RayTraceResult movingobjectposition = this.player.world.rayTraceBlocks(new RayTraceContext(vec3d, vec3d2, RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, this.player));
            boolean cancelled;
            if (movingobjectposition == null || movingobjectposition.getType() != RayTraceResult.Type.BLOCK) {
                PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.RIGHT_CLICK_AIR, itemstack, enumhand);
                cancelled = (event.useItemInHand() == Event.Result.DENY);
            } else if (((PlayerInteractionManagerBridge) this.player.interactionManager).bridge$isFiredInteract()) {
                ((PlayerInteractionManagerBridge) this.player.interactionManager).bridge$setFiredInteract(false);
                cancelled = ((PlayerInteractionManagerBridge) this.player.interactionManager).bridge$getInteractResult();
            } else {
                BlockRayTraceResult movingobjectpositionblock = (BlockRayTraceResult) movingobjectposition;
                PlayerInteractEvent event2 = CraftEventFactory.callPlayerInteractEvent(this.player, Action.RIGHT_CLICK_BLOCK, movingobjectpositionblock.getPos(), movingobjectpositionblock.getFace(), itemstack, true, enumhand);
                cancelled = (event2.useItemInHand() == Event.Result.DENY);
            }
            if (cancelled) {
                ((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity().updateInventory();
            } else {
                this.player.interactionManager.processRightClick(this.player, worldserver, itemstack, enumhand);
            }
        }
    }

    @Inject(method = "handleSpectate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;teleport(Lnet/minecraft/world/server/ServerWorld;DDDFF)V"))
    private void arclight$spectateTeleport(CSpectatePacket packetIn, CallbackInfo ci) {
        ((ServerPlayerEntityBridge) this.player).bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause.SPECTATE);
    }

    @Inject(method = "handleResourcePackStatus", at = @At("HEAD"))
    private void arclight$handleResourcePackStatus(CResourcePackStatusPacket packetIn, CallbackInfo ci) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        this.server.getPluginManager().callEvent(new PlayerResourcePackStatusEvent(this.getPlayer(), PlayerResourcePackStatusEvent.Status.values()[packetIn.action.ordinal()]));
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/IPacket;Lio/netty/util/concurrent/GenericFutureListener;)V", cancellable = true, at = @At("HEAD"))
    private void arclight$updateCompassTarget(IPacket<?> packetIn, GenericFutureListener<? extends Future<? super Void>> futureListeners, CallbackInfo ci) {
        if (!(packetIn instanceof SChatPacket)) {
            if (packetIn == null || processedDisconnect) {
                ci.cancel();
                return;
            }
            if (packetIn instanceof SSpawnPositionPacket) {
                SSpawnPositionPacket packet2 = (SSpawnPositionPacket) packetIn;
                ((ServerPlayerEntityBridge) this.player).bridge$setCompassTarget(new Location(this.getPlayer().getWorld(), packet2.spawnBlockPos.getX(), packet2.spawnBlockPos.getY(), packet2.spawnBlockPos.getZ()));
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void processHeldItemChange(CHeldItemChangePacket packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        if (packet.getSlotId() >= 0 && packet.getSlotId() < PlayerInventory.getHotbarSize()) {
            PlayerItemHeldEvent event = new PlayerItemHeldEvent(this.getPlayer(), this.player.inventory.currentItem, packet.getSlotId());
            this.server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                this.sendPacket(new SHeldItemChangePacket(this.player.inventory.currentItem));
                this.player.markPlayerActive();
                return;
            }
            this.player.inventory.currentItem = packet.getSlotId();
            this.player.markPlayerActive();
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
    public void processChatMessage(CChatMessagePacket packet) {
        if (this.minecraftServer.isServerStopped()) {
            return;
        }
        boolean isSync = packet.getMessage().startsWith("/");
        if (packet.getMessage().startsWith("/")) {
            PacketThreadUtil.checkThreadAndEnqueue(packet, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        }
        if (this.player.removed || this.player.getChatVisibility() == ChatVisibility.HIDDEN) {
            this.sendPacket(new SChatPacket(new TranslationTextComponent("chat.cannotSend").applyTextStyle(TextFormatting.RED)));
        } else {
            this.player.markPlayerActive();
            String s = packet.getMessage();
            s = org.apache.commons.lang3.StringUtils.normalizeSpace(s);
            for (int i = 0; i < s.length(); ++i) {
                if (!SharedConstants.isAllowedCharacter(s.charAt(i))) {
                    if (!isSync) {
                        class Disconnect extends Waitable {

                            @Override
                            protected Object evaluate() {
                                disconnect(new TranslationTextComponent("multiplayer.disconnect.illegal_characters"));
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
                    this.disconnect(new TranslationTextComponent("multiplayer.disconnect.illegal_characters"));
                    return;
                }
            }
            if (isSync) {
                try {
                    this.server.playerCommandState = true;
                    this.handleSlashCommand(s);
                } finally {
                    this.server.playerCommandState = false;
                }
                this.server.playerCommandState = false;
            } else if (s.isEmpty()) {
                LOGGER.warn(this.player.getScoreboardName() + " tried to send an empty message");
            } else if (this.getPlayer().isConversing()) {
                String conversationInput = s;
                ((MinecraftServerBridge) this.minecraftServer).bridge$queuedProcess(() -> this.getPlayer().acceptConversationInput(conversationInput));
            } else if (this.player.getChatVisibility() == ChatVisibility.SYSTEM) {
                TranslationTextComponent chatmessage = new TranslationTextComponent("chat.cannotSend");
                chatmessage.getStyle().setColor(TextFormatting.RED);
                this.sendPacket(new SChatPacket(chatmessage));
            } else {
                this.chat(s, true);
            }
            this.chatSpamThresholdCount += 20;
            if (this.chatSpamThresholdCount > 200 && !this.minecraftServer.getPlayerList().canSendCommands(this.player.getGameProfile())) {
                if (!isSync) {
                    class Disconnect2 extends Waitable {

                        @Override
                        protected Object evaluate() {
                            disconnect(new TranslationTextComponent("disconnect.spam"));
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
                this.disconnect(new TranslationTextComponent("disconnect.spam"));
            }
        }
    }

    public void chat(String s, boolean async) {
        if (s.isEmpty() || this.player.getChatVisibility() == ChatVisibility.HIDDEN) {
            return;
        }
        ServerPlayNetHandler handler = (ServerPlayNetHandler) (Object) this;
        if (!async && s.startsWith("/")) {
            this.handleSlashCommand(s);
        } else if (this.player.getChatVisibility() != ChatVisibility.SYSTEM) {
            Player player = this.getPlayer();
            AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(async, player, s, new LazyPlayerSet(this.minecraftServer));
            this.server.getPluginManager().callEvent(event);
            if (PlayerChatEvent.getHandlerList().getRegisteredListeners().length != 0) {
                PlayerChatEvent queueEvent = new PlayerChatEvent(player, event.getMessage(), event.getFormat(), event.getRecipients());
                queueEvent.setCancelled(event.isCancelled());
                class SyncChat extends Waitable {

                    @Override
                    protected Object evaluate() {
                        Bukkit.getPluginManager().callEvent(queueEvent);
                        if (queueEvent.isCancelled()) {
                            return null;
                        }
                        String message = String.format(queueEvent.getFormat(), queueEvent.getPlayer().getDisplayName(), queueEvent.getMessage());
                        ITextComponent component = ForgeHooks.onServerChatEvent(handler, queueEvent.getMessage(), ForgeHooks.newChatWithLinks(message));
                        if (component == null) return null;
                        Bukkit.getConsoleSender().sendMessage(CraftChatMessage.fromComponent(component));
                        if (((LazyPlayerSet) queueEvent.getRecipients()).isLazy()) {
                            for (ServerPlayerEntity player : minecraftServer.getPlayerList().players) {
                                ((ServerPlayerEntityBridge) player).bridge$sendMessage(component);
                            }
                        } else {
                            for (Player player2 : queueEvent.getRecipients()) {
                                ((ServerPlayerEntityBridge) ((CraftPlayer) player2).getHandle()).bridge$sendMessage(component);
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
            ITextComponent chatWithLinks = ForgeHooks.newChatWithLinks(s);
            class ForgeChat extends Waitable<Void> {

                @Override
                protected Void evaluate() {
                    // this is called on main thread
                    ITextComponent component = ForgeHooks.onServerChatEvent(handler, event.getMessage(), chatWithLinks);
                    if (component == null) return null;
                    Bukkit.getConsoleSender().sendMessage(CraftChatMessage.fromComponent(component));
                    if (((LazyPlayerSet) event.getRecipients()).isLazy()) {
                        for (ServerPlayerEntity recipient : minecraftServer.getPlayerList().players) {
                            ((ServerPlayerEntityBridge) recipient).bridge$sendMessage(component);
                        }
                    } else {
                        for (Player recipient2 : event.getRecipients()) {
                            ((ServerPlayerEntityBridge) ((CraftPlayer) recipient2).getHandle()).bridge$sendMessage(component);
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
    private void handleSlashCommand(String s) {
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
            minecraftServer.getCommandManager().handleCommand(((CraftPlayer) event.getPlayer()).getHandle().getCommandSource(), event.getMessage());
        } catch (CommandException ex) {
            player.sendMessage(ChatColor.RED + "An internal error occurred while attempting to perform this command");
            java.util.logging.Logger.getLogger(ServerPlayNetHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleAnimation(CAnimateHandPacket packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        this.player.markPlayerActive();
        float f1 = this.player.rotationPitch;
        float f2 = this.player.rotationYaw;
        double d0 = this.player.posX;
        double d2 = this.player.posY + this.player.getEyeHeight();
        double d3 = this.player.posZ;
        Vec3d vec3d = new Vec3d(d0, d2, d3);
        float f3 = MathHelper.cos(-f2 * 0.017453292f - 3.1415927f);
        float f4 = MathHelper.sin(-f2 * 0.017453292f - 3.1415927f);
        float f5 = -MathHelper.cos(-f1 * 0.017453292f);
        float f6 = MathHelper.sin(-f1 * 0.017453292f);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d4 = (this.player.interactionManager.getGameType() == GameType.CREATIVE) ? 5.0 : 4.5;
        Vec3d vec3d2 = vec3d.add(f7 * d4, f6 * d4, f8 * d4);
        RayTraceResult result = this.player.world.rayTraceBlocks(new RayTraceContext(vec3d, vec3d2, RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, this.player));
        if (result == null || result.getType() != RayTraceResult.Type.BLOCK) {
            CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_AIR, this.player.inventory.getCurrentItem(), Hand.MAIN_HAND);
        }
        PlayerAnimationEvent event = new PlayerAnimationEvent(this.getPlayer());
        this.server.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        this.player.swingArm(packet.getHand());
    }

    private static final Set<String> sneakKeys = ImmutableSet.of("START_SNEAKING", "PRESS_SHIFT_KEY");
    private static final Set<String> standKeys = ImmutableSet.of("STOP_SNEAKING", "RELEASE_SHIFT_KEY");

    @Inject(method = "processEntityAction", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;markPlayerActive()V"))
    private void arclight$toggleAction(CEntityActionPacket packetIn, CallbackInfo ci) {
        if (this.player.removed) {
            ci.cancel();
            return;
        }
        String name = packetIn.getAction().name();
        if (sneakKeys.contains(name) || standKeys.contains(name)) {
            PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this.getPlayer(), sneakKeys.contains(name));
            this.server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        } else if (packetIn.getAction() == CEntityActionPacket.Action.START_SPRINTING || packetIn.getAction() == CEntityActionPacket.Action.STOP_SPRINTING) {
            PlayerToggleSprintEvent e2 = new PlayerToggleSprintEvent(this.getPlayer(), packetIn.getAction() == CEntityActionPacket.Action.START_SPRINTING);
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
    public void processUseEntity(final CUseEntityPacket packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        final ServerWorld world = this.minecraftServer.getWorld(this.player.dimension);
        final Entity entity = packetIn.getEntityFromWorld(world);
        this.player.markPlayerActive();
        if (entity != null) {
            final boolean flag = this.player.canEntityBeSeen(entity);
            double d0 = 36.0;
            if (!flag) {
                d0 = 9.0;
            }
            if (this.player.getDistanceSq(entity) < d0) {
                final ItemStack itemInHand = this.player.getHeldItem((packetIn.getHand() == null) ? Hand.MAIN_HAND : packetIn.getHand());
                if (packetIn.getAction() == CUseEntityPacket.Action.INTERACT || packetIn.getAction() == CUseEntityPacket.Action.INTERACT_AT) {
                    final boolean triggerLeashUpdate = itemInHand != null && itemInHand.getItem() == Items.LEAD && entity instanceof MobEntity;
                    final Item origItem = (this.player.inventory.getCurrentItem() == null) ? null : this.player.inventory.getCurrentItem().getItem();
                    PlayerInteractEntityEvent event;
                    if (packetIn.getAction() == CUseEntityPacket.Action.INTERACT) {
                        event = new PlayerInteractEntityEvent(this.getPlayer(), ((EntityBridge) entity).bridge$getBukkitEntity(), (packetIn.getHand() == Hand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
                    } else {
                        final Vec3d target = packetIn.getHitVec();
                        event = new PlayerInteractAtEntityEvent(this.getPlayer(), ((EntityBridge) entity).bridge$getBukkitEntity(), new Vector(target.x, target.y, target.z), (packetIn.getHand() == Hand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
                    }
                    this.server.getPluginManager().callEvent(event);
                    if (entity instanceof AbstractFishEntity && origItem != null && origItem.asItem() == Items.WATER_BUCKET && (event.isCancelled() || this.player.inventory.getCurrentItem() == null || this.player.inventory.getCurrentItem().getItem() != origItem)) {
                        this.sendPacket(new SSpawnMobPacket((LivingEntity) entity));
                        this.player.sendContainerToPlayer(this.player.openContainer);
                    }
                    if (triggerLeashUpdate && (event.isCancelled() || this.player.inventory.getCurrentItem() == null || this.player.inventory.getCurrentItem().getItem() != origItem)) {
                        this.sendPacket(new SMountEntityPacket(entity, ((MobEntity) entity).getLeashHolder()));
                    }
                    if (event.isCancelled() || this.player.inventory.getCurrentItem() == null || this.player.inventory.getCurrentItem().getItem() != origItem) {
                        this.sendPacket(new SEntityMetadataPacket(entity.getEntityId(), entity.getDataManager(), true));
                    }
                    if (event.isCancelled()) {
                        return;
                    }
                }
                if (packetIn.getAction() == CUseEntityPacket.Action.INTERACT) {
                    final Hand hand = packetIn.getHand();
                    this.player.interactOn(entity, hand);
                    if (!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
                        this.player.sendContainerToPlayer(this.player.openContainer);
                    }
                } else if (packetIn.getAction() == CUseEntityPacket.Action.INTERACT_AT) {
                    final Hand hand = packetIn.getHand();
                    if (net.minecraftforge.common.ForgeHooks.onInteractEntityAt(player, entity, packetIn.getHitVec(), hand) != null)
                        return;
                    ActionResultType result = entity.applyPlayerInteraction(this.player, packetIn.getHitVec(), hand);
                    if (result.isSuccess()) {
                        this.player.swingArm(hand);
                    }
                    if (!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
                        this.player.sendContainerToPlayer(this.player.openContainer);
                    }
                } else if (packetIn.getAction() == CUseEntityPacket.Action.ATTACK) {
                    if (entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof AbstractArrowEntity || (entity == this.player && !this.player.isSpectator())) {
                        this.disconnect(new TranslationTextComponent("multiplayer.disconnect.invalid_entity_attacked"));
                        this.minecraftServer.logWarning("Player " + this.player.getName().getString() + " tried to attack an invalid entity");
                        return;
                    }
                    this.player.attackTargetEntityWithCurrentItem(entity);
                    if (!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
                        this.player.sendContainerToPlayer(this.player.openContainer);
                    }
                }
            }
        }
    }

    @Inject(method = "processCloseWindow", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;closeContainer()V"))
    private void arclight$invClose(CCloseWindowPacket packetIn, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            ci.cancel();
            return;
        }
        CraftEventFactory.handleInventoryCloseEvent(this.player);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void processClickWindow(CClickWindowPacket packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        if (((ServerPlayerEntityBridge) this.player).bridge$isMovementBlocked()) {
            return;
        }
        this.player.markPlayerActive();
        if (this.player.openContainer.windowId == packet.getWindowId() && this.player.openContainer.getCanCraft(this.player) && this.player.openContainer.canInteractWith(this.player)) {
            boolean cancelled = this.player.isSpectator();
            if (packet.getSlotId() < -1 && packet.getSlotId() != -999) {
                return;
            }
            InventoryView inventory = ((ContainerBridge) this.player.openContainer).bridge$getBukkitView();
            InventoryType.SlotType type = inventory.getSlotType(packet.getSlotId());
            org.bukkit.event.inventory.ClickType click = org.bukkit.event.inventory.ClickType.UNKNOWN;
            InventoryAction action = InventoryAction.UNKNOWN;
            ItemStack itemstack = ItemStack.EMPTY;
            switch (packet.getClickType()) {
                case PICKUP: {
                    if (packet.getUsedButton() == 0) {
                        click = org.bukkit.event.inventory.ClickType.LEFT;
                    } else if (packet.getUsedButton() == 1) {
                        click = org.bukkit.event.inventory.ClickType.RIGHT;
                    }
                    if (packet.getUsedButton() != 0 && packet.getUsedButton() != 1) {
                        break;
                    }
                    action = InventoryAction.NOTHING;
                    if (packet.getSlotId() == -999) {
                        if (!this.player.inventory.getItemStack().isEmpty()) {
                            action = ((packet.getUsedButton() == 0) ? InventoryAction.DROP_ALL_CURSOR : InventoryAction.DROP_ONE_CURSOR);
                            break;
                        }
                        break;
                    } else {
                        if (packet.getSlotId() < 0) {
                            action = InventoryAction.NOTHING;
                            break;
                        }
                        Slot slot = this.player.openContainer.getSlot(packet.getSlotId());
                        if (slot == null) {
                            break;
                        }
                        ItemStack clickedItem = slot.getStack();
                        ItemStack cursor = this.player.inventory.getItemStack();
                        if (clickedItem.isEmpty()) {
                            if (!cursor.isEmpty()) {
                                action = ((packet.getUsedButton() == 0) ? InventoryAction.PLACE_ALL : InventoryAction.PLACE_ONE);
                                break;
                            }
                            break;
                        } else {
                            if (!slot.canTakeStack(this.player)) {
                                break;
                            }
                            if (cursor.isEmpty()) {
                                action = ((packet.getUsedButton() == 0) ? InventoryAction.PICKUP_ALL : InventoryAction.PICKUP_HALF);
                                break;
                            }
                            if (slot.isItemValid(cursor)) {
                                if (clickedItem.isItemEqual(cursor) && ItemStack.areItemStackTagsEqual(clickedItem, cursor)) {
                                    int toPlace = (packet.getUsedButton() == 0) ? cursor.getCount() : 1;
                                    toPlace = Math.min(toPlace, clickedItem.getMaxStackSize() - clickedItem.getCount());
                                    toPlace = Math.min(toPlace, slot.inventory.getInventoryStackLimit() - clickedItem.getCount());
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
                                    if (cursor.getCount() <= slot.getSlotStackLimit()) {
                                        action = InventoryAction.SWAP_WITH_CURSOR;
                                        break;
                                    }
                                    break;
                                }
                            } else {
                                if (cursor.getItem() == clickedItem.getItem() && ItemStack.areItemStackTagsEqual(cursor, clickedItem) && clickedItem.getCount() >= 0 && clickedItem.getCount() + cursor.getCount() <= cursor.getMaxStackSize()) {
                                    action = InventoryAction.PICKUP_ALL;
                                    break;
                                }
                                break;
                            }
                        }
                    }
                }
                case QUICK_MOVE: {
                    if (packet.getUsedButton() == 0) {
                        click = org.bukkit.event.inventory.ClickType.SHIFT_LEFT;
                    } else if (packet.getUsedButton() == 1) {
                        click = org.bukkit.event.inventory.ClickType.SHIFT_RIGHT;
                    }
                    if (packet.getUsedButton() != 0 && packet.getUsedButton() != 1) {
                        break;
                    }
                    if (packet.getSlotId() < 0) {
                        action = InventoryAction.NOTHING;
                        break;
                    }
                    Slot slot = this.player.openContainer.getSlot(packet.getSlotId());
                    if (slot != null && slot.canTakeStack(this.player) && slot.getHasStack()) {
                        action = InventoryAction.MOVE_TO_OTHER_INVENTORY;
                        break;
                    }
                    action = InventoryAction.NOTHING;
                    break;
                }
                case SWAP: {
                    if (packet.getUsedButton() < 0 || packet.getUsedButton() >= 9) {
                        break;
                    }
                    click = org.bukkit.event.inventory.ClickType.NUMBER_KEY;
                    Slot clickedSlot = this.player.openContainer.getSlot(packet.getSlotId());
                    if (!clickedSlot.canTakeStack(this.player)) {
                        action = InventoryAction.NOTHING;
                        break;
                    }
                    ItemStack hotbar = this.player.inventory.getStackInSlot(packet.getUsedButton());
                    boolean canCleanSwap = hotbar.isEmpty() || (clickedSlot.inventory == this.player.inventory && clickedSlot.isItemValid(hotbar));
                    if (clickedSlot.getHasStack()) {
                        if (canCleanSwap) {
                            action = InventoryAction.HOTBAR_SWAP;
                            break;
                        }
                        action = InventoryAction.HOTBAR_MOVE_AND_READD;
                        break;
                    } else {
                        if (!clickedSlot.getHasStack() && !hotbar.isEmpty() && clickedSlot.isItemValid(hotbar)) {
                            action = InventoryAction.HOTBAR_SWAP;
                            break;
                        }
                        action = InventoryAction.NOTHING;
                        break;
                    }
                }
                case CLONE: {
                    if (packet.getUsedButton() != 2) {
                        click = org.bukkit.event.inventory.ClickType.UNKNOWN;
                        action = InventoryAction.UNKNOWN;
                        break;
                    }
                    click = org.bukkit.event.inventory.ClickType.MIDDLE;
                    if (packet.getSlotId() < 0) {
                        action = InventoryAction.NOTHING;
                        break;
                    }
                    Slot slot = this.player.openContainer.getSlot(packet.getSlotId());
                    if (slot != null && slot.getHasStack() && this.player.abilities.isCreativeMode && this.player.inventory.getItemStack().isEmpty()) {
                        action = InventoryAction.CLONE_STACK;
                        break;
                    }
                    action = InventoryAction.NOTHING;
                    break;
                }
                case THROW: {
                    if (packet.getSlotId() < 0) {
                        click = org.bukkit.event.inventory.ClickType.LEFT;
                        if (packet.getUsedButton() == 1) {
                            click = org.bukkit.event.inventory.ClickType.RIGHT;
                        }
                        action = InventoryAction.NOTHING;
                        break;
                    }
                    if (packet.getUsedButton() == 0) {
                        click = org.bukkit.event.inventory.ClickType.DROP;
                        Slot slot = this.player.openContainer.getSlot(packet.getSlotId());
                        if (slot != null && slot.getHasStack() && slot.canTakeStack(this.player) && !slot.getStack().isEmpty() && slot.getStack().getItem() != Item.getItemFromBlock(Blocks.AIR)) {
                            action = InventoryAction.DROP_ONE_SLOT;
                            break;
                        }
                        action = InventoryAction.NOTHING;
                        break;
                    } else {
                        if (packet.getUsedButton() != 1) {
                            break;
                        }
                        click = org.bukkit.event.inventory.ClickType.CONTROL_DROP;
                        Slot slot = this.player.openContainer.getSlot(packet.getSlotId());
                        if (slot != null && slot.getHasStack() && slot.canTakeStack(this.player) && !slot.getStack().isEmpty() && slot.getStack().getItem() != Item.getItemFromBlock(Blocks.AIR)) {
                            action = InventoryAction.DROP_ALL_SLOT;
                            break;
                        }
                        action = InventoryAction.NOTHING;
                        break;
                    }
                }
                case QUICK_CRAFT: {
                    itemstack = this.player.openContainer.slotClick(packet.getSlotId(), packet.getUsedButton(), packet.getClickType(), this.player);
                    break;
                }
                case PICKUP_ALL: {
                    click = org.bukkit.event.inventory.ClickType.DOUBLE_CLICK;
                    action = InventoryAction.NOTHING;
                    if (packet.getSlotId() < 0 || this.player.inventory.getItemStack().isEmpty()) {
                        break;
                    }
                    ItemStack cursor2 = this.player.inventory.getItemStack();
                    action = InventoryAction.NOTHING;
                    if (inventory.getTopInventory().contains(CraftMagicNumbers.getMaterial(cursor2.getItem())) || inventory.getBottomInventory().contains(CraftMagicNumbers.getMaterial(cursor2.getItem()))) {
                        action = InventoryAction.COLLECT_TO_CURSOR;
                        break;
                    }
                    break;
                }
            }
            if (packet.getClickType() != net.minecraft.inventory.container.ClickType.QUICK_CRAFT) {
                InventoryClickEvent event;
                if (click == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
                    event = new InventoryClickEvent(inventory, type, packet.getSlotId(), click, action, packet.getUsedButton());
                } else {
                    event = new InventoryClickEvent(inventory, type, packet.getSlotId(), click, action);
                }
                Inventory top = inventory.getTopInventory();
                if (packet.getSlotId() == 0 && top instanceof org.bukkit.inventory.CraftingInventory) {
                    Recipe recipe = ((org.bukkit.inventory.CraftingInventory) top).getRecipe();
                    if (recipe != null) {
                        if (click == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
                            event = new CraftItemEvent(recipe, inventory, type, packet.getSlotId(), click, action, packet.getUsedButton());
                        } else {
                            event = new CraftItemEvent(recipe, inventory, type, packet.getSlotId(), click, action);
                        }
                    }
                }
                event.setCancelled(cancelled);
                Container oldContainer = this.player.openContainer;
                this.server.getPluginManager().callEvent(event);
                if (this.player.openContainer != oldContainer) {
                    return;
                }
                switch (event.getResult()) {
                    case DEFAULT:
                    case ALLOW: {
                        itemstack = this.player.openContainer.slotClick(packet.getSlotId(), packet.getUsedButton(), packet.getClickType(), this.player);
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
                                this.player.sendContainerToPlayer(this.player.openContainer);
                                break;
                            }
                            case PICKUP_SOME:
                            case PICKUP_HALF:
                            case PICKUP_ONE:
                            case PLACE_ALL:
                            case PLACE_SOME:
                            case PLACE_ONE:
                            case SWAP_WITH_CURSOR: {
                                this.player.connection.sendPacket(new SSetSlotPacket(-1, -1, this.player.inventory.getItemStack()));
                                this.player.connection.sendPacket(new SSetSlotPacket(this.player.openContainer.windowId, packet.getSlotId(), this.player.openContainer.getSlot(packet.getSlotId()).getStack()));
                                break;
                            }
                            case DROP_ALL_SLOT:
                            case DROP_ONE_SLOT: {
                                this.player.connection.sendPacket(new SSetSlotPacket(this.player.openContainer.windowId, packet.getSlotId(), this.player.openContainer.getSlot(packet.getSlotId()).getStack()));
                                break;
                            }
                            case DROP_ALL_CURSOR:
                            case DROP_ONE_CURSOR:
                            case CLONE_STACK: {
                                this.player.connection.sendPacket(new SSetSlotPacket(-1, -1, this.player.inventory.getItemStack()));
                                break;
                            }
                        }
                        return;
                    }
                }
                if (event instanceof CraftItemEvent) {
                    this.player.sendContainerToPlayer(this.player.openContainer);
                }
            }
            if (ItemStack.areItemStacksEqual(packet.getClickedItem(), itemstack)) {
                this.player.connection.sendPacket(new SConfirmTransactionPacket(packet.getWindowId(), packet.getActionNumber(), true));
                this.player.isChangingQuantityOnly = true;
                this.player.openContainer.detectAndSendChanges();
                this.player.updateHeldItem();
                this.player.isChangingQuantityOnly = false;
            } else {
                this.pendingTransactions.put(this.player.openContainer.windowId, packet.getActionNumber());
                this.player.connection.sendPacket(new SConfirmTransactionPacket(packet.getWindowId(), packet.getActionNumber(), false));
                this.player.openContainer.setCanCraft(this.player, false);
                NonNullList<ItemStack> nonnulllist1 = NonNullList.create();
                for (int j = 0; j < this.player.openContainer.inventorySlots.size(); ++j) {
                    ItemStack itemstack2 = this.player.openContainer.inventorySlots.get(j).getStack();
                    nonnulllist1.add(itemstack2.isEmpty() ? ItemStack.EMPTY : itemstack2);
                }
                this.player.sendAllContents(this.player.openContainer, nonnulllist1);
            }
        }
    }

    @Inject(method = "processEnchantItem", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;markPlayerActive()V"))
    private void arclight$noEnchant(CEnchantItemPacket packetIn, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void processCreativeInventoryAction(final CCreativeInventoryActionPacket packetplayinsetcreativeslot) {
        PacketThreadUtil.checkThreadAndEnqueue(packetplayinsetcreativeslot, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        if (this.player.interactionManager.isCreative()) {
            final boolean flag = packetplayinsetcreativeslot.getSlotId() < 0;
            ItemStack itemstack = packetplayinsetcreativeslot.getStack();
            final CompoundNBT nbttagcompound = itemstack.getChildTag("BlockEntityTag");
            if (!itemstack.isEmpty() && nbttagcompound != null && nbttagcompound.contains("x") && nbttagcompound.contains("y") && nbttagcompound.contains("z")) {
                final BlockPos blockposition = new BlockPos(nbttagcompound.getInt("x"), nbttagcompound.getInt("y"), nbttagcompound.getInt("z"));
                final TileEntity tileentity = this.player.world.getTileEntity(blockposition);
                if (tileentity != null) {
                    final CompoundNBT nbttagcompound2 = tileentity.write(new CompoundNBT());
                    nbttagcompound2.remove("x");
                    nbttagcompound2.remove("y");
                    nbttagcompound2.remove("z");
                    itemstack.setTagInfo("BlockEntityTag", nbttagcompound2);
                }
            }
            final boolean flag2 = packetplayinsetcreativeslot.getSlotId() >= 1 && packetplayinsetcreativeslot.getSlotId() <= 45;
            boolean flag3 = itemstack.isEmpty() || (itemstack.getDamage() >= 0 && itemstack.getCount() <= 64 && !itemstack.isEmpty());
            if (flag || (flag2 && !ItemStack.areItemStacksEqual(this.player.container.getSlot(packetplayinsetcreativeslot.getSlotId()).getStack(), packetplayinsetcreativeslot.getStack()))) {
                final InventoryView inventory = ((ContainerBridge) this.player.container).bridge$getBukkitView();
                final org.bukkit.inventory.ItemStack item = CraftItemStack.asBukkitCopy(packetplayinsetcreativeslot.getStack());
                InventoryType.SlotType type = InventoryType.SlotType.QUICKBAR;
                if (flag) {
                    type = InventoryType.SlotType.OUTSIDE;
                } else if (packetplayinsetcreativeslot.getSlotId() < 36) {
                    if (packetplayinsetcreativeslot.getSlotId() >= 5 && packetplayinsetcreativeslot.getSlotId() < 9) {
                        type = InventoryType.SlotType.ARMOR;
                    } else {
                        type = InventoryType.SlotType.CONTAINER;
                    }
                }
                final InventoryCreativeEvent event = new InventoryCreativeEvent(inventory, type, flag ? -999 : packetplayinsetcreativeslot.getSlotId(), item);
                this.server.getPluginManager().callEvent(event);
                itemstack = CraftItemStack.asNMSCopy(event.getCursor());
                switch (event.getResult()) {
                    case ALLOW: {
                        flag3 = true;
                    }
                    case DENY: {
                        if (packetplayinsetcreativeslot.getSlotId() >= 0) {
                            this.player.connection.sendPacket(new SSetSlotPacket(this.player.container.windowId, packetplayinsetcreativeslot.getSlotId(), this.player.container.getSlot(packetplayinsetcreativeslot.getSlotId()).getStack()));
                            this.player.connection.sendPacket(new SSetSlotPacket(-1, -1, ItemStack.EMPTY));
                        }
                        return;
                    }
                }
            }
            if (flag2 && flag3) {
                if (itemstack.isEmpty()) {
                    this.player.container.putStackInSlot(packetplayinsetcreativeslot.getSlotId(), ItemStack.EMPTY);
                } else {
                    this.player.container.putStackInSlot(packetplayinsetcreativeslot.getSlotId(), itemstack);
                }
                this.player.container.setCanCraft(this.player, true);
                this.player.container.detectAndSendChanges();
            } else if (flag && flag3 && this.itemDropThreshold < 200) {
                this.itemDropThreshold += 20;
                this.player.dropItem(itemstack, true);
            }
        }
    }

    @Inject(method = "processConfirmTransaction", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/IPacket;Lnet/minecraft/network/INetHandler;Lnet/minecraft/world/server/ServerWorld;)V"))
    private void arclight$noTransaction(CConfirmTransactionPacket packetIn, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "processUpdateSign", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;markPlayerActive()V"))
    private void arclight$noSignEdit(CUpdateSignPacket packetIn, CallbackInfo ci) {
        if (((ServerPlayerEntityBridge) player).bridge$isMovementBlocked()) {
            ci.cancel();
        }
    }

    private ITextComponent[] arclight$lines;

    @Inject(method = "processUpdateSign", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/client/CUpdateSignPacket;getLines()[Ljava/lang/String;"))
    public void arclight$onSignChangePre(CUpdateSignPacket packetIn, CallbackInfo ci) {
        String[] lines = packetIn.getLines();
        Player player = ((CraftServer) Bukkit.getServer()).getPlayer(this.player);
        CraftBlock block = CraftBlock.at(this.player.world, packetIn.getPosition());
        String[] bukkitLines = new String[lines.length];
        for (int i = 0; i < lines.length; i++) {
            bukkitLines[i] = TextFormatting.getTextWithoutFormattingCodes(
                new StringTextComponent(
                    Objects.requireNonNull(
                        TextFormatting.getTextWithoutFormattingCodes(lines[i])
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

    @Redirect(method = "processUpdateSign", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/SignTileEntity;setText(ILnet/minecraft/util/text/ITextComponent;)V"))
    public void arclight$onSignChangePost(SignTileEntity signTileEntity, int line, ITextComponent p_212365_2_) {
        if (arclight$lines != null) {
            signTileEntity.setText(line, arclight$lines[line]);
            if (line == arclight$lines.length - 1) {
                arclight$lines = null;
                ((SignTileEntityBridge) signTileEntity).bridge$setEditable(false);
            }
        }
    }

    @Inject(method = "processKeepAlive", at = @At("HEAD"))
    private void arclight$syncKeepAlive(CKeepAlivePacket packetIn, CallbackInfo ci) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void processPlayerAbilities(CPlayerAbilitiesPacket packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, (ServerPlayNetHandler) (Object) this, this.player.getServerWorld());
        if (this.player.abilities.allowFlying && this.player.abilities.isFlying != packet.isFlying()) {
            PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(this.server.getPlayer(this.player), packet.isFlying());
            this.server.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                this.player.abilities.isFlying = packet.isFlying();
            } else {
                this.player.sendPlayerAbilities();
            }
        }
    }

    private static final ResourceLocation CUSTOM_REGISTER = new ResourceLocation("register");
    private static final ResourceLocation CUSTOM_UNREGISTER = new ResourceLocation("unregister");

    @Inject(method = "processCustomPayload", at = @At("RETURN"))
    private void arclight$customPayload(CCustomPayloadPacket packet, CallbackInfo ci) {
        if (packet.channel.equals(CUSTOM_REGISTER)) {
            try {
                String channels = packet.data.toString(Charsets.UTF_8);
                for (String channel : channels.split("\0")) {
                    if (!StringUtils.isNullOrEmpty(channel)) {
                        this.getPlayer().addChannel(channel);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Couldn't register custom payload", ex);
                this.disconnect("Invalid payload REGISTER!");
            }
        } else if (packet.channel.equals(CUSTOM_UNREGISTER)) {
            try {
                final String channels = packet.data.toString(Charsets.UTF_8);
                for (String channel : channels.split("\0")) {
                    if (!StringUtils.isNullOrEmpty(channel)) {
                        this.getPlayer().removeChannel(channel);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Couldn't unregister custom payload", ex);
                this.disconnect("Invalid payload UNREGISTER!");
            }
        } else {
            try {
                final byte[] data = new byte[packet.data.readableBytes()];
                packet.data.readBytes(data);
                this.server.getMessenger().dispatchIncomingMessage(((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity(), packet.channel.toString(), data);
            } catch (Exception ex) {
                LOGGER.error("Couldn't dispatch custom payload", ex);
                this.disconnect("Invalid custom payload!");
            }
        }
    }

    public final boolean isDisconnected() {
        return !((ServerPlayerEntityBridge) this.player).bridge$isJoining() && !this.netManager.isChannelOpen();
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
    public void setPlayerLocation(double x, double y, double z, float yaw, float pitch, Set<SPlayerPositionLookPacket.Flags> relativeSet) {
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
        this.justTeleported = true;
        double d0 = relativeSet.contains(SPlayerPositionLookPacket.Flags.X) ? this.player.posX : 0.0D;
        double d1 = relativeSet.contains(SPlayerPositionLookPacket.Flags.Y) ? this.player.posY : 0.0D;
        double d2 = relativeSet.contains(SPlayerPositionLookPacket.Flags.Z) ? this.player.posZ : 0.0D;
        float f = relativeSet.contains(SPlayerPositionLookPacket.Flags.Y_ROT) ? this.player.rotationYaw : 0.0F;
        float f1 = relativeSet.contains(SPlayerPositionLookPacket.Flags.X_ROT) ? this.player.rotationPitch : 0.0F;
        this.targetPos = new Vec3d(x, y, z);
        if (++this.teleportId == Integer.MAX_VALUE) {
            this.teleportId = 0;
        }
        this.lastPosX = this.targetPos.x;
        this.lastPosY = this.targetPos.y;
        this.lastPosZ = this.targetPos.z;
        this.lastYaw = yaw;
        this.lastPitch = pitch;
        this.lastPositionUpdate = this.networkTickCount;
        this.player.setPositionAndRotation(x, y, z, yaw, pitch);
        this.player.connection.sendPacket(new SPlayerPositionLookPacket(x - d0, y - d1, z - d2, yaw - f, pitch - f1, relativeSet, this.teleportId));
    }

    public void a(double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        this.a(d0, d1, d2, f, f1, Collections.emptySet(), cause);
    }

    public void a(double d0, double d1, double d2, float f, float f1, Set<SPlayerPositionLookPacket.Flags> set, PlayerTeleportEvent.TeleportCause cause) {
        bridge$pushTeleportCause(cause);
        this.setPlayerLocation(d0, d1, d2, f, f1, set);
    }

    private void internalTeleport(double d0, double d1, double d2, float f, float f1, Set<SPlayerPositionLookPacket.Flags> set) {
        this.setPlayerLocation(d0, d1, d2, f, f1, set);
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
