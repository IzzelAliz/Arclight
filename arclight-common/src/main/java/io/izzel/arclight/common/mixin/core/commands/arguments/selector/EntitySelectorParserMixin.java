package io.izzel.arclight.common.mixin.core.commands.arguments.selector;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.izzel.arclight.common.bridge.core.command.arguments.EntitySelectorParserBridge;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntitySelectorParser.class)
public abstract class EntitySelectorParserMixin implements EntitySelectorParserBridge {

    // @formatter:off
    @Shadow private boolean usesSelectors;
    @Shadow protected abstract void shadow$parseSelector() throws CommandSyntaxException;
    @Shadow public abstract EntitySelector parse() throws CommandSyntaxException;
    // @formatter:on

    private Boolean arclight$overridePermissions;

    @Override
    public EntitySelector bridge$parse(boolean overridePermissions) throws CommandSyntaxException {
        return this.parse(overridePermissions);
    }

    public EntitySelector parse(boolean overridePermissions) throws CommandSyntaxException {
        try {
            this.arclight$overridePermissions = overridePermissions;
            return this.parse();
        } finally {
            this.arclight$overridePermissions = null;
        }
    }

    @Override
    public void bridge$parseSelector(boolean overridePermissions) throws CommandSyntaxException {
        this.parseSelector(overridePermissions);
    }

    public void parseSelector(boolean overridePermissions) throws CommandSyntaxException {
        this.usesSelectors = !overridePermissions;
        this.shadow$parseSelector();
    }

    @Inject(method = "parseSelector", at = @At("HEAD"))
    public void arclight$onParserSelector(CallbackInfo ci) {
        if (this.arclight$overridePermissions != null) {
            this.usesSelectors = !this.arclight$overridePermissions;
        }
    }
}
