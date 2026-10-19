package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.mod.util.FrameBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.bukkit.craftbukkit.entity.CraftEntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftEntityTypes.class, remap = false)
public abstract class CraftEntityTypesMixin {
    // Fixed CB caller is static; native ItemFrame owns a private instance method of this descriptor.
    @Redirect(method = "lambda$createHanging$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;createBoundingBox(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Lnet/minecraft/world/phys/AABB;"))
    private static AABB arclight$frameBounds(BlockPos pos, Direction direction, boolean hasMap) {
        return FrameBounds.create(pos, direction, hasMap);
    }
}
