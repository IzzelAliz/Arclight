package io.izzel.arclight.common.mixin.core.entity.item;

import com.google.common.collect.Lists;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.PaintingEntity;
import net.minecraft.entity.item.PaintingType;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PaintingEntity.class)
public abstract class PaintingEntityMixin extends HangingEntityMixin {

    @Shadow public PaintingType art;

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends PaintingEntity> type, World worldIn, CallbackInfo ci) {
        final List<PaintingType> list = Lists.newArrayList(ForgeRegistries.PAINTING_TYPES.getValues());
        this.art = list.get(this.rand.nextInt(list.size()));
    }
}
