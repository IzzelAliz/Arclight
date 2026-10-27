package io.izzel.arclight.common.mixin.core.world.inventory;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.ContainerLevelAccessBridge;
import io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftInventoryBeacon;
import org.bukkit.craftbukkit.inventory.view.CraftBeaconView;
import org.bukkit.craftbukkit.potion.CraftPotionEffectType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeaconMenu.class)
public abstract class BeaconMenuMixin extends AbstractContainerMenuMixin {

    // @formatter:off
    @Shadow @Final private Container beacon;
    @Shadow @Final private ContainerLevelAccess access;
    // @formatter:on

    private CraftBeaconView bukkitEntity;
    private Inventory playerInventory;

    @Inject(method = "<init>(ILnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    public void arclight$init(int id, Container inventory, ContainerData p_i50100_3_, ContainerLevelAccess worldPosCallable, CallbackInfo ci) {
        this.playerInventory = (Inventory) inventory;
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    // PaymentSlot is private, so these calls use WrapOperation's coerced receiver support.
    @WrapOperation(method = "updateEffects(Ljava/util/Optional;Ljava/util/Optional;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/BeaconMenu$PaymentSlot;hasItem()Z"),
        require = 1, allow = 1)
    private boolean arclight$changeBeaconEffects(@Coerce Object paymentSlot, Operation<Boolean> original,
                                                Optional<Holder<MobEffect>> primary, Optional<Holder<MobEffect>> secondary,
                                                @Share("arclightBeaconSelection") LocalRef<PlayerChangeBeaconEffectEvent> selection) {
        if (!original.call(paymentSlot)) {
            return false;
        }
        // Paper validates the packet's secondary before dispatch, not plugin replacements.
        if (secondary.isPresent() && secondary.get() != MobEffects.REGENERATION
            && primary.isPresent() && secondary.get() != primary.get()) {
            secondary = Optional.empty();
        }
        var event = new PlayerChangeBeaconEffectEvent(
            (org.bukkit.entity.Player) ((PlayerBridge) this.playerInventory.player).bridge$getBukkitEntity(),
            primary.map(CraftPotionEffectType::minecraftHolderToBukkit).orElse(null),
            secondary.map(CraftPotionEffectType::minecraftHolderToBukkit).orElse(null),
            ((ContainerLevelAccessBridge) this.access).bridge$getLocation().getBlock());
        selection.set(event);
        // The native if owns both data writes, payment removal and block-entity notification.
        return event.callEvent();
    }

    @WrapOperation(method = "updateEffects(Ljava/util/Optional;Ljava/util/Optional;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/BeaconMenu;encodeEffect(Lnet/minecraft/core/Holder;)I", ordinal = 0),
        require = 1, allow = 1)
    private int arclight$selectedPrimaryEffect(Holder<MobEffect> effect, Operation<Integer> original,
                                              @Share("arclightBeaconSelection") LocalRef<PlayerChangeBeaconEffectEvent> selection) {
        var event = selection.get();
        return original.call(event == null ? effect : event.getPrimary() == null ? null
            : CraftPotionEffectType.bukkitToMinecraftHolder(event.getPrimary()));
    }

    @WrapOperation(method = "updateEffects(Ljava/util/Optional;Ljava/util/Optional;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/BeaconMenu;encodeEffect(Lnet/minecraft/core/Holder;)I", ordinal = 1),
        require = 1, allow = 1)
    private int arclight$selectedSecondaryEffect(Holder<MobEffect> effect, Operation<Integer> original,
                                                @Share("arclightBeaconSelection") LocalRef<PlayerChangeBeaconEffectEvent> selection) {
        var event = selection.get();
        // Keep ContainerData.set and the block entity's native valid-effect filter.
        return original.call(event == null ? effect : event.getSecondary() == null ? null
            : CraftPotionEffectType.bukkitToMinecraftHolder(event.getSecondary()));
    }

    // PaymentSlot is private. WrapOperation supports a coerced receiver without widening it.
    @WrapOperation(method = "updateEffects(Ljava/util/Optional;Ljava/util/Optional;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/BeaconMenu$PaymentSlot;remove(I)Lnet/minecraft/world/item/ItemStack;"),
        require = 1, allow = 1)
    private ItemStack arclight$consumeBeaconPayment(@Coerce Object paymentSlot, int amount, Operation<ItemStack> original,
                                                   @Share("arclightBeaconSelection") LocalRef<PlayerChangeBeaconEffectEvent> selection) {
        var event = selection.get();
        return event != null && !event.willConsumeItem() ? ItemStack.EMPTY : original.call(paymentSlot, amount);
    }

    @Override
    public CraftBeaconView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        var inventory = new CraftInventoryBeacon(this.beacon);
        bukkitEntity = new CraftBeaconView(((PlayerBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (BeaconMenu) (Object) this);
        return bukkitEntity;
    }
}
