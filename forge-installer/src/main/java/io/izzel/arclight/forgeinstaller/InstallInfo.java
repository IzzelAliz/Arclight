package io.izzel.arclight.forgeinstaller;

import java.util.Map;

public class InstallInfo {

    public Installer installer;
    public Map<String, String> libraries;

    public static class Installer {

        public String minecraft;
        public String forge;
        public String hash;
    }
}
