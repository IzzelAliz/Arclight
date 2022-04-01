package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.world.entity.MobCategory;
import org.bukkit.craftbukkit.v.util.CraftSpawnCategory;
import org.bukkit.entity.SpawnCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftSpawnCategory.class, remap = false)
public class CraftSpawnCategoryMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static boolean isValidForLimits(SpawnCategory spawnCategory) {
        return spawnCategory != null && spawnCategory.ordinal() < SpawnCategory.MISC.ordinal();
    }

    @Inject(method = "toBukkit", cancellable = true, at = @At(value = "NEW", target = "java/lang/UnsupportedOperationException"))
    private static void arclight$modToBukkit(MobCategory mobCategory, CallbackInfoReturnable<SpawnCategory> cir) {
        cir.setReturnValue(SpawnCategory.valueOf(mobCategory.name()));
    }

    @Inject(method = "toNMS", cancellable = true, at = @At(value = "NEW", target = "java/lang/UnsupportedOperationException"))
    private static void arclight$bukkitToMod(SpawnCategory spawnCategory, CallbackInfoReturnable<MobCategory> cir) {
        cir.setReturnValue(MobCategory.valueOf(spawnCategory.name()));
    }
}
