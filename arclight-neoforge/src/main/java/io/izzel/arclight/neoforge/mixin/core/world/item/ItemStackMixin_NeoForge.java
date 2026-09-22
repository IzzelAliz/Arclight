package io.izzel.arclight.neoforge.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin_NeoForge implements ItemStackBridge, IItemStackExtension {

    // @formatter:off
    @Mutable @Shadow @Deprecated @Nullable private Item item;
    @Shadow private int count;
    // @formatter:on

    @WrapOperation(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int arclight$itemDamage(ItemStack stack, int amount, ServerLevel level, LivingEntity damager, Operation<Integer> original) {
        // Preserve the existing event gate, but do not eventize hurtWithoutBreaking.
        boolean eventEligible = amount > 0 && stack.isDamageableItem()
            && damager != null && !damager.hasInfiniteMaterials();
        int result = original.call(stack, amount, level, damager);
        if (eventEligible && damager instanceof ServerPlayer) {
            PlayerItemDamageEvent event = new PlayerItemDamageEvent(((ServerPlayerBridge) damager).bridge$getBukkitEntity(), CraftItemStack.asCraftMirror(stack), result);
            event.getPlayer().getServer().getPluginManager().callEvent(event);
            if (result != event.getDamage() || event.isCancelled()) {
                event.getPlayer().updateInventory();
            }
            if (event.isCancelled()) {
                return 0;
            }
            result = event.getDamage();
        }
        return result;
    }

    @Inject(method = "applyDamage(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void arclight$itemBreak(int damage, @org.jetbrains.annotations.Nullable LivingEntity livingEntity, Consumer<Item> onBroken, CallbackInfo ci) {
        if (this.count == 1 && livingEntity instanceof ServerPlayer serverPlayer) {
            CraftEventFactory.callPlayerItemBreakEvent(serverPlayer, (ItemStack) (Object) this);
        }
    }

    @Deprecated
    public void setItem(@Nullable Item item) {
        if (item == null) { return;
        }
        this.item = item;
    }

    @Override
    public InteractionResult bridge$forge$onItemUseFirst(UseOnContext context) {
        return onItemUseFirst(context);
    }

    @Override
    public boolean bridge$forge$doesSneakBypassUse(LevelReader level, BlockPos pos, Player player) {
        return doesSneakBypassUse(level, pos, player);
    }
}
