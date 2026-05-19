package io.izzel.arclight.common.mixin.core.world.entity.monster.piglin;

import io.izzel.arclight.common.bridge.core.world.entity.monster.piglin.PiglinBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(Piglin.class)
public abstract class PiglinMixin extends PathfinderMobMixin implements PiglinBridge {

    public Set<Item> allowedBarterItems = new HashSet<>();
    public Set<Item> interestItems = new HashSet<>();

    @Override
    public Set<Item> bridge$getAllowedBarterItems() {
        return allowedBarterItems;
    }

    @Override
    public Set<Item> bridge$getInterestItems() {
        return interestItems;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void arclight$writeAdditional(ValueOutput output, CallbackInfo ci) {
        var barterList = output.list("Bukkit.BarterList", Codec.STRING);
        allowedBarterItems.stream().map(BuiltInRegistries.ITEM::getKey).map(Identifier::toString).forEach(barterList::add);
        var interestList = output.list("Bukkit.InterestList", Codec.STRING);
        interestItems.stream().map(BuiltInRegistries.ITEM::getKey).map(Identifier::toString).forEach(interestList::add);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void arclight$readAdditional(ValueInput input, CallbackInfo ci) {
        this.allowedBarterItems = input.listOrEmpty("Bukkit.BarterList", Codec.STRING).stream().map(Identifier::tryParse).map(id -> BuiltInRegistries.ITEM.getValue(id)).collect(Collectors.toCollection(HashSet::new));
        this.interestItems = input.listOrEmpty("Bukkit.InterestList", Codec.STRING).stream().map(Identifier::tryParse).map(id -> BuiltInRegistries.ITEM.getValue(id)).collect(Collectors.toCollection(HashSet::new));
    }

    @Redirect(method = "holdInOffHand", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean arclight$customBarter(ItemStack itemStack, Item item) {
        return itemStack.is(item) || allowedBarterItems.contains(itemStack.getItem());
    }

    @Redirect(method = "canReplaceCurrentItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;isLovedItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean arclight$customLoved(ItemStack stack) {
        return PiglinAi.isLovedItem(stack) || interestItems.contains(stack.getItem()) || allowedBarterItems.contains(stack.getItem());
    }
}
