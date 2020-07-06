package io.izzel.arclight.common.asm;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.tree.ClassNode;

public interface Implementer {

    boolean processClass(ClassNode node, ILaunchPluginService.ITransformerLoader transformerLoader);
}
