package io.izzel.arclight.common.mixin.bukkit;

import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;

@Mixin(value = PotionEffectType.class, remap = false)
public class PotionEffectTypeMixin {

    @Shadow @Final private static PotionEffectType[] byId;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @NotNull
    public static PotionEffectType[] values() {
        int from = byId[0] == null ? 1 : 0;
        int to = byId[byId.length - 1] == null ? byId.length - 1 : byId.length;
        return Arrays.copyOfRange(byId, from, to);
    }
}
