package io.izzel.arclight.common.bridge.core.world.entity.animal;

import javax.annotation.Nullable;

/** Invocation-scoped experience supplied by the egg-fertilization caller. */
public interface AnimalFertilizeEggBridge {

    @Nullable Integer bridge$setFertilizeEggExperience(@Nullable Integer experience);
}
