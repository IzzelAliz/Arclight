import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import io.izzel.arclight.boot.ArclightImplementer_Forge;
import io.izzel.arclight.boot.ArclightLocator_Forge;
import net.minecraftforge.forgespi.locating.IModLocator;

open module arclight.boot {
    requires net.minecraftforge.forgespi;
    requires cpw.mods.securejarhandler;
    requires cpw.mods.modlauncher;
    requires org.objectweb.asm;
    requires jdk.unsupported;
    requires org.objectweb.asm.tree;

    exports io.izzel.arclight.boot;

    uses IModLocator;
    provides IModLocator with ArclightLocator_Forge;
    uses ILaunchPluginService;
    provides ILaunchPluginService with ArclightImplementer_Forge;
}