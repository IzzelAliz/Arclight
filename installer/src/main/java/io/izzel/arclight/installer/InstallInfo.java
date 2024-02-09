package io.izzel.arclight.installer;

import java.util.Map;

public class InstallInfo {

    public Installer installer;
    public Map<String, String> libraries;

    public static class Installer {

        public String minecraft;
        public String forge;
        public String forgeHash;
        public String neoforge;
        public String neoforgeHash;
    }
}
