package io.izzel.arclight.common.mixin.core.world.entity.monster.piglin;

import io.izzel.arclight.common.bridge.core.entity.monster.piglin.PiglinBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
    private void arclight$writeAdditional(CompoundTag compound, CallbackInfo ci) {
        ListTag barterList = new ListTag();
        allowedBarterItems.stream().map(Registry.ITEM::getKey).map(ResourceLocation::toString).map(StringTag::valueOf).forEach(barterList::add);
        compound.put("Bukkit.BarterList", barterList);
        ListTag interestList = new ListTag();
        interestItems.stream().map(Registry.ITEM::getKey).map(ResourceLocation::toString).map(StringTag::valueOf).forEach(interestList::add);
        compound.put("Bukkit.InterestList", interestList);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void arclight$readAdditional(CompoundTag compound, CallbackInfo ci) {
        this.allowedBarterItems = compound.getList("Bukkit.BarterList", 8).stream().map(Tag::getAsString).map(ResourceLocation::tryParse).map(Registry.ITEM::get).collect(Collectors.toCollection(HashSet::new));
        this.interestItems = compound.getList("Bukkit.InterestList", 8).stream().map(Tag::getAsString).map(ResourceLocation::tryParse).map(Registry.ITEM::get).collect(Collectors.toCollection(HashSet::new));
    }

    @Redirect(method = "holdInOffHand", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private boolean arclight$customBarter(ItemStack itemStack) {
        return itemStack.isPiglinCurrency() || allowedBarterItems.contains(itemStack.getItem());
    }

    @Redirect(method = "canReplaceCurrentItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;isLovedItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean arclight$customLoved(ItemStack stack) {
        return PiglinAi.isLovedItem(stack) || interestItems.contains(stack.getItem()) || allowedBarterItems.contains(stack.getItem());
    }
}
