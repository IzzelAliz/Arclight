package io.izzel.arclight.mixin.core.tileentity;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_14_R1.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.v1_14_R1.persistence.CraftPersistentDataTypeRegistry;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.izzel.arclight.bridge.tileentity.TileEntityBridge;

import javax.annotation.Nullable;

@Mixin(TileEntity.class)
public abstract class TileEntityMixin implements TileEntityBridge {

    private static final CraftPersistentDataTypeRegistry DATA_TYPE_REGISTRY = new CraftPersistentDataTypeRegistry();
    public CraftPersistentDataContainer persistentDataContainer;

    // @formatter:off
    @Shadow @Nullable protected World world;
    @Shadow protected BlockPos pos;
    @Shadow public abstract BlockState getBlockState();
    // @formatter:on

    @Inject(method = "read", at = @At("RETURN"))
    public void arclight$loadPersistent(CompoundNBT compound, CallbackInfo ci) {
        this.persistentDataContainer = new CraftPersistentDataContainer(DATA_TYPE_REGISTRY);

        CompoundNBT persistentDataTag = compound.getCompound("PublicBukkitValues");
        if (persistentDataTag != null) {
            this.persistentDataContainer.putAll(persistentDataTag);
        }
    }

    @Inject(method = "writeInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundNBT;putString(Ljava/lang/String;Ljava/lang/String;)V"))
    public void arclight$savePersistent(CompoundNBT compound, CallbackInfoReturnable<CompoundNBT> cir) {
        if (this.persistentDataContainer != null && !this.persistentDataContainer.isEmpty()) {
            compound.put("PublicBukkitValues", this.persistentDataContainer.toTagCompound());
        }
    }

    public InventoryHolder getOwner() {
        if (this.world == null) return null;
        org.bukkit.block.Block block = CraftBlock.at(this.world, this.pos);
        if (block == null) {
            org.bukkit.Bukkit.getLogger().log(java.util.logging.Level.WARNING, "No block for owner at %s %d %d %d", new Object[]{world.getWorld(), pos.getX(), pos.getY(), pos.getZ()});
            return null;
        }
        org.bukkit.block.BlockState state = block.getState();
        if (state instanceof InventoryHolder) return (InventoryHolder) state;
        return null;
    }

    @Override
    public InventoryHolder bridge$getOwner() {
        return getOwner();
    }
}
