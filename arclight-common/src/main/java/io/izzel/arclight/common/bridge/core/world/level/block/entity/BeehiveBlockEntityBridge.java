package io.izzel.arclight.common.bridge.core.world.level.block.entity;

public interface BeehiveBlockEntityBridge {
    int bridge$getMaxBees();
    void bridge$setMaxBees(int max);
    void bridge$clearBees();
    java.util.List<net.minecraft.world.entity.Entity> releaseBees(net.minecraft.world.level.block.state.BlockState state,
        net.minecraft.world.level.block.entity.BeehiveBlockEntity.BeeReleaseStatus status, boolean force);

    default void clearBees() {
        this.bridge$clearBees();
    }

}
