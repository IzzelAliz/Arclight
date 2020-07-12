package io.izzel.arclight.common.mixin.v1_15.entity.player;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.world.TeleporterBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.dimension.DimensionTypeBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Blocks;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.SChangeGameStatePacket;
import net.minecraft.network.play.server.SPlayEntityEffectPacket;
import net.minecraft.network.play.server.SPlaySoundEventPacket;
import net.minecraft.network.play.server.SPlayerAbilitiesPacket;
import net.minecraft.network.play.server.SRespawnPacket;
import net.minecraft.network.play.server.SServerDifficultyPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.server.management.PlayerList;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Teleporter;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.hooks.BasicEventHooks;
import net.minecraftforge.fml.network.NetworkHooks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin_1_15 extends PlayerEntityMixin_1_15 implements ServerPlayerEntityBridge {

    // @formatter:off
    @Shadow public boolean invulnerableDimensionChange;
    @Shadow public abstract ServerWorld getServerWorld();
    @Shadow public boolean queuedEndExit;
    @Shadow public ServerPlayNetHandler connection;
    @Shadow private boolean seenCredits;
    @Shadow @Final public MinecraftServer server;
    @Shadow @Final public PlayerInteractionManager interactionManager;
    @Shadow @Nullable private Vec3d enteredNetherPosition;
    @Shadow public abstract void func_213846_b(ServerWorld p_213846_1_);
    @Shadow public int lastExperience;
    @Shadow private float lastHealth;
    @Shadow private int lastFoodLevel;
    // @formatter:on

    @Override
    public CraftPlayer bridge$getBukkitEntity() {
        return (CraftPlayer) ((InternalEntityBridge) this).internal$getBukkitEntity();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    @Nullable
    public Entity changeDimension(DimensionType dim, ITeleporter teleporter) {
        DimensionType[] destination = {dim};
        if (this.isSleeping()) return (ServerPlayerEntity) (Object) this;

        if (!ForgeHooks.onTravelToDimension((ServerPlayerEntity) (Object) this, destination[0])) return null;

        PlayerTeleportEvent.TeleportCause cause = bridge$getTeleportCause().orElse(PlayerTeleportEvent.TeleportCause.UNKNOWN);
        // this.invulnerableDimensionChange = true;
        DimensionType dimensiontype = this.dimension;
        if (((DimensionTypeBridge) dimensiontype).bridge$getType() == DimensionType.THE_END && ((DimensionTypeBridge) destination[0]).bridge$getType() == DimensionType.OVERWORLD && teleporter instanceof Teleporter) { //Forge: Fix non-vanilla teleporters triggering end credits
            this.invulnerableDimensionChange = true;
            this.detach();
            this.getServerWorld().removePlayer((ServerPlayerEntity) (Object) this, true); //Forge: The player entity is cloned so keep the data until after cloning calls copyFrom
            if (!this.queuedEndExit) {
                this.queuedEndExit = true;
                this.connection.sendPacket(new SChangeGameStatePacket(4, this.seenCredits ? 0.0F : 1.0F));
                this.seenCredits = true;
            }

            return (ServerPlayerEntity) (Object) this;
        } else {
            ServerWorld serverworld = this.server.getWorld(dimensiontype);
            // this.dimension = destination;
            ServerWorld[] serverworld1 = {this.server.getWorld(destination[0])};

            /*
            WorldInfo worldinfo = serverworld1.getWorldInfo();
            NetworkHooks.sendDimensionDataPacket(this.connection.netManager, (ServerPlayerEntity) (Object) this);
            this.connection.sendPacket(new SRespawnPacket(destination, WorldInfo.byHashing(worldinfo.getSeed()), worldinfo.getGenerator(), this.interactionManager.getGameType()));
            this.connection.sendPacket(new SServerDifficultyPacket(worldinfo.getDifficulty(), worldinfo.isDifficultyLocked()));
            PlayerList playerlist = this.server.getPlayerList();
            playerlist.updatePermissionLevel((ServerPlayerEntity) (Object) this);
            serverworld.removeEntity((ServerPlayerEntity) (Object) this, true); //Forge: the player entity is moved to the new world, NOT cloned. So keep the data alive with no matching invalidate call.
            this.revive();
            */
            PlayerList[] playerlist = new PlayerList[1];

            Entity e = teleporter.placeEntity((ServerPlayerEntity) (Object) this, serverworld, serverworld1[0], this.rotationYaw, spawnPortal -> {//Forge: Start vanilla logic
                double d0 = this.getPosX();
                double d1 = this.getPosY();
                double d2 = this.getPosZ();
                float f = this.rotationPitch;
                float f1 = this.rotationYaw;
                double d3 = 8.0D;
                float f2 = f1;
                serverworld.getProfiler().startSection("moving");

                if (serverworld1[0] != null) {
                    double moveFactor = serverworld.getDimension().getMovementFactor() / serverworld1[0].getDimension().getMovementFactor();
                    d0 *= moveFactor;
                    d2 *= moveFactor;
                    if (dimensiontype == DimensionType.OVERWORLD && destination[0] == DimensionType.THE_NETHER) {
                        this.enteredNetherPosition = this.getPositionVec();
                    } else if (dimensiontype == DimensionType.OVERWORLD && destination[0] == DimensionType.THE_END) {
                        BlockPos blockpos = serverworld1[0].getSpawnCoordinate();
                        d0 = blockpos.getX();
                        d1 = blockpos.getY();
                        d2 = blockpos.getZ();
                        f1 = 90.0F;
                        f = 0.0F;
                    }
                }

                Location enter = this.bridge$getBukkitEntity().getLocation();
                Location exit = (serverworld1[0] == null) ? null : new Location(((ServerWorldBridge) serverworld1[0]).bridge$getWorld(), d0, d1, d2, f1, f);
                PlayerPortalEvent event = new PlayerPortalEvent(this.bridge$getBukkitEntity(), enter, exit, cause, 128, true, ((DimensionTypeBridge) destination[0]).bridge$getType() == DimensionType.THE_END ? 0 : 16);
                Bukkit.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled() || event.getTo() == null) {
                    return null;
                }

                exit = event.getTo();
                if (exit == null) {
                    return null;
                }
                serverworld1[0] = ((CraftWorld) exit.getWorld()).getHandle();
                d0 = exit.getX();
                d1 = exit.getY();
                d2 = exit.getZ();

                // this.setLocationAndAngles(d0, d1, d2, f1, f);
                serverworld.getProfiler().endSection();
                serverworld.getProfiler().startSection("placing");
                double d7 = Math.max(-2.9999872E7D, serverworld1[0].getWorldBorder().minX() + 16.0D);
                double d4 = Math.max(-2.9999872E7D, serverworld1[0].getWorldBorder().minZ() + 16.0D);
                double d5 = Math.min(2.9999872E7D, serverworld1[0].getWorldBorder().maxX() - 16.0D);
                double d6 = Math.min(2.9999872E7D, serverworld1[0].getWorldBorder().maxZ() - 16.0D);
                d0 = MathHelper.clamp(d0, d7, d5);
                d2 = MathHelper.clamp(d2, d4, d6);
                // this.setLocationAndAngles(d0, d1, d2, f1, f);

                Vec3d exitVelocity = Vec3d.ZERO;
                BlockPos exitPosition = new BlockPos(d0, d1, d2);

                if (((DimensionTypeBridge) destination[0]).bridge$getType() == DimensionType.THE_END) {
                    int i = exitPosition.getX();
                    int j = exitPosition.getY() - 1;
                    int k = exitPosition.getZ();

                    if (event.getCanCreatePortal()) {

                        BlockStateListPopulator blockList = new BlockStateListPopulator(serverworld1[0]);

                        for (int j1 = -2; j1 <= 2; ++j1) {
                            for (int k1 = -2; k1 <= 2; ++k1) {
                                for (int l1 = -1; l1 < 3; ++l1) {
                                    int i2 = i + k1 * 1 + j1 * 0;
                                    int j2 = j + l1;
                                    int k2 = k + k1 * 0 - j1 * 1;
                                    boolean flag = l1 < 0;
                                    blockList.setBlockState(new BlockPos(i2, j2, k2), flag ? Blocks.OBSIDIAN.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
                                }
                            }
                        }

                        org.bukkit.World bworld = ((ServerWorldBridge) serverworld1[0]).bridge$getWorld();
                        PortalCreateEvent portalEvent = new PortalCreateEvent((List<BlockState>) (List) blockList.getList(), bworld, this.bridge$getBukkitEntity(), PortalCreateEvent.CreateReason.END_PLATFORM);

                        Bukkit.getPluginManager().callEvent(portalEvent);
                        if (!portalEvent.isCancelled()) {
                            blockList.updateList();
                        }
                    }

                    // this.setLocationAndAngles(i, j, k, f1, 0.0F);
                    exit.setX(i);
                    exit.setY(j);
                    exit.setZ(k);
                    // this.setMotion(Vec3d.ZERO);
                    exitVelocity = Vec3d.ZERO;
                } else {
                    BlockPattern.PortalInfo portalInfo = ((TeleporterBridge) serverworld1[0].getDefaultTeleporter()).bridge$placeInPortal((ServerPlayerEntity) (Object) this, exitPosition, f2, event.getSearchRadius(), true);
                    if (spawnPortal && portalInfo == null && event.getCanCreatePortal()) {
                        if (((TeleporterBridge) serverworld1[0].getDefaultTeleporter()).bridge$makePortal((ServerPlayerEntity) (Object) this, exitPosition, event.getCreationRadius())) {
                            // serverworld1.getDefaultTeleporter().placeInPortal((ServerPlayerEntity) (Object) this, f2);
                            portalInfo = ((TeleporterBridge) serverworld1[0].getDefaultTeleporter()).bridge$placeInPortal((ServerPlayerEntity) (Object) this, exitPosition, f2, event.getSearchRadius(), true);
                        }
                    }
                    if (portalInfo == null) {
                        return null;
                    }

                    exitVelocity = portalInfo.motion;
                    exit.setX(portalInfo.pos.getX());
                    exit.setY(portalInfo.pos.getY());
                    exit.setZ(portalInfo.pos.getZ());
                    exit.setYaw(f2 + (float) portalInfo.rotation);
                }

                serverworld.getProfiler().endSection();

                PlayerTeleportEvent tpEvent = new PlayerTeleportEvent(this.bridge$getBukkitEntity(), enter, exit, cause);
                Bukkit.getServer().getPluginManager().callEvent(tpEvent);
                if (tpEvent.isCancelled() || tpEvent.getTo() == null) {
                    return null;
                }
                exit = tpEvent.getTo();
                if (exit == null) {
                    return null;
                }
                serverworld1[0] = ((CraftWorld) exit.getWorld()).getHandle();
                this.invulnerableDimensionChange = true;

                destination[0] = serverworld1[0].getDimension().getType();
                this.dimension = destination[0];

                WorldInfo worldinfo = serverworld1[0].getWorldInfo();
                NetworkHooks.sendDimensionDataPacket(this.connection.netManager, (ServerPlayerEntity) (Object) this);
                this.connection.sendPacket(new SRespawnPacket(destination[0], WorldInfo.byHashing(worldinfo.getSeed()), worldinfo.getGenerator(), this.interactionManager.getGameType()));
                this.connection.sendPacket(new SServerDifficultyPacket(worldinfo.getDifficulty(), worldinfo.isDifficultyLocked()));

                playerlist[0] = this.server.getPlayerList();
                playerlist[0].updatePermissionLevel((ServerPlayerEntity) (Object) this);

                serverworld.removeEntity((ServerPlayerEntity) (Object) this, true); //Forge: the player entity is moved to the new world, NOT cloned. So keep the data alive with no matching invalidate call.
                this.revive();

                this.setMotion(exitVelocity);

                this.setWorld(serverworld1[0]);
                serverworld1[0].addDuringPortalTeleport((ServerPlayerEntity) (Object) this);
                this.func_213846_b(serverworld);

                // this.connection.setPlayerLocation(this.getPosX(), this.getPosY(), this.getPosZ(), f1, f);
                ((ServerPlayNetHandlerBridge) this.connection).bridge$teleport(exit);
                this.connection.captureCurrentPosition();

                return (ServerPlayerEntity) (Object) this;//forge: this is part of the ITeleporter patch
            });//Forge: End vanilla logic
            if (e == null) {
                return (ServerPlayerEntity) (Object) this;
            } else if (e != (Object) this) {
                throw new IllegalArgumentException(String.format("Teleporter %s returned not the player entity but instead %s, expected PlayerEntity %s", teleporter, e, this));
            }
            this.interactionManager.setWorld(serverworld1[0]);
            this.connection.sendPacket(new SPlayerAbilitiesPacket(this.abilities));
            playerlist[0].sendWorldInfo((ServerPlayerEntity) (Object) this, serverworld1[0]);
            playerlist[0].sendInventory((ServerPlayerEntity) (Object) this);

            for (EffectInstance effectinstance : this.getActivePotionEffects()) {
                this.connection.sendPacket(new SPlayEntityEffectPacket(this.getEntityId(), effectinstance));
            }

            this.connection.sendPacket(new SPlaySoundEventPacket(1032, BlockPos.ZERO, 0, false));
            this.lastExperience = -1;
            this.lastHealth = -1.0F;
            this.lastFoodLevel = -1;
            BasicEventHooks.firePlayerChangedDimensionEvent((ServerPlayerEntity) (Object) this, dimensiontype, destination[0]);

            PlayerChangedWorldEvent changeEvent = new PlayerChangedWorldEvent(this.bridge$getBukkitEntity(), ((WorldBridge) serverworld).bridge$getWorld());
            Bukkit.getPluginManager().callEvent(changeEvent);
            return (ServerPlayerEntity) (Object) this;
        }
    }

    public Entity a(DimensionType dimensionmanager, final PlayerTeleportEvent.TeleportCause cause) {
        bridge$pushChangeDimensionCause(cause);
        return this.changeDimension(dimensionmanager);
    }

    @Override
    public Either<PlayerEntity.SleepResult, Unit> sleep(BlockPos at, boolean force) {
        return super.sleep(at, force).ifRight((p_213849_1_) -> {
            this.addStat(Stats.SLEEP_IN_BED);
            CriteriaTriggers.SLEPT_IN_BED.trigger((ServerPlayerEntity) (Object) this);
        });
    }

    @Inject(method = "stopSleepInBed", cancellable = true, at = @At("HEAD"))
    private void arclight$notWake(boolean flag, boolean flag1, CallbackInfo ci) {
        if (!isSleeping()) ci.cancel();
    }

    @Override
    public Entity bridge$changeDimension(DimensionType dimensionType, PlayerTeleportEvent.TeleportCause cause) {
        return a(dimensionType, cause);
    }
}
