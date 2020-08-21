package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.inventory.container.LecternContainerBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.LecternContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.LecternTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIntArray;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.command.CraftBlockCommandSender;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(LecternTileEntity.class)
public abstract class LecternTileEntityMixin extends TileEntityMixin implements ICommandSource, ICommandSourceBridge {

    // @formatter:off
    @Shadow @Final public IInventory inventory;
    @Shadow @Final private IIntArray field_214049_b;
    // @formatter:on

    @Redirect(method = "createCommandSource", at = @At(value = "NEW", target = "net/minecraft/command/CommandSource"))
    private CommandSource arclight$source(ICommandSource source, Vector3d vec3d, Vector2f vec2f, ServerWorld world, int i, String s, ITextComponent component, MinecraftServer server, @Nullable Entity entity) {
        return new CommandSource(this, vec3d, vec2f, world, i, s, component, server, entity);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity entity) {
        LecternContainer container = new LecternContainer(i, this.inventory, this.field_214049_b);
        ((LecternContainerBridge) container).bridge$setPlayerInventory(playerInventory);
        return container;
    }

    @Override
    public void sendMessage(@NotNull ITextComponent component, @NotNull UUID uuid) {
    }

    @Override
    public boolean shouldReceiveFeedback() {
        return false;
    }

    @Override
    public boolean shouldReceiveErrors() {
        return false;
    }

    @Override
    public boolean allowLogging() {
        return false;
    }

    public CommandSender getBukkitSender(CommandSource wrapper) {
        return wrapper.getEntity() != null ? ((EntityBridge) wrapper.getEntity()).bridge$getBukkitSender(wrapper) : new CraftBlockCommandSender(wrapper, (TileEntity) (Object) this);
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSource wrapper) {
        return getBukkitSender(wrapper);
    }
}
