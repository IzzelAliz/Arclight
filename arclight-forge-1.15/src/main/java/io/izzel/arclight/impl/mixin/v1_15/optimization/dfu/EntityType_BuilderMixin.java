package io.izzel.arclight.impl.mixin.v1_15.optimization.dfu;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.FMLPlayMessages;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

@Mixin(EntityType.Builder.class)
public class EntityType_BuilderMixin<T extends Entity> {

    @Shadow private boolean serializable;
    @Shadow @Final private EntityType.IFactory<T> factory;
    @Shadow @Final private EntityClassification classification;
    @Shadow private boolean summonable;
    @Shadow private boolean immuneToFire;
    @Shadow private boolean field_225436_f;
    @Shadow private EntitySize size;
    @Shadow private Predicate<EntityType<?>> velocityUpdateSupplier;
    @Shadow private ToIntFunction<EntityType<?>> trackingRangeSupplier;
    @Shadow private ToIntFunction<EntityType<?>> updateIntervalSupplier;
    @Shadow private BiFunction<FMLPlayMessages.SpawnEntity, World, T> customClientFactory;

    /**
     * @author Izzel_Aliz
     * @reason
     */
    @Overwrite
    public EntityType<T> build(String id) {
        return new EntityType<>(this.factory, this.classification, this.serializable, this.summonable, this.immuneToFire, this.field_225436_f, this.size, velocityUpdateSupplier, trackingRangeSupplier, updateIntervalSupplier, customClientFactory);
    }
}
