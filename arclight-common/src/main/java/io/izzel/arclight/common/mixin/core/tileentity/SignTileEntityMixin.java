package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.tileentity.SignTileEntityBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.item.DyeColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.command.CraftBlockCommandSender;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(SignTileEntity.class)
public abstract class SignTileEntityMixin extends TileEntityMixin implements SignTileEntityBridge, ICommandSource, ICommandSourceBridge {

    // @formatter:off
    @Override @Accessor("isEditable") public abstract void bridge$setEditable(boolean editable);
    // @formatter:on

    @Redirect(method = "getCommandSource", at = @At(value = "NEW", target = "net/minecraft/command/CommandSource"))
    private CommandSource arclight$source(ICommandSource source, Vector3d vec3d, Vector2f vec2f, ServerWorld world, int i, String s, ITextComponent component, MinecraftServer server, @Nullable Entity entity) {
        return new CommandSource(this, vec3d, vec2f, world, i, s, component, server, entity);
    }

    @Inject(method = "setTextColor", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;notifyBlockUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;I)V"))
    public void arclight$setColor(DyeColor newColor, CallbackInfoReturnable<Boolean> cir) {
        if (this.world == null) {
            cir.setReturnValue(true);
        }
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
