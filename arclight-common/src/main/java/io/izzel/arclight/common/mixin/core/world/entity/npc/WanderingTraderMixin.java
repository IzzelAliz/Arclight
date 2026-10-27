package io.izzel.arclight.common.mixin.core.world.entity.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderMixin extends AbstractVillagerMixin {

    public boolean canDrinkPotion = true;
    public boolean canDrinkMilk = true;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"), require = 1)
    private void arclight$defaultDespawnDelay(EntityType<? extends WanderingTrader> type, Level level, CallbackInfo ci) {
        ((WanderingTrader) (Object) this).setDespawnDelay(48000);
    }

    @ModifyArg(method = "registerGoals()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/UseItemGoal;<init>(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/sounds/SoundEvent;Ljava/util/function/Predicate;)V", ordinal = 0), index = 3, require = 1)
    private Predicate<WanderingTrader> arclight$canDrinkPotion(Predicate<WanderingTrader> original) {
        return trader -> this.canDrinkPotion && original.test(trader);
    }

    @ModifyArg(method = "registerGoals()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/UseItemGoal;<init>(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/sounds/SoundEvent;Ljava/util/function/Predicate;)V", ordinal = 1), index = 3, require = 1)
    private Predicate<WanderingTrader> arclight$canDrinkMilk(Predicate<WanderingTrader> original) {
        return trader -> this.canDrinkMilk && original.test(trader);
    }

    @Inject(method = "maybeDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/wanderingtrader/WanderingTrader;discard()V"))
    private void arclight$despawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }
}
