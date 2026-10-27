package io.izzel.arclight.forge.mixin.core.world.entity.animal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Fox.class)
public abstract class FoxMixin_Forge extends Animal {
    private FoxMixin_Forge(EntityType<? extends Animal> arg, Level arg2) {
        super(arg, arg2);
    }

    /**
     * @author InitAuther97
     * @reason Move the mouth item into Forge's death-loot capture in dropEquipment
     */
    @Overwrite
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        super.dropAllDeathLoot(level, source);
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        ItemStack itemstack = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!itemstack.isEmpty()) {
            this.spawnAtLocation(level, itemstack);
            ((io.izzel.arclight.common.bridge.core.world.entity.DeathEventBridge) this)
                .bridge$deferDeathChange(() -> this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY));
        }
    }
}
