package io.izzel.arclight.forge.mixin.core.world.entity.player;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.core.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.PortalInfoBridge;
import io.izzel.arclight.common.mod.server.block.ChestBlockDoubleInventoryHacks;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.OptionalInt;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin_Forge extends PlayerMixin_Forge implements ServerPlayerEntityBridge {

    // @formatter:off
    @Shadow public abstract ServerLevel serverLevel();
    @Shadow public boolean isChangingDimension;
    @Shadow public boolean wonGame;
    @Shadow public ServerGamePacketListenerImpl connection;
    @Shadow private boolean seenCredits;
    @Shadow public abstract CommonPlayerSpawnInfo createCommonSpawnInfo(ServerLevel arg);
    @Shadow @Final public MinecraftServer server;
    @Shadow @Nullable private Vec3 enteredNetherPosition;
    @Shadow protected abstract void createEndPlatform(ServerLevel arg, BlockPos arg2);
    @Shadow public abstract void setServerLevel(ServerLevel arg);
    @Shadow public abstract void triggerDimensionChangeTriggers(ServerLevel arg);
    @Shadow @Final public ServerPlayerGameMode gameMode;
    @Shadow public int lastSentExp;
    @Shadow private float lastSentHealth;
    @Shadow private int lastSentFood;
    // @formatter:on

    @Inject(method = "die", cancellable = true, at = @At("HEAD"))
    private void arclight$onDeath(DamageSource source, CallbackInfo ci) {
        if (net.minecraftforge.common.ForgeHooks.onLivingDeath((ServerPlayer) (Object) this, source)) {
            ci.cancel();
        }
    }

    @Inject(method = "openHorseInventory", at = @At("TAIL"))
    private void arclight$openHorstContainer(AbstractHorse arg, Container arg2, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open((ServerPlayer) (Object) this, this.containerMenu));
    }

    @Inject(method = "setRespawnPosition", cancellable = true, at = @At("HEAD"))
    private void arclight$forgeRespawnPos(ResourceKey<Level> arg, BlockPos arg2, float f, boolean bl, boolean bl2, CallbackInfo ci) {
        if (ForgeEventFactory.onPlayerSpawnSet((ServerPlayer) (Object) this, arg2 == null ? Level.OVERWORLD : arg, arg2, bl)) {
            ci.cancel();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Override
    @Nullable
    public Entity changeDimension(ServerLevel arg) {
        return this.changeDimension(arg, arg.getPortalForcer());
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    @Nullable
    public Entity changeDimension(ServerLevel server, ITeleporter teleporter) {
        PlayerTeleportEvent.TeleportCause cause = bridge$getTeleportCause().orElse(PlayerTeleportEvent.TeleportCause.UNKNOWN);
        if (this.isSleeping()) {
            return (ServerPlayer) (Object) this;
        }
        if (ForgeEventFactory.onTravelToDimension((ServerPlayer) (Object) this, server.dimension())) return null;

        // this.invulnerableDimensionChange = true;
        ServerLevel serverworld = this.serverLevel();
        ResourceKey<LevelStem> registrykey = ((WorldBridge) serverworld).bridge$getTypeKey();
        if (registrykey == LevelStem.END && ((WorldBridge) server).bridge$getTypeKey() == LevelStem.OVERWORLD && teleporter.isVanilla()) { //Forge: Fix non-vanilla teleporters triggering end credits
            this.isChangingDimension = true;
            this.unRide();
            this.serverLevel().removePlayerImmediately((ServerPlayer) (Object) this, Entity.RemovalReason.CHANGED_DIMENSION);
            if (!this.wonGame) {
                this.wonGame = true;
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, this.seenCredits ? 0.0F : 1.0F));
                this.seenCredits = true;
            }

            return (ServerPlayer) (Object) this;
        } else {
            PortalInfo portalinfo = teleporter.getPortalInfo((ServerPlayer) (Object) this, server, this::findDimensionEntryPoint);
            if (portalinfo != null) {
                if (((PortalInfoBridge) portalinfo).bridge$getWorld() != null) {
                    server = ((PortalInfoBridge) portalinfo).bridge$getWorld();
                }
                ServerLevel[] exitWorld = new ServerLevel[]{server};
                LevelData iworldinfo = server.getLevelData();
                this.connection.send(new ClientboundRespawnPacket(this.createCommonSpawnInfo(server), (byte) 3));
                this.connection.send(new ClientboundChangeDifficultyPacket(iworldinfo.getDifficulty(), iworldinfo.isDifficultyLocked()));
                PlayerList playerlist = this.server.getPlayerList();
                playerlist.sendPlayerPermissionLevel((ServerPlayer) (Object) this);
                this.serverLevel().removePlayerImmediately((ServerPlayer) (Object) this, Entity.RemovalReason.CHANGED_DIMENSION);
                this.revive();
                Entity e = teleporter.placeEntity((ServerPlayer) (Object) this, serverworld, exitWorld[0], this.getYRot(), spawnPortal -> {//Forge: Start vanilla logic
                    serverworld.getProfiler().push("moving");
                    if (exitWorld[0] != null) {
                        if (registrykey == LevelStem.OVERWORLD && ((WorldBridge) exitWorld[0]).bridge$getTypeKey() == LevelStem.NETHER) {
                            this.enteredNetherPosition = this.position();
                        } else if (spawnPortal && ((WorldBridge) exitWorld[0]).bridge$getTypeKey() == LevelStem.END
                            && (((PortalInfoBridge) portalinfo).bridge$getPortalEventInfo() == null || ((PortalInfoBridge) portalinfo).bridge$getPortalEventInfo().getCanCreatePortal())) {
                            this.createEndPlatform(exitWorld[0], BlockPos.containing(portalinfo.pos));
                        }
                    }

                    Location enter = this.bridge$getBukkitEntity().getLocation();
                    Location exit = (exitWorld[0] == null) ? null : new Location(exitWorld[0].bridge$getWorld(), portalinfo.pos.x, portalinfo.pos.y, portalinfo.pos.z, portalinfo.yRot, portalinfo.xRot);
                    PlayerTeleportEvent tpEvent = new PlayerTeleportEvent(this.bridge$getBukkitEntity(), enter, exit, cause);
                    Bukkit.getServer().getPluginManager().callEvent(tpEvent);
                    if (tpEvent.isCancelled() || tpEvent.getTo() == null) {
                        return null;
                    }
                    exit = tpEvent.getTo();

                    serverworld.getProfiler().pop();
                    serverworld.getProfiler().push("placing");

                    this.isChangingDimension = true;
                    ServerLevel newWorld = ((CraftWorld) exit.getWorld()).getHandle();
                    if (newWorld != exitWorld[0]) {
                        exitWorld[0] = newWorld;
                        LevelData newWorldInfo = exitWorld[0].getLevelData();
                        this.connection.send(new ClientboundRespawnPacket(this.createCommonSpawnInfo(newWorld), (byte) 3));
                        this.connection.send(new ClientboundChangeDifficultyPacket(newWorldInfo.getDifficulty(), newWorldInfo.isDifficultyLocked()));
                    }

                    this.setServerLevel(exitWorld[0]);
                    exitWorld[0].addDuringPortalTeleport((ServerPlayer) (Object) this);

                    ((ServerPlayNetHandlerBridge) this.connection).bridge$teleport(exit);
                    this.connection.resetPosition();

                    serverworld.getProfiler().pop();
                    this.triggerDimensionChangeTriggers(exitWorld[0]);
                    return (ServerPlayer) (Object) this;//forge: this is part of the ITeleporter patch
                });//Forge: End vanilla logic
                if (e == null) {
                    serverworld.addDuringPortalTeleport((ServerPlayer) (Object) this);
                    return (ServerPlayer) (Object) this;
                } else if (e != (Object) this) {
                    throw new IllegalArgumentException(String.format("Teleporter %s returned not the player entity but instead %s, expected PlayerEntity %s", teleporter, e, this));
                }

                this.gameMode.setLevel(exitWorld[0]);
                this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
                playerlist.sendLevelInfo((ServerPlayer) (Object) this, exitWorld[0]);
                playerlist.sendAllPlayerInfo((ServerPlayer) (Object) this);

                for (MobEffectInstance effectinstance : this.getActiveEffects()) {
                    this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effectinstance));
                }

                if (teleporter.playTeleportSound((ServerPlayer) (Object) this, serverworld, exitWorld[0])) {
                    this.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
                }
                this.lastSentExp = -1;
                this.lastSentHealth = -1.0F;
                this.lastSentFood = -1;
                ForgeEventFactory.onPlayerChangedDimension((ServerPlayer) (Object) this, serverworld.dimension(), exitWorld[0].dimension());
                PlayerChangedWorldEvent changeEvent = new PlayerChangedWorldEvent(this.bridge$getBukkitEntity(), serverworld.bridge$getWorld());
                Bukkit.getPluginManager().callEvent(changeEvent);
            }

            return (ServerPlayer) (Object) this;
        }
    }

    @Redirect(method = "openMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;closeContainer()V"))
    private void arclight$skipSwitch(ServerPlayer serverPlayer) {
    }

    @Inject(method = "openMenu", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/MenuProvider;createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;"))
    private void arclight$invOpen(MenuProvider iTileInventory, CallbackInfoReturnable<OptionalInt> cir, AbstractContainerMenu container) {
        if (container != null) {
            ((ContainerBridge) container).bridge$setTitle(iTileInventory.getDisplayName());
            boolean cancelled = false;
            ArclightCaptures.captureContainerOwner((ServerPlayer) (Object) this);
            container = CraftEventFactory.callInventoryOpenEvent((ServerPlayer) (Object) this, container, cancelled);
            ArclightCaptures.resetContainerOwner();
            if (container == null && !cancelled) {
                if (iTileInventory instanceof Container) {
                    ((Container) iTileInventory).stopOpen((ServerPlayer) (Object) this);
                } else if (ChestBlockDoubleInventoryHacks.isInstance(iTileInventory)) {
                    ChestBlockDoubleInventoryHacks.get(iTileInventory).stopOpen((ServerPlayer) (Object) this);
                }
                cir.setReturnValue(OptionalInt.empty());
            }
        }
    }
}
