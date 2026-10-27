package io.izzel.arclight.common.bridge.core.world.entity.animal;

import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import org.jetbrains.annotations.Nullable;

public interface SchoolingFishBridge {

    int bridge$getSchoolSize();

    @Nullable
    AbstractSchoolingFish bridge$getSchoolLeader();
}
