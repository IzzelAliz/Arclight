package io.izzel.arclight.common.bridge.command.arguments;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.arguments.EntitySelector;

public interface EntitySelectorParserBridge {

    EntitySelector bridge$parse(boolean overridePermissions) throws CommandSyntaxException;

    void bridge$parseSelector(boolean overridePermissions) throws CommandSyntaxException;
}
