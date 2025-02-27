package io.izzel.arclight.gradle.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GitOps {
    public static boolean isGitRepo(Path dir) {
        return Files.exists(dir.resolve(".git"));
    }

    public static List<String> clone(Path dir, String url) {
        return List.of("git", "clone", url, dir.normalize().toString());
    }

    public static List<String> checkout(String refs) {
        return List.of("git", "checkout", "-f", refs);
    }
}
