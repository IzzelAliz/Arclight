package io.izzel.arclight.forge.mixin.core.world.food;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.util.FoodStatsBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(FoodData.class)
public abstract class FoodDataMixin_Forge implements FoodStatsBridge {

    // @formatter:off
    @Shadow public int foodLevel;
    @Shadow public abstract void eat(int i, float f);
    // @formatter:on

    @Redirect(method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V", remap = false,
        at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    private void arclight$foodLevelChangeForge(FoodData foodStats, int foodLevelIn, float foodSaturationModifier, Item maybeFood, ItemStack stack, @Nullable LivingEntity entity) {
        var player = this.bridge$getEntityHuman() != null ? this.bridge$getEntityHuman() : (entity instanceof Player p ? p : null);
        if (player == null) {
            foodStats.eat(foodLevelIn, foodSaturationModifier);
            return;
        } else if (this.bridge$getEntityHuman() == null) {
            this.bridge$setEntityHuman(player);
        }
        FoodProperties food = maybeFood.getFoodProperties(stack, entity);
        int oldFoodLevel = this.foodLevel;
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(player, food.getNutrition() + oldFoodLevel, stack);
        if (!event.isCancelled()) {
            this.eat(event.getFoodLevel() - oldFoodLevel, food.getSaturationModifier());
        }
        ((ServerPlayerEntityBridge) player).bridge$getBukkitEntity().sendHealthUpdate();
    }

    @Override
    public FoodProperties bridge$forge$getFoodProperties(ItemStack stack, LivingEntity entity) {
        return stack.getFoodProperties(entity);
    }
}
