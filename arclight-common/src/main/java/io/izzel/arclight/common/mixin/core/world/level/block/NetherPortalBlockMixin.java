package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.EntityTypeBridge;
import io.izzel.arclight.common.bridge.core.world.TeleporterBridge;
import io.izzel.arclight.common.bridge.core.world.level.portal.DimensionTransitionBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.util.CraftLocation;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;)Lnet/minecraft/world/entity/Entity;"))
    public Entity arclight$spawn(EntityType<?> instance, ServerLevel p_262634_, BlockPos p_262707_, MobSpawnType p_262597_) {
        return ((EntityTypeBridge<?>) instance).bridge$spawnCreature(p_262634_, p_262707_, p_262597_, CreatureSpawnEvent.SpawnReason.NETHER_PORTAL);
    }

    @Inject(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setAsInsidePortal(Lnet/minecraft/world/level/block/Portal;Lnet/minecraft/core/BlockPos;)V"))
    public void arclight$portalEnter(BlockState state, Level worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        EntityPortalEnterEvent event = new EntityPortalEnterEvent(entityIn.bridge$getBukkitEntity(),
            new Location(worldIn.bridge$getWorld(), pos.getX(), pos.getY(), pos.getZ()));
        Bukkit.getPluginManager().callEvent(event);
    }

    @Decorate(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/NetherPortalBlock;getExitPortal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/level/border/WorldBorder;)Lnet/minecraft/world/level/portal/DimensionTransition;"))
    private DimensionTransition arclight$createPortalEvent(NetherPortalBlock instance, ServerLevel serverLevel, Entity entity, BlockPos blockPos, BlockPos blockPos2, boolean bl, WorldBorder worldBorder) throws Throwable {
        var event = ((EntityBridge) entity).bridge$callPortalEvent(entity, CraftLocation.toBukkit(blockPos2, serverLevel.bridge$getWorld()), PlayerTeleportEvent.TeleportCause.NETHER_PORTAL, bl ? 16 : 128, 16);
        if (event == null) {
            return null;
        }
        serverLevel = ((CraftWorld) event.getTo().getWorld()).getHandle();
        worldBorder = serverLevel.getWorldBorder();
        blockPos2 = worldBorder.clampToBounds(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());
        ((TeleporterBridge) serverLevel.getPortalForcer()).bridge$pushSearchRadius(event.getSearchRadius());
        ((TeleporterBridge) serverLevel.getPortalForcer()).bridge$pushPortalCreate(entity, event.getCreationRadius());
        var result = (DimensionTransition) DecorationOps.callsite().invoke(instance, serverLevel, entity, blockPos, blockPos2, bl, worldBorder);
        ((TeleporterBridge) serverLevel.getPortalForcer()).bridge$pushSearchRadius(-1);
        ((TeleporterBridge) serverLevel.getPortalForcer()).bridge$pushPortalCreate(null, -1);
        return result;
    }

    @Inject(method = "createDimensionTransition", at = @At("RETURN"))
    private static void arclight$setCause(ServerLevel serverLevel, BlockUtil.FoundRectangle foundRectangle, Direction.Axis axis, Vec3 vec3, Entity entity, Vec3 vec32, float f, float g, DimensionTransition.PostDimensionTransition postDimensionTransition, CallbackInfoReturnable<DimensionTransition> cir) {
        var dimensionTransition = cir.getReturnValue();
        if (dimensionTransition != null) {
            ((DimensionTransitionBridge) (Object) dimensionTransition).bridge$setTeleportCause(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
        }
    }
}
