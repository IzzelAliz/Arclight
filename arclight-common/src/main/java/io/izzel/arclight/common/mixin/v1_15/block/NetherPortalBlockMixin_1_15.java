package io.izzel.arclight.common.mixin.v1_15.block;

import io.izzel.arclight.common.bridge.entity.EntityTypeBridge;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin_1_15 {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;spawn(Lnet/minecraft/world/World;Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/SpawnReason;ZZ)Lnet/minecraft/entity/Entity;"))
    public Entity arclight$spawn(EntityType<?> entityType, World worldIn, CompoundNBT compound, ITextComponent customName, PlayerEntity playerIn, BlockPos pos, SpawnReason reason, boolean flag, boolean flag1) {
        return ((EntityTypeBridge<?>) entityType).bridge$spawnCreature(worldIn, compound, customName, playerIn, pos, reason, flag, flag1, CreatureSpawnEvent.SpawnReason.NETHER_PORTAL);
    }
}
