package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(BellBlockEntity.class)
public class BellBlockEntityMixin {

    @Redirect(method = "makeRaidersGlow", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"))
    private static void arclight$bellResonate(Stream<LivingEntity> instance, Consumer<? super LivingEntity> consumer, Level level, BlockPos pos) {
        var list = instance.map(it -> (org.bukkit.entity.LivingEntity) ((EntityBridge) it).bridge$getBukkitEntity()).toList();
        CraftEventFactory.handleBellResonateEvent(level, pos, list).forEach(consumer);
    }
}
