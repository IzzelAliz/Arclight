package io.izzel.arclight.installer;

import java.util.HashMap;
import java.util.Map;

public class InstallInfo {

    public Installer installer;
    public Map<String, String> libraries;
    public Map<String, String> fabricExtra;

    public static class Installer {

        public String minecraft;
        public String forge;
        public String forgeHash;
        public String neoforge;
        public String neoforgeHash;
        public String fabricLoader;
        public String fabricLoaderHash;
    }

    public Map<String, String> fabricDeps() {
        var map = new HashMap<>(this.libraries);
        map.putAll(this.fabricExtra);
        return map;
    }
}
