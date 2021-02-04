package io.izzel.arclight.impl.mixin.optimization.general.activationrange;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.impl.bridge.EntityBridge_ActivationRange;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import org.spigotmc.ActivationRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin_ActivationRange implements EntityBridge_ActivationRange {

    // @formatter:off
    @Shadow public abstract void recalculateSize();
    @Shadow public int ticksExisted;
    @Shadow public abstract void remove();
    @Shadow public World world;
    // @formatter:on

    public ActivationRange.ActivationType activationType;
    public boolean defaultActivationState;
    public long activatedTick = Integer.MIN_VALUE;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<?> entityTypeIn, World worldIn, CallbackInfo ci) {
        activationType = ActivationRange.initializeEntityActivationType((Entity) (Object) this);
        if (worldIn != null) {
            this.defaultActivationState = ActivationRange.initializeEntityActivationState((Entity) (Object) this, ((WorldBridge) worldIn).bridge$spigotConfig());
        } else {
            this.defaultActivationState = false;
        }
    }

    public void inactiveTick() {
    }

    @Override
    public void bridge$inactiveTick() {
        this.inactiveTick();
    }
}
