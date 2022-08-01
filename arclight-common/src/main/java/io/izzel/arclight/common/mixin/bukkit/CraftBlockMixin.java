package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import io.izzel.arclight.common.mod.util.ResourceLocationUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraftforge.registries.ForgeRegistries;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftBlock.class, remap = false)
public abstract class CraftBlockMixin {

    // @formatter:off
    @Shadow public abstract Material getType();
    // @formatter:on

    @Inject(method = "getState", cancellable = true, at = @At("HEAD"))
    private void arclight$getState(CallbackInfoReturnable<BlockState> cir) {
        MaterialBridge bridge = (MaterialBridge) (Object) getType();
        if (bridge.bridge$shouldApplyStateFactory()) {
            cir.setReturnValue(bridge.bridge$blockStateFactory().apply((CraftBlock) (Object) this));
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static Biome biomeBaseToBiome(Registry<net.minecraft.world.level.biome.Biome> registry, Holder<net.minecraft.world.level.biome.Biome> base) {
        return Biome.valueOf(ResourceLocationUtil.standardize(ForgeRegistries.BIOMES.getKey(base.value())));
    }
}
