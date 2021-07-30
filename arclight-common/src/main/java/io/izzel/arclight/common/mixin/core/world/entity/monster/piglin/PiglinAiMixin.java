package io.izzel.arclight.common.mixin.core.world.entity.monster.piglin;

import io.izzel.arclight.common.bridge.core.entity.MobEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.monster.piglin.PiglinBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {

    // @formatter:off
    @Shadow private static void stopWalking(Piglin p_234531_0_) { }
    @Shadow private static ItemStack removeOneItemFromItemEntity(ItemEntity p_234465_0_) { return null; }
    @Shadow private static void holdInOffhand(Piglin p_241427_0_, ItemStack p_241427_1_) { }
    @Shadow private static void admireGoldItem(LivingEntity p_234501_0_) { }
    @Shadow private static boolean hasEatenRecently(Piglin p_234538_0_) { return false; }
    @Shadow private static void eat(Piglin p_234536_0_) { }
    @Shadow private static void putInInventory(Piglin p_234498_0_, ItemStack p_234498_1_) { }
    @Shadow protected static boolean isLovedItem(ItemStack p_149966_) { return false; }
    @Shadow private static boolean isFood(ItemStack p_149970_) { return false; }
    @Shadow private static boolean isBarterCurrency(ItemStack p_149968_) { return false; }
    @Shadow private static List<ItemStack> getBarterResponseItems(Piglin p_34997_) { return null; }
    @Shadow private static void throwItems(Piglin p_34861_, List<ItemStack> p_34862_) { }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected static void pickUpItem(Piglin piglinEntity, ItemEntity itemEntity) {
        ItemStack itemstack;
        stopWalking(piglinEntity);
        if (itemEntity.getItem().getItem() == Items.GOLD_NUGGET && !CraftEventFactory.callEntityPickupItemEvent(piglinEntity, itemEntity, 0, false).isCancelled()) {
            piglinEntity.take(itemEntity, itemEntity.getItem().getCount());
            itemstack = itemEntity.getItem();
            itemEntity.discard();
        } else if (!CraftEventFactory.callEntityPickupItemEvent(piglinEntity, itemEntity, itemEntity.getItem().getCount() - 1, false).isCancelled()) {
            piglinEntity.take(itemEntity, 1);
            itemstack = removeOneItemFromItemEntity(itemEntity);
        } else {
            return;
        }

        if (isLovedByPiglin(itemstack, piglinEntity)) {
            piglinEntity.getBrain().eraseMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
            holdInOffhand(piglinEntity, itemstack);
            admireGoldItem(piglinEntity);
        } else if (isFood(itemstack) && !hasEatenRecently(piglinEntity)) {
            eat(piglinEntity);
        } else {
            ((MobEntityBridge) piglinEntity).bridge$captureItemDrop(itemEntity);
            boolean flag = piglinEntity.equipItemIfPossible(itemstack);
            if (!flag) {
                putInInventory(piglinEntity, itemstack);
            }
        }
    }

    private static boolean isLovedByPiglin(ItemStack itemstack, Piglin piglin) {
        return isLovedItem(itemstack) || (((PiglinBridge) piglin).bridge$getInterestItems().contains(itemstack.getItem())
            || ((PiglinBridge) piglin).bridge$getAllowedBarterItems().contains(itemstack.getItem()));
    }

    private static boolean isBarterItem(ItemStack itemstack, Piglin piglin) {
        return isBarterCurrency(itemstack) || ((PiglinBridge) piglin).bridge$getAllowedBarterItems().contains(itemstack.getItem());
    }

    @Redirect(method = "stopHoldingOffHandItem", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private static boolean arclight$customBarter(ItemStack stack, Piglin piglin) {
        return isBarterItem(stack, piglin);
    }

    @Redirect(method = "stopHoldingOffHandItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;throwItems(Lnet/minecraft/world/entity/monster/piglin/Piglin;Ljava/util/List;)V"))
    private static void arclight$barterEvent(Piglin piglin, List<ItemStack> items) {
        ItemStack stack = piglin.getItemInHand(InteractionHand.OFF_HAND);
        PiglinBarterEvent event = CraftEventFactory.callPiglinBarterEvent(piglin, getBarterResponseItems(piglin), stack);
        if (!event.isCancelled()) {
            throwItems(piglin, event.getOutcome().stream().map(CraftItemStack::asNMSCopy).collect(Collectors.toList()));
        }
    }

    @Redirect(method = "stopHoldingOffHandItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;isLovedItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean arclight$customLove(ItemStack stack, Piglin piglin) {
        return isLovedByPiglin(stack, piglin);
    }

    @Redirect(method = "wantsToPickup", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private static boolean arclight$customBanter2(ItemStack stack, Piglin piglin) {
        return isBarterItem(stack, piglin);
    }

    @Redirect(method = "canAdmire", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private static boolean arclight$customBanter3(ItemStack stack, Piglin piglin) {
        return isBarterItem(stack, piglin);
    }

    @Redirect(method = "isNotHoldingLovedItemInOffHand", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;isLovedItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean arclight$customLove2(ItemStack stack, Piglin piglin) {
        return isLovedByPiglin(stack, piglin);
    }
}
