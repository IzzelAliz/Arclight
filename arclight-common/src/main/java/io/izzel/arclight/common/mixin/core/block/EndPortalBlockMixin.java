package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {

    @Redirect(method = "onEntityCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/util/RegistryKey;)Lnet/minecraft/world/server/ServerWorld;"))
    public ServerWorld arclight$enterPortal(MinecraftServer minecraftServer, RegistryKey<World> dimension, BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
        ServerWorld world = minecraftServer.getWorld(dimension);
        EntityPortalEnterEvent event = new EntityPortalEnterEvent(((EntityBridge) entityIn).bridge$getBukkitEntity(),
            new Location(((WorldBridge) worldIn).bridge$getWorld(), pos.getX(), pos.getY(), pos.getZ()));
        Bukkit.getPluginManager().callEvent(event);
        if (entityIn instanceof ServerPlayerEntity && world != null) {
            ((ServerPlayerEntityBridge) entityIn).bridge$changeDimension(world, PlayerTeleportEvent.TeleportCause.END_PORTAL);
            return null;
        }
        return world;
    }
}
