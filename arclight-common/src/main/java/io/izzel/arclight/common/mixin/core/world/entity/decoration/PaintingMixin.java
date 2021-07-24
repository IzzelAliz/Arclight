package io.izzel.arclight.common.mixin.core.world.entity.decoration;

import com.google.common.collect.Lists;
import io.izzel.arclight.common.mixin.core.world.entity.item.HangingEntityMixin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.Motive;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Painting.class)
public abstract class PaintingMixin extends HangingEntityMixin {

    @Shadow public Motive motive;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends Painting> type, Level worldIn, CallbackInfo ci) {
        final List<Motive> list = Lists.newArrayList(ForgeRegistries.PAINTING_TYPES.getValues());
        this.motive = list.get(this.random.nextInt(list.size()));
    }
}
