package io.izzel.arclight.common.mixin.core.world.entity.animal.frog;

import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(Tadpole.class)
public abstract class TadpoleMixin extends PathfinderMobMixin {

    @Shadow @Final private static EntityDataAccessor<Boolean> AGE_LOCKED;
    @Shadow public abstract boolean isAgeLocked();
    @Shadow protected abstract void setAge(int age);

    @Decorate(method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AgeableMob;setAgeLocked(Lnet/minecraft/world/entity/Mob;Ljava/util/function/Supplier;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)V"))
    private void arclight$toggleAgeLock(Mob mob, Supplier<Boolean> ageLocked, Player player, ItemStack item,
                                       Consumer<Mob> setAgeLockData, @Local(ordinal = 0) InteractionHand hand) throws Throwable {
        if (CraftEventFactory.callPlayerToggleEntityAgeLockEvent(player, mob, item, hand, !this.isAgeLocked(), AGE_LOCKED)) {
            DecorationOps.callsite().invoke(mob, ageLocked, player, item, setAgeLockData);
        }
        // The golden-dandelion branch still returns SUCCESS when the event is cancelled.
    }

    @Decorate(method = "ageUp()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/frog/Tadpole;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob arclight$transform(Tadpole tadpole, EntityType<?> type, ConversionParams params,
                                  ConversionParams.AfterConversion<?> afterConversion) throws Throwable {
        this.bridge$pushTransformReason(EntityTransformEvent.TransformReason.METAMORPHOSIS);
        ((ServerLevelBridge) this.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.METAMORPHOSIS);
        Mob result = (Mob) DecorationOps.callsite().invoke(tadpole, type, params, afterConversion);
        if (result == null) {
            this.setAge(0);
        }
        return result;
    }
}
