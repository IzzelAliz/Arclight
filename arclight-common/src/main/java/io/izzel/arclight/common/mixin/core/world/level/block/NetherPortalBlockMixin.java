package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityTypeBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;)Lnet/minecraft/world/entity/Entity;"))
    public Entity arclight$spawn(EntityType<?> instance, ServerLevel p_262634_, BlockPos p_262707_, MobSpawnType p_262597_) {
        return ((EntityTypeBridge<?>) instance).bridge$spawnCreature(p_262634_, p_262707_, p_262597_, CreatureSpawnEvent.SpawnReason.NETHER_PORTAL);
    }

    @Inject(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;handleInsidePortal(Lnet/minecraft/core/BlockPos;)V"))
    public void arclight$portalEnter(BlockState state, Level worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        EntityPortalEnterEvent event = new EntityPortalEnterEvent(entityIn.bridge$getBukkitEntity(),
            new Location(worldIn.bridge$getWorld(), pos.getX(), pos.getY(), pos.getZ()));
        Bukkit.getPluginManager().callEvent(event);
    }
}
