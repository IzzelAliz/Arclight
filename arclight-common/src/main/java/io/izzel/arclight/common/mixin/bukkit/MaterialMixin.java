package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.block.FireBlockBridge;
import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.i18n.conf.MaterialPropertySpec;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.material.MaterialData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;

@Mixin(value = Material.class, remap = false)
public abstract class MaterialMixin implements MaterialBridge {

    // @formatter:off
    @Shadow @Mutable @Final private NamespacedKey key;
    @Shadow @Mutable @Final private Constructor<? extends MaterialData> ctor;
    @Shadow @Mutable @Final public Class<?> data;
    @Shadow public abstract boolean isBlock();
    // @formatter:on

    private MaterialPropertySpec.MaterialType arclight$type = MaterialPropertySpec.MaterialType.VANILLA;
    private MaterialPropertySpec arclight$spec;

    @Inject(method = "isBlock", cancellable = true, at = @At("HEAD"))
    private void arclight$isBlock(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$type != MaterialPropertySpec.MaterialType.VANILLA) {
            cir.setReturnValue(arclight$type == MaterialPropertySpec.MaterialType.FORGE_BLOCK);
        }
    }

    @Inject(method = "isItem", cancellable = true, at = @At("HEAD"))
    private void arclight$isItem(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$type != MaterialPropertySpec.MaterialType.VANILLA) {
            cir.setReturnValue(arclight$type == MaterialPropertySpec.MaterialType.FORGE_ITEM);
        }
    }

    @Inject(method = "isEdible", cancellable = true, at = @At("HEAD"))
    private void arclight$isEdible(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.edible);
        }
    }

    @Inject(method = "isRecord", cancellable = true, at = @At("HEAD"))
    private void arclight$isRecord(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.record);
        }
    }

    @Inject(method = "isSolid", cancellable = true, at = @At("HEAD"))
    private void arclight$isSolid(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.solid);
        }
    }

    @Inject(method = "isAir", cancellable = true, at = @At("HEAD"))
    private void arclight$isAir(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.air);
        }
    }

    @Inject(method = "isTransparent", cancellable = true, at = @At("HEAD"))
    private void arclight$isTransparent(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.transparent);
        }
    }

    @Inject(method = "isFlammable", cancellable = true, at = @At("HEAD"))
    private void arclight$isFlammable(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.flammable);
        }
    }

    @Inject(method = "isBurnable", cancellable = true, at = @At("HEAD"))
    private void arclight$isBurnable(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.burnable);
        }
    }

    @Inject(method = "isFuel", cancellable = true, at = @At("HEAD"))
    private void arclight$isFuel(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.fuel);
        }
    }

    @Inject(method = "isOccluding", cancellable = true, at = @At("HEAD"))
    private void arclight$isOccluding(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.occluding);
        }
    }

    @Inject(method = "hasGravity", cancellable = true, at = @At("HEAD"))
    private void arclight$hasGravity(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.gravity);
        }
    }

    @Inject(method = "isInteractable", cancellable = true, at = @At("HEAD"))
    private void arclight$isInteractable(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.interactable);
        }
    }

    @Inject(method = "getHardness", cancellable = true, at = @At("HEAD"))
    private void arclight$getHardness(CallbackInfoReturnable<Float> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.hardness);
        }
    }

    @Inject(method = "getBlastResistance", cancellable = true, at = @At("HEAD"))
    private void arclight$getBlastResistance(CallbackInfoReturnable<Float> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(arclight$spec.blastResistance);
        }
    }

    @Inject(method = "getCraftingRemainingItem", cancellable = true, at = @At("HEAD"))
    private void arclight$getCraftingRemainingItem(CallbackInfoReturnable<Material> cir) {
        if (arclight$spec != null) {
            cir.setReturnValue(Material.getMaterial(arclight$spec.craftingRemainingItem));
        }
    }

    @Override
    public MaterialPropertySpec bridge$getSpec() {
        return arclight$spec;
    }

    @Override
    public MaterialPropertySpec.MaterialType bridge$getType() {
        return arclight$type;
    }

    @Override
    public void bridge$setupBlock(ResourceLocation key, Block block, MaterialPropertySpec spec) {
        this.arclight$spec = spec.clone();
        arclight$type = MaterialPropertySpec.MaterialType.FORGE_BLOCK;
        arclight$setupCommon(key, block, block.asItem());
    }

    @Override
    public void bridge$setupItem(ResourceLocation key, Item item, MaterialPropertySpec spec) {
        this.arclight$spec = spec.clone();
        arclight$type = MaterialPropertySpec.MaterialType.FORGE_ITEM;
        arclight$setupCommon(key, null, item);
    }

    @SuppressWarnings("unchecked")
    private void arclight$setupCommon(ResourceLocation key, Block block, Item item) {
        this.key = CraftNamespacedKey.fromMinecraft(key);
        if (arclight$spec.materialDataClass != null) {
            try {
                Class<?> data = Class.forName(arclight$spec.materialDataClass);
                if (MaterialData.class.isAssignableFrom(data)) {
                    this.data = data;
                    this.ctor = (Constructor<? extends MaterialData>) data.getConstructor(Material.class, byte.class);
                }
            } catch (Exception e) {
                ArclightMod.LOGGER.warn("Bad material data class {} for {}", arclight$spec.materialDataClass, this);
                ArclightMod.LOGGER.warn(e);
            }
        }
        if (arclight$spec.maxStack == null) {
            arclight$spec.maxStack = tryGetMaxStackSize(item);
        }
        if (arclight$spec.maxDurability == null) {
            arclight$spec.maxDurability = tryGetDurability(item);
        }
        if (arclight$spec.edible == null) {
            arclight$spec.edible = false;
        }
        if (arclight$spec.record == null) {
            arclight$spec.record = false;
        }
        if (arclight$spec.solid == null) {
            arclight$spec.solid = block != null && block.getDefaultState().isSolid();
        }
        if (arclight$spec.air == null) {
            arclight$spec.air = block != null && block.getDefaultState().isAir();
        }
        if (arclight$spec.transparent == null) {
            arclight$spec.transparent = block != null && block.getDefaultState().isTransparent();
        }
        if (arclight$spec.flammable == null) {
            arclight$spec.flammable = block != null && ((FireBlockBridge) Blocks.FIRE).bridge$canBurn(block);
        }
        if (arclight$spec.burnable == null) {
            arclight$spec.burnable = block != null && ((FireBlockBridge) Blocks.FIRE).bridge$canBurn(block);
        }
        if (arclight$spec.fuel == null) {
            arclight$spec.fuel = item != null && new ItemStack(item).getBurnTime() > 0;
        }
        if (arclight$spec.occluding == null) {
            arclight$spec.occluding = arclight$spec.solid;
        }
        if (arclight$spec.gravity == null) {
            arclight$spec.gravity = block instanceof FallingBlock;
        }
        if (arclight$spec.interactable == null) {
            arclight$spec.interactable = true;
        }
        if (arclight$spec.hardness == null) {
            arclight$spec.hardness = block != null ? block.blockHardness : 0;
        }
        if (arclight$spec.blastResistance == null) {
            arclight$spec.blastResistance = block != null ? block.getExplosionResistance() : 0;
        }
    }

    private static int tryGetMaxStackSize(Item item) {
        try {
            return item.getItemStackLimit(new ItemStack(item));
        } catch (Throwable t) {
            try {
                return item.getMaxStackSize();
            } catch (Throwable t1) {
                return 64;
            }
        }
    }

    private static int tryGetDurability(Item item) {
        try {
            return item.getMaxDamage(new ItemStack(item));
        } catch (Throwable t) {
            try {
                return item.getMaxDamage();
            } catch (Throwable t1) {
                return 0;
            }
        }
    }
}
