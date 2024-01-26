package io.izzel.arclight.forge.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Material.class, remap = false)
public abstract class MaterialMixin_Forge implements MaterialBridge {

    @Override
    public int bridge$forge$getMaxStackSize(Item item) {
        return item.getMaxStackSize(new ItemStack(item));
    }

    @Override
    public int bridge$forge$getDurability(Item item) {
        return item.getMaxDamage(new ItemStack(item));
    }

    @Override
    public int bridge$forge$getBurnTime(Item item) {
        return new ItemStack(item).getBurnTime(null);
    }
}
