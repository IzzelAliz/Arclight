package io.izzel.arclight.neoforge.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.neoforge.mixin.core.world.entity.MobMixin_NeoForge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiConsumer;

@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin_NeoForge extends MobMixin_NeoForge {

    @Decorate(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/cow/MushroomCow;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob arclight$animalTransform(MushroomCow mushroomCow, EntityType<? extends Mob> entityType, ConversionParams conversionParams, ConversionParams.AfterConversion afterConversion, ServerLevel level, SoundSource source, ItemStack stack) throws Throwable {
        ((WorldBridge) mushroomCow.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SHEARED);
        ((MobBridge) mushroomCow).bridge$pushTransformReason(EntityTransformEvent.TransformReason.SHEARED);
        return (Mob) DecorationOps.callsite().invoke(mushroomCow, entityType, conversionParams, afterConversion);
    }

    @Decorate(method = "lambda$shear$2", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/cow/MushroomCow;dropFromShearingLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/BiConsumer;)V"))
    private void arclight$forceDrop(MushroomCow mushroomCow, ServerLevel level, ResourceKey<LootTable> lootTable, ItemStack stack, BiConsumer<ServerLevel, ItemStack> dropper, ServerLevel conversionLevel, ItemStack tool, Cow cow) throws Throwable {
        var bridge = (io.izzel.arclight.common.bridge.core.entity.EntityBridge) mushroomCow;
        boolean forceDrops = bridge.bridge$isForceDrops();
        bridge.bridge$setForceDrops(true);
        try {
            DecorationOps.callsite().invoke(mushroomCow, level, lootTable, stack, dropper);
        } finally {
            bridge.bridge$setForceDrops(forceDrops);
        }
    }
}
