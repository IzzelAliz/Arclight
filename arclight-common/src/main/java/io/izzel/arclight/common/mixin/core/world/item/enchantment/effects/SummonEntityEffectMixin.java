package io.izzel.arclight.common.mixin.core.world.item.enchantment.effects;

import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.SummonEntityEffect;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SummonEntityEffect.class)
public abstract class SummonEntityEffectMixin {

    // Deliberately use CraftBukkit's create-then-admit path instead of the spawn wrapper chain.
    @Redirect(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"), require = 1)
    private Entity arclight$create(EntityType<?> type, ServerLevel level, BlockPos pos, EntitySpawnReason reason) {
        return type.create(level, null, pos, reason, false, false);
    }

    // Native setCause has already run. Keep joinTeam/snapTo even if admission is cancelled.
    @Inject(method = "apply", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/enchantment/effects/SummonEntityEffect;joinTeam:Z"), require = 1)
    private void arclight$admit(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item,
                               Entity entity, Vec3 position, CallbackInfo ci, @Local(index = 8) Entity spawned) {
        ServerLevelBridge bridge = (ServerLevelBridge) level;
        if (spawned instanceof LightningBolt lightning) {
            bridge.bridge$strikeLightning(lightning, item.itemStack().getItem() == Items.TRIDENT
                ? LightningStrikeEvent.Cause.TRIDENT : LightningStrikeEvent.Cause.ENCHANTMENT);
        } else {
            bridge.bridge$addAllEntities(spawned, CreatureSpawnEvent.SpawnReason.ENCHANTMENT);
        }
    }
}
