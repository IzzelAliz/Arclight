package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.world.entity.AgeableMobBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(AgeableMob.class)
public abstract class AgeableMobMixin extends PathfinderMobMixin implements AgeableMobBridge {

    // @formatter:off
    @Shadow @Final private static EntityDataAccessor<Boolean> AGE_LOCKED;
    @Shadow public abstract boolean isBaby();
    @Shadow @Nullable public abstract AgeableMob getBreedOffspring(ServerLevel world, AgeableMob mate);
    @Shadow public abstract void setAge(int age);
    @Shadow public abstract boolean isAgeLocked();
    @Shadow public abstract int getAge();
    @Shadow public abstract boolean canAgeUp();
    // @formatter:on

    @Override
    public void bridge$inactiveTick() {
        super.bridge$inactiveTick();
        if (this.isAlive()) {
            int age = this.getAge();
            if (this.canAgeUp()) {
                this.setAge(++age);
            } else if (age > 0) {
                this.setAge(--age);
            }
        }
    }

    @Decorate(method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AgeableMob;setAgeLocked(Lnet/minecraft/world/entity/Mob;Ljava/util/function/Supplier;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)V"))
    private void arclight$toggleAgeLock(Mob mob, Supplier<Boolean> ageLocked, Player player, ItemStack item,
                                       Consumer<Mob> setAgeLockData, @Local(ordinal = 0) InteractionHand hand) throws Throwable {
        if (CraftEventFactory.callPlayerToggleEntityAgeLockEvent(player, mob, item, hand, !this.isAgeLocked(), AGE_LOCKED)) {
            DecorationOps.callsite().invoke(mob, ageLocked, player, item, setAgeLockData);
        }
        // Both outcomes retain the native SUCCESS return; a veto skips all toggle side effects.
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isClientSide()Z"))
    private boolean arclight$tickIfNotLocked(Level world) {
        return world.isClientSide() || this.isAgeLocked();
    }

    @Override
    public boolean bridge$isAgeLocked() {
        return this.isAgeLocked();
    }
}
