package io.izzel.arclight.common.mixin.optimization.general;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

@Mixin(Entity.class)
public abstract class EntityMixin_Optimize {

    // @formatter:off
    @Shadow @Final public ImmutableList<Entity> passengers;
    // @formatter:on

    /**
     * @author danorris709
     * @reason `getPassengers` instantiates a new instance of the list when called
     */
    @Overwrite
    protected boolean canFitPassenger(Entity passenger) {
        return this.passengers.size() < 1;
    }

    /**
     * @author danorris709
     * @reason `getPassengers` instantiates a new instance of the list when called
     */
    @Overwrite
    public boolean isBeingRidden() {
        return !this.passengers.isEmpty();
    }

    /**
     * @author danorris709
     * @reason `getPassengers` instantiates a new instance of the list when called
     */
    @Overwrite
    public boolean isPassenger(Entity entityIn) {
        for (Entity passenger : this.passengers) {
            if (passenger.equals(entityIn)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @author danorris709
     * @reason `getPassengers` instantiates a new instance of the list when called
     */
    @Overwrite
    public boolean isPassenger(Class<? extends Entity> entityClazz) {
        for (Entity passenger : this.passengers) {
            if (entityClazz.isAssignableFrom(passenger.getClass())) {
                return true;
            }
        }

        return false;
    }

    /**
     * @author danorris709
     * @reason `getPassengers` instantiates a new instance of the list when called
     */
    @Overwrite
    public Collection<Entity> getRecursivePassengers() {
        if (this.passengers.isEmpty()) {
            return ImmutableList.of();
        }

        Set<Entity> set = Sets.newHashSet();

        for (Entity entity : this.passengers) {
            set.add(entity);
            ((EntityAccessor) entity).callGetRecursivePassengers(false, set);
        }

        return set;
    }

    /**
     * @author danorris709
     * @reason `getPassengers` instantiates a new instance of the list when called
     */
    @Overwrite
    private void getRecursivePassengers(boolean playersOnly, Set<Entity> passengers) {
        if (this.passengers.isEmpty()) {
            return;
        }

        for(Entity entity : this.passengers) {
            if (!playersOnly || ServerPlayerEntity.class.isAssignableFrom(entity.getClass())) {
                passengers.add(entity);
            }

            ((EntityAccessor) entity).callGetRecursivePassengers(playersOnly, passengers);
        }
    }

    /**
     * @author danorris709
     * @reason `getPassengers` instantiates a new instance of the list when called
     */
    @Overwrite
    public Stream<Entity> getSelfAndPassengers() {
        if (this.passengers.isEmpty()) {
            return Stream.empty();
        }

        return Stream.concat(Stream.of((Entity) (((Object) this))), this.passengers.stream().flatMap(Entity::getSelfAndPassengers));
    }
}
