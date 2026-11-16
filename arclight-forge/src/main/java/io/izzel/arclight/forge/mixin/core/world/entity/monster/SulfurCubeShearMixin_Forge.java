package io.izzel.arclight.forge.mixin.core.world.entity.monster;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(SulfurCube.class)
public abstract class SulfurCubeShearMixin_Forge implements io.izzel.arclight.common.bridge.core.world.entity.ShearableBridge {

    @Unique private List<ItemStack> arclight$shearDrops;

    @Override
    public List<ItemStack> bridge$swapShearDrops(List<ItemStack> drops) {
        List<ItemStack> previous = this.arclight$shearDrops;
        this.arclight$shearDrops = drops;
        return previous;
    }

    @Decorate(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"), require = 1, allow = 1)
    private void arclight$playerShear(SulfurCube cube, ServerLevel level, SoundSource source,
                                    ItemStack tool, Player player, InteractionHand hand) throws Throwable {
        List<ItemStack> defaults = new ArrayList<>(1);
        defaults.add(cube.getItemBySlot(EquipmentSlot.BODY).copy());
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, cube, tool, hand, defaults);
        if (event != null && event.isCancelled()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.CONSUME);
            return;
        }
        List<ItemStack> previous = this.arclight$shearDrops;
        this.arclight$shearDrops = event == null ? defaults : CraftItemStack.asNMSCopy(event.getDrops());
        try {
            DecorationOps.callsite().invoke(cube, level, source, tool);
        } finally {
            this.arclight$shearDrops = previous;
        }
    }

    @WrapOperation(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;"), require = 1, allow = 1)
    private ItemEntity arclight$dropShearedItems(SulfurCube cube, ServerLevel level, ItemStack stack,
                                               Vec3 offset, Operation<ItemEntity> original) {
        List<ItemStack> drops = this.arclight$shearDrops;
        if (drops == null) {
            return original.call(cube, level, stack, offset);
        }
        this.arclight$shearDrops = List.of();
        var bridge = (EntityBridge) cube;
        boolean previous = bridge.bridge$isForceDrops();
        bridge.bridge$setForceDrops(true);
        try {
            ItemEntity result = null;
            for (ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    result = original.call(cube, level, drop, offset);
                }
            }
            return result;
        } finally {
            bridge.bridge$setForceDrops(previous);
        }
    }
}
