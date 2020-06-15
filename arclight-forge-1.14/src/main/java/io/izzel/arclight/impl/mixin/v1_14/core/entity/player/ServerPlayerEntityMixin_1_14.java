package io.izzel.arclight.impl.mixin.v1_14.core.entity.player;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.dimension.DimensionTypeBridge;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Blocks;
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
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.entity.Player;
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
public abstract class ServerPlayerEntityMixin_1_14 extends PlayerEntityMixin_1_14 implements ServerPlayerEntityBridge {

    // @formatter:off
    @Shadow public boolean invulnerableDimensionChange;
    @Shadow public abstract ServerWorld func_71121_q();
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
    @Nullable
    @Overwrite
    public Entity changeDimension(DimensionType dimensionmanager) {
        PlayerTeleportEvent.TeleportCause cause = bridge$getTeleportCause().orElse(PlayerTeleportEvent.TeleportCause.UNKNOWN);
        if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension((ServerPlayerEntity) (Object) this, dimensionmanager))
            return null;
        if (this.isSleeping()) {
            return (ServerPlayerEntity) (Object) this;
        }
        DimensionType dimensionmanager2 = this.dimension;
        if (((DimensionTypeBridge) dimensionmanager2).bridge$getType() == DimensionType.THE_END && ((DimensionTypeBridge) dimensionmanager).bridge$getType() == DimensionType.OVERWORLD) {
            this.invulnerableDimensionChange = true;
            this.detach();
            this.func_71121_q().removePlayer((ServerPlayerEntity) (Object) this, true);
            if (!this.queuedEndExit) {
                this.queuedEndExit = true;
                this.connection.sendPacket(new SChangeGameStatePacket(4, this.seenCredits ? 0.0f : 1.0f));
                this.seenCredits = true;
            }
            return (ServerPlayerEntity) (Object) this;
        }
        ServerWorld worldserver = this.server.func_71218_a(dimensionmanager2);
        ServerWorld worldserver2 = this.server.func_71218_a(dimensionmanager);
        WorldInfo worlddata = this.world.getWorldInfo();
        double d0 = this.posX;
        double d2 = this.posY;
        double d3 = this.posZ;
        float f = this.rotationPitch;
        float f2 = this.rotationYaw;
        double d4 = 8.0;
        float f3 = f2;
        worldserver.getProfiler().startSection("moving");
        if (worldserver2 != null) {
            if (dimensionmanager2 == DimensionType.OVERWORLD && dimensionmanager == DimensionType.THE_NETHER) {
                this.enteredNetherPosition = new Vec3d(this.posX, this.posY, this.posZ);
                d0 /= 8.0;
                d3 /= 8.0;
            } else if (dimensionmanager2 == DimensionType.THE_NETHER && dimensionmanager == DimensionType.OVERWORLD) {
                d0 *= 8.0;
                d3 *= 8.0;
            } else if (dimensionmanager2 == DimensionType.OVERWORLD && dimensionmanager == DimensionType.THE_END) {
                BlockPos blockposition = worldserver2.getSpawnCoordinate();
                d0 = blockposition.getX();
                d2 = blockposition.getY();
                d3 = blockposition.getZ();
                f2 = 90.0f;
                f = 0.0f;
            }
        }
        Location enter = this.bridge$getBukkitEntity().getLocation();
        Location exit = (worldserver2 == null) ? null : new Location(((WorldBridge) worldserver2).bridge$getWorld(), d0, d2, d3, f2, f);
        PlayerPortalEvent event = new PlayerPortalEvent((Player) this.bridge$getBukkitEntity(), enter, exit, cause);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null) {
            return null;
        }
        exit = event.getTo();
        if (exit == null) {
            return null;
        }
        PlayerTeleportEvent tpEvent = new PlayerTeleportEvent((Player) this.bridge$getBukkitEntity(), enter, exit, cause);
        Bukkit.getServer().getPluginManager().callEvent(tpEvent);
        if (tpEvent.isCancelled() || tpEvent.getTo() == null) {
            return null;
        }
        exit = tpEvent.getTo();
        if (exit == null) {
            return null;
        }
        worldserver2 = ((CraftWorld) exit.getWorld()).getHandle();
        d0 = exit.getX();
        d2 = exit.getY();
        d3 = exit.getZ();
        f2 = exit.getYaw();
        f = exit.getPitch();
        this.invulnerableDimensionChange = true;
        dimensionmanager = worldserver2.getDimension().getType();
        this.dimension = dimensionmanager;
        net.minecraftforge.fml.network.NetworkHooks.sendDimensionDataPacket(this.connection.netManager, (ServerPlayerEntity) (Object) this);
        this.connection.sendPacket(new SRespawnPacket(worldserver2.dimension.getType(), this.world.getWorldInfo().getGenerator(), this.interactionManager.getGameType()));
        this.connection.sendPacket(new SServerDifficultyPacket(this.world.getDifficulty(), this.world.getWorldInfo().isDifficultyLocked()));
        PlayerList playerlist = this.server.getPlayerList();
        playerlist.updatePermissionLevel((ServerPlayerEntity) (Object) this);
        worldserver.removePlayer((ServerPlayerEntity) (Object) this, true);
        this.removed = false;
        this.setLocationAndAngles(d0, d2, d3, f2, f);
        worldserver.getProfiler().endSection();
        worldserver.getProfiler().startSection("placing");
        double d5 = Math.min(-2.9999872E7, worldserver2.getWorldBorder().minX() + 16.0);
        double d6 = Math.min(-2.9999872E7, worldserver2.getWorldBorder().minZ() + 16.0);
        double d7 = Math.min(2.9999872E7, worldserver2.getWorldBorder().maxX() - 16.0);
        double d8 = Math.min(2.9999872E7, worldserver2.getWorldBorder().maxZ() - 16.0);
        d0 = MathHelper.clamp(d0, d5, d7);
        d3 = MathHelper.clamp(d3, d6, d8);
        this.setLocationAndAngles(d0, d2, d3, f2, f);
        if (((DimensionTypeBridge) dimensionmanager).bridge$getType() == DimensionType.THE_END) {
            int i = MathHelper.floor(this.posX);
            int j = MathHelper.floor(this.posY) - 1;
            int k = MathHelper.floor(this.posZ);
            boolean flag = true;
            boolean flag2 = false;
            BlockStateListPopulator blockList = new BlockStateListPopulator(worldserver2);
            for (int l = -2; l <= 2; ++l) {
                for (int i2 = -2; i2 <= 2; ++i2) {
                    for (int j2 = -1; j2 < 3; ++j2) {
                        int k2 = i + i2 * 1 + l * 0;
                        int l2 = j + j2;
                        int i3 = k + i2 * 0 - l * 1;
                        boolean flag3 = j2 < 0;
                        blockList.setBlockState(new BlockPos(k2, l2, i3), flag3 ? Blocks.OBSIDIAN.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
                    }
                }
            }
            org.bukkit.World bworld = ((WorldBridge) worldserver2).bridge$getWorld();
            PortalCreateEvent portalEvent = new PortalCreateEvent((List) blockList.getList(), bworld, this.bridge$getBukkitEntity(), PortalCreateEvent.CreateReason.END_PLATFORM);
            Bukkit.getPluginManager().callEvent(portalEvent);
            if (!portalEvent.isCancelled()) {
                blockList.updateList();
            }
            this.setLocationAndAngles(i, j, k, f2, 0.0f);
            this.setMotion(Vec3d.ZERO);
        } else if (!worldserver2.getDefaultTeleporter().placeInPortal((ServerPlayerEntity) (Object) this, f3)) {
            worldserver2.getDefaultTeleporter().makePortal((ServerPlayerEntity) (Object) this);
            worldserver2.getDefaultTeleporter().placeInPortal((ServerPlayerEntity) (Object) this, f3);
        }
        worldserver.getProfiler().endSection();
        this.setWorld(worldserver2);
        worldserver2.func_217447_b((ServerPlayerEntity) (Object) this);
        this.func_213846_b(worldserver);
        this.connection.setPlayerLocation(this.posX, this.posY, this.posZ, f2, f);
        this.interactionManager.func_73080_a(worldserver2);
        this.connection.sendPacket(new SPlayerAbilitiesPacket(this.abilities));
        playerlist.func_72354_b((ServerPlayerEntity) (Object) this, worldserver2);
        playerlist.sendInventory((ServerPlayerEntity) (Object) this);
        for (EffectInstance mobeffect : this.getActivePotionEffects()) {
            this.connection.sendPacket(new SPlayEntityEffectPacket(this.getEntityId(), mobeffect));
        }
        this.connection.sendPacket(new SPlaySoundEventPacket(1032, BlockPos.ZERO, 0, false));
        this.lastExperience = -1;
        this.lastHealth = -1.0f;
        this.lastFoodLevel = -1;
        PlayerChangedWorldEvent changeEvent = new PlayerChangedWorldEvent((Player) this.bridge$getBukkitEntity(), ((WorldBridge) worldserver).bridge$getWorld());
        Bukkit.getPluginManager().callEvent(changeEvent);
        net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerChangedDimensionEvent((ServerPlayerEntity) (Object) this, dimensionmanager2, dimensionmanager);
        return (ServerPlayerEntity) (Object) this;
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

    @Inject(method = "wakeUpPlayer", cancellable = true, at = @At("HEAD"))
    private void arclight$notWake(boolean immediately, boolean updateWorldFlag, boolean setSpawn, CallbackInfo ci) {
        if (!isSleeping()) ci.cancel();
    }

    @Override
    public Entity bridge$changeDimension(DimensionType dimensionType, PlayerTeleportEvent.TeleportCause cause) {
        return a(dimensionType, cause);
    }

    @Inject(method = "setElytraFlying", cancellable = true, at = @At("HEAD"))
    private void arclight$beginGlide(CallbackInfo ci) {
        if (CraftEventFactory.callToggleGlideEvent((ServerPlayerEntity) (Object) this, true).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "clearElytraFlying", cancellable = true, at = @At("HEAD"))
    private void arclight$endGlide(CallbackInfo ci) {
        if (CraftEventFactory.callToggleGlideEvent((ServerPlayerEntity) (Object) this, false).isCancelled()) {
            ci.cancel();
        }
    }
}
