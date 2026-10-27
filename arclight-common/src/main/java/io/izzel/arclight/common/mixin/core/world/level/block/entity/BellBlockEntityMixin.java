package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.block.BellRevealRaiderEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(BellBlockEntity.class)
public class BellBlockEntityMixin {

    @Decorate(method = "makeRaidersGlow(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Ljava/util/List;)V", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"), require = 1)
    private static void arclight$bellResonate(Stream<LivingEntity> instance, Consumer<? super LivingEntity> consumer,
            @Local(ordinal = 0) Level level, @Local(ordinal = 0) BlockPos pos) throws Throwable {
        var list = instance.map(it -> (org.bukkit.entity.LivingEntity) ((EntityBridge) it).bridge$getBukkitEntity()).collect(Collectors.toCollection(ArrayList::new));
        var resonatedEntities = CraftEventFactory.handleBellResonateEvent(level, pos, list);
        Consumer<LivingEntity> reveal = entity -> {
            var bukkitEntity = ((EntityBridge) entity).bridge$getBukkitEntity();
            // A mod's RAIDERS tag (or the mutable resonate list) can include non-Raiders.
            // Preserve their native consumer; the legacy event only represents Bukkit Raiders.
            if (!(bukkitEntity instanceof Raider raider)
                || new BellRevealRaiderEvent(CraftBlock.at(entity.level(), pos), raider).callEvent()) {
                consumer.accept(entity);
            }
        };
        DecorationOps.callsite().invoke(resonatedEntities, reveal);
    }
}
