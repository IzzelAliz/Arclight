package io.izzel.arclight.neoforge.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin_NeoForge {
    @WrapWithCondition(method = "feed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/TamableAnimal;usePlayerItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V", ordinal = 1))
    private boolean arclight$consumeOnlyOnce(TamableAnimal animal, Player player, InteractionHand hand, ItemStack stack) {
        return false;
    }
}
