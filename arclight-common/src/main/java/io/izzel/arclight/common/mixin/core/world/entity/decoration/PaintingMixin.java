package io.izzel.arclight.common.mixin.core.world.entity.decoration;

import io.izzel.arclight.common.mixin.core.world.entity.decoration.BlockAttachedEntityMixin;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Painting.class)
public abstract class PaintingMixin extends BlockAttachedEntityMixin {
    @Shadow
    public abstract Holder<PaintingVariant> getVariant();

    @Inject(method = "calculateBoundingBox", at = @At("HEAD"), cancellable = true)
    private void arclight$calculateBoundingBox(BlockPos blockPos, Direction direction, CallbackInfoReturnable<AABB> cir) {
        var variant = getVariant().value();
        cir.setReturnValue(calculateBoundingBoxStatic(blockPos, direction, variant.width(), variant.height()));
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static AABB calculateBoundingBoxStatic(BlockPos blockPos, Direction enumdirection, int width, int height) {
        // CraftBukkit end
        float f = 0.46875F;
        Vec3 vec3d = Vec3.atCenterOf(blockPos).relative(enumdirection, -0.46875D);
        // CraftBukkit start
        double d0 = offsetForPaintingSize0(width);
        double d1 = offsetForPaintingSize0(height);
        // CraftBukkit end
        Direction enumdirection1 = enumdirection.getCounterClockWise();
        Vec3 vec3d1 = vec3d.relative(enumdirection1, d0).relative(Direction.UP, d1);
        Direction.Axis enumdirection_enumaxis = enumdirection.getAxis();
        // CraftBukkit start
        double d2 = enumdirection_enumaxis == Direction.Axis.X ? 0.0625D : (double) width;
        double d3 = height;
        double d4 = enumdirection_enumaxis == Direction.Axis.Z ? 0.0625D : (double) width;
        // CraftBukkit end

        return AABB.ofSize(vec3d1, d2, d3, d4);
    }

    private static double offsetForPaintingSize0(int i) {
        return i % 2 == 0 ? 0.5D : 0.0D;
    }
}
