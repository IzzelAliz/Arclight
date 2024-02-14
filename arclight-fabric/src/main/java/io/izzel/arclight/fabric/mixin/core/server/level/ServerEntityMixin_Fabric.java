package io.izzel.arclight.fabric.mixin.core.server.level;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin_Fabric {

    // @formatter:off
    @Shadow @Final private Entity entity;
    @Shadow private int yHeadRotp;
    @Shadow @Nullable private List<SynchedEntityData.DataValue<?>> trackedDataValues;
    @Shadow @Final private boolean trackDelta;
    @Shadow private Vec3 ap;
    // @formatter:on
}
