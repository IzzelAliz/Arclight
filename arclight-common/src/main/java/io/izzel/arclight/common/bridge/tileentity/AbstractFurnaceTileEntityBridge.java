package io.izzel.arclight.common.bridge.tileentity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.List;

public interface AbstractFurnaceTileEntityBridge {

    List<IRecipe<?>> bridge$dropExp(World world, Vector3d pos, PlayerEntity entity, ItemStack itemStack, int amount);
}
