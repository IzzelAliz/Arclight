package io.izzel.arclight.common.mixin.vanilla.world.item;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin_Vanilla {

    // @formatter:off
    @Shadow private int count;
    @Shadow public abstract boolean canPlaceOnBlockInAdventureMode(BlockInWorld blockInWorld);
    @Shadow public abstract Item getItem();
    @Shadow public abstract ItemStack copy();
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public InteractionResult useOn(UseOnContext useOnContext) {
        Player player = useOnContext.getPlayer();
        BlockPos blockPos = useOnContext.getClickedPos();
        if (player != null && !player.getAbilities().mayBuild && !this.canPlaceOnBlockInAdventureMode(new BlockInWorld(useOnContext.getLevel(), blockPos, false))) {
            return InteractionResult.PASS;
        } else {
            Item item = this.getItem();
            InteractionResult interactionResult;
            ItemStack oldStack = this.copy();
            if (!(item instanceof BucketItem || item instanceof SolidBucketItem)) {
                ((WorldBridge) useOnContext.getLevel()).bridge$platform$startCaptureBlockBreak();
            }
            interactionResult = item.useOn(useOnContext);
            ((WorldBridge) useOnContext.getLevel()).bridge$platform$endCaptureBlockBreak();
            if (player != null && interactionResult.indicateItemUse()) {
                interactionResult = ArclightEventFactory.onBlockPlace(useOnContext, player, oldStack, (ItemStack) (Object) this, interactionResult);
                if (interactionResult != InteractionResult.FAIL) {
                    player.awardStat(Stats.ITEM_USED.get(item));
                }
            }

            ((WorldBridge) useOnContext.getLevel()).bridge$getCapturedBlockEntity().clear();
            ((WorldBridge) useOnContext.getLevel()).bridge$getCapturedBlockState().clear();
            return interactionResult;
        }
    }

    @Decorate(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"))
    private int arclight$itemDamage(ServerLevel serverLevel, ItemStack itemStack, int i, @Local(ordinal = 0) ServerPlayer damager) throws Throwable {
        int result = (int) DecorationOps.callsite().invoke(serverLevel, itemStack, i);
        if (damager != null) {
            PlayerItemDamageEvent event = new PlayerItemDamageEvent(((ServerPlayerBridge) damager).bridge$getBukkitEntity(), CraftItemStack.asCraftMirror((ItemStack) (Object) this), result);
            event.getPlayer().getServer().getPluginManager().callEvent(event);

            if (result != event.getDamage() || event.isCancelled()) {
                event.getPlayer().updateInventory();
            }
            if (event.isCancelled()) {
                return (int) DecorationOps.cancel().invoke();
            }
            result = event.getDamage();
        }
        return result;
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V", require = 0, at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private void arclight$itemBreak(int amount, ServerLevel level, @Nullable ServerPlayer serverPlayer, Consumer<Item> onBroken, CallbackInfo ci) {
        if (this.count == 1 && serverPlayer != null) {
            CraftEventFactory.callPlayerItemBreakEvent(serverPlayer, (ItemStack) (Object) this);
        }
    }
}
