package io.izzel.arclight.forge.mixin.core.world.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityDamageItemEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.common.extensions.IForgeItemStack;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin_Forge implements ItemStackBridge, IForgeItemStack {

    // @formatter:off
    @Shadow private int count;
    // @formatter:on

    @WrapOperation(method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/common/ForgeHooks;onPlaceItemIntoWorld(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"), require = 1, allow = 1)
    private InteractionResult arclight$signPlacement(UseOnContext context, Operation<InteractionResult> original) {
        return io.izzel.arclight.common.mod.util.TreeCapture.useOn(
            (ItemStack) (Object) this, context, () -> ArclightEventFactory.useSignItem(context, original));
    }

    @Override
    public InteractionResult bridge$forge$onItemUseFirst(UseOnContext context) {
        return onItemUseFirst(context);
    }

    @Override
    public boolean bridge$forge$doesSneakBypassUse(LevelReader level, BlockPos pos, Player player) {
        return doesSneakBypassUse(level, pos, player);
    }

    @Decorate(method = "processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;ZLjava/util/function/Consumer;)I",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;hasInfiniteMaterials()Z"), require = 1, allow = 1)
    private boolean arclight$forceItemDamage(ServerPlayer owner) throws Throwable {
        boolean infiniteMaterials = (boolean) DecorationOps.callsite().invoke(owner);
        return infiniteMaterials && !ArclightCaptures.isItemDamageForced((ItemStack) (Object) this, owner);
    }

    @Inject(method = "processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;ZLjava/util/function/Consumer;)I",
        at = @At("RETURN"), cancellable = true, require = 3, allow = 3)
    private void arclight$itemDamage(int amount, ServerLevel level, @Nullable ServerPlayer damager, boolean canBreak, Consumer<Item> onBreak, CallbackInfoReturnable<Integer> cir) {
        // The non-breaking sibling passes false: do not eventize hurtWithoutBreaking.
        if (!canBreak) {
            return;
        }
        ItemStack stack = (ItemStack) (Object) this;
        LivingEntity apiOwner = ArclightCaptures.getItemDamageOwner(stack, damager);
        int result = cir.getReturnValueI();
        if (apiOwner != null && !(apiOwner instanceof ServerPlayer)) {
            if (result > 0) {
                EntityDamageItemEvent event = new EntityDamageItemEvent(((LivingEntityBridge) apiOwner).bridge$getBukkitEntity(), CraftItemStack.asCraftMirror(stack), result);
                cir.setReturnValue(event.callEvent() ? event.getDamage() : 0);
            }
            return;
        }
        boolean eventEligible = amount > 0 && stack.isDamageableItem()
            && damager != null && !damager.hasInfiniteMaterials();
        if (damager != null && (apiOwner != null ? result > 0 : eventEligible)) {
            // Forge overwrites argument slot 1 with Item.damageItem's result before returning.
            PlayerItemDamageEvent event = new PlayerItemDamageEvent(((ServerPlayerBridge) damager).bridge$getBukkitEntity(), CraftItemStack.asCraftMirror(stack), result, amount);
            event.getPlayer().getServer().getPluginManager().callEvent(event);

            if (result != event.getDamage() || event.isCancelled()) {
                event.getPlayer().updateInventory();
            }
            if (event.isCancelled()) {
                cir.setReturnValue(0);
                return;
            }
            result = event.getDamage();
        }
        cir.setReturnValue(result);
    }

    @Inject(method = "applyDamage(ILnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"), require = 1, allow = 1)
    private void arclight$itemBreak(int amount, @Nullable ServerPlayer serverPlayer, Consumer<Item> onBroken, CallbackInfo ci) {
        if (this.count == 1 && serverPlayer != null) {
            CraftEventFactory.callPlayerItemBreakEvent(serverPlayer, (ItemStack) (Object) this);
        }
    }
}
