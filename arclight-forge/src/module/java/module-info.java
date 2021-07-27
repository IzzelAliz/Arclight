import io.izzel.arclight.boot.ArclightLocator_Forge;

open module arclight.boot {
    requires net.minecraftforge.forgespi;
    requires cpw.mods.securejarhandler;
    requires jdk.unsupported;
    exports io.izzel.arclight.boot;
    uses net.minecraftforge.forgespi.locating.IModLocator;
    provides net.minecraftforge.forgespi.locating.IModLocator with ArclightLocator_Forge;
}