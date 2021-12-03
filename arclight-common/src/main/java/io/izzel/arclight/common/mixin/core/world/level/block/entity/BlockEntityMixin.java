package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.tileentity.TileEntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataTypeRegistry;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements TileEntityBridge {

    private static final CraftPersistentDataTypeRegistry DATA_TYPE_REGISTRY = new CraftPersistentDataTypeRegistry();
    public CraftPersistentDataContainer persistentDataContainer;

    // @formatter:off
    @Shadow @Nullable public Level level;
    @Shadow @Final protected BlockPos worldPosition;
    @Shadow public abstract BlockState getBlockState();
    @Shadow public abstract void setChanged();
    @Shadow public BlockPos getBlockPos() { return null; }
    @Shadow public abstract boolean onlyOpCanSetNbt();
    @Shadow protected static void setChanged(Level p_155233_, BlockPos p_155234_, BlockState p_155235_) { }
    // @formatter:on

    @Inject(method = "load", at = @At("RETURN"))
    public void arclight$loadPersistent(CompoundTag compound, CallbackInfo ci) {
        this.persistentDataContainer = new CraftPersistentDataContainer(DATA_TYPE_REGISTRY);

        CompoundTag persistentDataTag = compound.getCompound("PublicBukkitValues");
        if (persistentDataTag != null) {
            this.persistentDataContainer.putAll(persistentDataTag);
        }
    }

    @Inject(method = "saveWithoutMetadata", at = @At("RETURN"))
    private void arclight$savePersistent(CallbackInfoReturnable<CompoundTag> cir) {
        if (this.persistentDataContainer != null && !this.persistentDataContainer.isEmpty()) {
            cir.getReturnValue().put("PublicBukkitValues", this.persistentDataContainer.toTagCompound());
        }
    }

    public InventoryHolder getOwner() {
        if (this.level == null) return null;
        org.bukkit.block.Block block = CraftBlock.at(this.level, this.worldPosition);
        org.bukkit.block.BlockState state = block.getState();
        if (state instanceof InventoryHolder) return (InventoryHolder) state;
        return null;
    }

    @Override
    public InventoryHolder bridge$getOwner() {
        return getOwner();
    }
}
