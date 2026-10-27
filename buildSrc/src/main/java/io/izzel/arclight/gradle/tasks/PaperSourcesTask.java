package io.izzel.arclight.gradle.tasks;

import com.cloudbees.diff.ContextualPatch;
import com.cloudbees.diff.Diff;
import com.google.gson.Gson;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.Patch;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FS;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

public abstract class PaperSourcesTask extends DefaultTask {
    private File sourceRoot;
    private String operation;
    private String selectedGroup = "";

    @Internal public File getSourceRoot() { return sourceRoot; }
    public void setSourceRoot(File root) { sourceRoot = root; }
    @Input public String getOperation() { return operation; }
    public void setOperation(String value) { operation = value; }
    @Input public String getSelectedGroup() { return selectedGroup; }
    public void setSelectedGroup(String value) { selectedGroup = value; }

    private record Group(List<String> roots, List<String> exclude) { }
    private record Manifest(String commit, Map<String, Group> groups) { }
    private record Content(String hash, boolean executable) { }
    private record SourcePatch(Path file, String path, FileHeader header) { }
    @FunctionalInterface private interface Work { void run(Path directory) throws Exception; }

    private static Repository open(Path directory) throws Exception {
        // JGit otherwise discovers system configuration by invoking native Git.
        FS.DETECTED.setGitSystemConfig(null);
        return new FileRepositoryBuilder().setWorkTree(directory.toFile()).build();
    }

    private static void safe(String name) {
        if (name.isEmpty() || name.startsWith("/") || name.contains("\\") || name.contains(":")) {
            throw new GradleException("Unsafe source path: " + name);
        }
        for (String part : name.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..") || part.equalsIgnoreCase(".git")) {
                throw new GradleException("Unsafe source path: " + name);
            }
        }
    }

    private static boolean selected(String name, Group group) {
        return group.roots.stream().anyMatch(root -> name.equals(root) || name.startsWith(root + "/"));
    }

    private static boolean excluded(String name, Group group) {
        if (group.exclude == null) return false;
        return group.exclude.stream().anyMatch(glob -> {
            StringBuilder regex = new StringBuilder();
            for (char c : glob.toCharArray()) regex.append(c == '*' ? ".*" : c == '?' ? "." : Pattern.quote(String.valueOf(c)));
            return name.matches(regex.toString());
        });
    }

    private static void allowed(String name, Group group) {
        safe(name);
        if (!selected(name, group) || excluded(name, group)) throw new GradleException("Source outside selected roots: " + name);
    }

    private void verify(Repository upstream, Manifest manifest) throws Exception {
        try (Repository root = open(sourceRoot.toPath())) {
            var entry = root.readDirCache().getEntry("upstream/Paper");
            if (entry == null || !entry.getFileMode().equals(FileMode.GITLINK) || !entry.getObjectId().name().equals(manifest.commit)) {
                throw new GradleException("Paper gitlink does not match the pinned source manifest");
            }
        }
        var head = upstream.resolve(Constants.HEAD);
        if (head == null) throw new GradleException("Paper submodule is not initialized; initialize upstream/Paper before building");
        if (!manifest.commit.equals(head.name())) throw new GradleException("Paper revision does not match the pinned source manifest");
        if (!new Git(upstream).status().call().isClean()) throw new GradleException("Paper upstream is dirty");
    }

    private static void export(Repository upstream, String commit, Group group, Path output) throws Exception {
        try (RevWalk revisions = new RevWalk(upstream); TreeWalk tree = new TreeWalk(upstream)) {
            tree.addTree(revisions.parseCommit(upstream.resolve(commit)).getTree());
            tree.setRecursive(true);
            while (tree.next()) {
                String name = tree.getPathString();
                if (!selected(name, group) || excluded(name, group)) continue;
                safe(name);
                FileMode mode = tree.getFileMode(0);
                if (!mode.equals(FileMode.REGULAR_FILE) && !mode.equals(FileMode.EXECUTABLE_FILE)) throw new GradleException("Unsupported source mode: " + name);
                Path file = output.resolve(name);
                Files.createDirectories(file.getParent());
                try (var stream = Files.newOutputStream(file)) { upstream.open(tree.getObjectId(0)).copyTo(stream); }
                executable(file, mode.equals(FileMode.EXECUTABLE_FILE));
            }
        }
    }

    private static void executable(Path path, boolean value) {
        if (!path.toFile().setExecutable(value, false)) throw new GradleException("Cannot set source permissions: " + path);
    }

    private static Map<String, Content> snapshot(Path directory, Group group) throws Exception {
        Map<String, Content> result = new TreeMap<>();
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.toList()) {
                String name = directory.relativize(path).toString().replace(File.separatorChar, '/');
                if (name.equals(".git") || name.startsWith(".git/")) continue;
                if (Files.isSymbolicLink(path)) throw new GradleException("Source symlink: " + path);
                if (!Files.isRegularFile(path)) continue;
                allowed(name, group);
                result.put(name, new Content(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))), Files.isExecutable(path)));
            }
        }
        return result;
    }

    private static List<SourcePatch> patches(Path directory, Group group) throws Exception {
        if (!Files.isDirectory(directory) || Files.isSymbolicLink(directory) || Files.exists(directory.resolve("series"))) throw new GradleException("Expected per-file patch directory: " + directory);
        List<SourcePatch> result = new ArrayList<>();
        try (var paths = Files.walk(directory)) {
            for (Path file : paths.sorted().toList()) {
                if (Files.isSymbolicLink(file)) throw new GradleException("Patch symlink: " + file);
                if (!Files.isRegularFile(file) || !file.toString().endsWith(".patch")) continue;
                String name = directory.relativize(file).toString().replace(File.separatorChar, '/');
                name = name.substring(0, name.length() - 6);
                allowed(name, group);
                Patch parsed = new Patch();
                try (var in = Files.newInputStream(file)) { parsed.parse(in); }
                if (!parsed.getErrors().isEmpty() || parsed.getFiles().size() != 1) throw new GradleException("Invalid single-file patch: " + file);
                FileHeader header = parsed.getFiles().getFirst();
                for (String target : List.of(header.getOldPath(), header.getNewPath())) {
                    if (!target.equals(DiffEntry.DEV_NULL) && !target.equals(name)) throw new GradleException("Patch path mismatch: " + file);
                }
                if (header.getPatchType() != FileHeader.PatchType.UNIFIED || header.getChangeType() == DiffEntry.ChangeType.RENAME || header.getChangeType() == DiffEntry.ChangeType.COPY) throw new GradleException("Unsupported patch format: " + file);
                for (FileMode mode : new FileMode[] {header.getOldMode(), header.getNewMode()}) {
                    if (mode != null && !mode.equals(FileMode.MISSING) && !mode.equals(FileMode.REGULAR_FILE) && !mode.equals(FileMode.EXECUTABLE_FILE)) throw new GradleException("Unsupported patch mode: " + file);
                }
                result.add(new SourcePatch(file, name, header));
            }
        }
        return result;
    }

    private void applyOne(SourcePatch patch, Path output) throws Exception {
        Path target = output.resolve(patch.path);
        boolean add = patch.header.getChangeType() == DiffEntry.ChangeType.ADD;
        boolean delete = patch.header.getChangeType() == DiffEntry.ChangeType.DELETE;
        if (add == Files.exists(target)) throw new GradleException("Unexpected patch target existence: " + patch.path);
        temporary(getTemporaryDir().toPath(), tmp -> {
            // A fixed existing file prevents diff4j's parent-directory context search escaping staging.
            Path content = tmp.resolve("content");
            Files.write(content, add ? new byte[0] : Files.readAllBytes(target));
            if (!patch.header.getHunks().isEmpty()) {
                String raw = Files.readString(patch.file);
                int begin = raw.indexOf("--- ");
                if (begin < 0) throw new GradleException("Missing unified headers: " + patch.file);
                Path normalized = tmp.resolve("change.patch");
                Files.writeString(normalized, ContextualPatch.MAGIC + "\n" + raw.substring(begin));
                var reports = ContextualPatch.create(normalized.toFile(), content.toFile()).patch(false);
                if (reports.size() != 1 || reports.getFirst().getStatus() != ContextualPatch.PatchStatus.Patched || !reports.getFirst().getFile().equals(content.toFile())) {
                    throw new GradleException("Cannot apply " + patch.file, reports.isEmpty() ? null : reports.getFirst().getFailure());
                }
            }
            if (delete) Files.delete(target);
            else {
                boolean mode = (patch.header.getNewMode() == null || patch.header.getNewMode().equals(FileMode.MISSING)) ? (!add && Files.isExecutable(target)) : patch.header.getNewMode().equals(FileMode.EXECUTABLE_FILE);
                Files.createDirectories(target.getParent());
                Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
                executable(target, mode);
            }
        });
    }

    private void replay(Repository upstream, Manifest manifest, Group group, Path directory, Path output) throws Exception {
        Files.createDirectories(output);
        export(upstream, manifest.commit, group, output);
        for (SourcePatch patch : patches(directory, group)) applyOne(patch, output);
        snapshot(output, group);
    }

    private static void remove(Path path) throws Exception {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        try (var paths = Files.walk(path)) {
            for (Path child : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(child);
        }
    }

    private static void temporary(Path parent, Work work) throws Exception {
        Files.createDirectories(parent);
        Path tmp = Files.createTempDirectory(parent, ".paper-stage-");
        try { work.run(tmp); } finally { remove(tmp); }
    }

    private static void publish(Path staged, Path output, Path tmp) throws Exception {
        if (Files.isSymbolicLink(output)) throw new GradleException("Refusing to replace symlink: " + output);
        Path previous = tmp.resolve("previous");
        if (Files.exists(output)) Files.move(output, previous);
        try { Files.move(staged, output); }
        catch (Exception failure) {
            if (Files.exists(previous)) Files.move(previous, output);
            throw failure;
        }
    }

    private void check(Repository upstream, Manifest manifest, Group group, Path directory, Path work) throws Exception {
        temporary(getTemporaryDir().toPath(), tmp -> {
            Path replay = tmp.resolve("replay");
            replay(upstream, manifest, group, directory, replay);
            Path comparison = work;
            if (!Files.exists(work)) {
                comparison = tmp.resolve("second");
                replay(upstream, manifest, group, directory, comparison);
            }
            if (!snapshot(replay, group).equals(snapshot(comparison, group))) throw new GradleException("Patch replay differs from editing directory: " + work);
        });
    }

    private void rebuild(Repository upstream, Manifest manifest, Group group, Path directory, Path work) throws Exception {
        Map<String, Content> edited = snapshot(work, group);
        temporary(directory.getParent(), tmp -> {
            Path baseline = tmp.resolve("baseline");
            Path previousReplay = tmp.resolve("replay");
            Path generated = tmp.resolve("patches");
            Files.createDirectories(baseline);
            Files.createDirectories(generated);
            export(upstream, manifest.commit, group, baseline);
            replay(upstream, manifest, group, directory, previousReplay);
            Map<String, Content> original = snapshot(baseline, group);
            Map<String, Content> previous = snapshot(previousReplay, group);
            var names = new TreeSet<>(original.keySet());
            names.addAll(edited.keySet());
            for (String name : names) {
                if (java.util.Objects.equals(original.get(name), edited.get(name))) continue;
                Path patch = generated.resolve(name + ".patch");
                Files.createDirectories(patch.getParent());
                Path existing = directory.resolve(name + ".patch");
                if (Files.isRegularFile(existing) && java.util.Objects.equals(previous.get(name), edited.get(name))) {
                    Files.copy(existing, patch);
                    continue;
                }
                boolean added = !original.containsKey(name), deleted = !edited.containsKey(name);
                String before = added ? "" : Files.readString(baseline.resolve(name));
                String after = deleted ? "" : Files.readString(work.resolve(name));
                if (before.indexOf('\0') >= 0 || after.indexOf('\0') >= 0) throw new GradleException("Binary patch is not supported: " + name);
                Diff diff = Diff.diff(new StringReader(before), new StringReader(after), false);
                String oldMode = !added && original.get(name).executable ? "100755" : "100644";
                String newMode = !deleted && edited.get(name).executable ? "100755" : "100644";
                String header = "diff --git a/" + name + " b/" + name + "\n";
                if (added) header += "new file mode " + newMode + "\n";
                else if (deleted) header += "deleted file mode " + oldMode + "\n";
                else if (!oldMode.equals(newMode)) header += "old mode " + oldMode + "\nnew mode " + newMode + "\n";
                String unified = diff.toUnifiedDiff(added ? "/dev/null" : "a/" + name, deleted ? "/dev/null" : "b/" + name, new StringReader(before), new StringReader(after), 3);
                Files.writeString(patch, header + unified, StandardCharsets.UTF_8);
            }
            check(upstream, manifest, group, generated, work);
            publish(generated, directory, tmp);
        });
    }

    @TaskAction
    public void runSources() throws Exception {
        Path root = sourceRoot.toPath();
        Manifest manifest = new Gson().fromJson(Files.readString(root.resolve("build-support/paper-sources.json")), Manifest.class);
        if (!selectedGroup.isEmpty() && !manifest.groups.containsKey(selectedGroup)) throw new GradleException("Unknown Paper source group: " + selectedGroup);
        if (!Files.exists(root.resolve("upstream/Paper/.git"))) {
            throw new GradleException("Paper submodule is not initialized; initialize upstream/Paper before building");
        }
        try (Repository upstream = open(root.resolve("upstream/Paper"))) {
            verify(upstream, manifest);
            if (!operation.equals("verify")) for (var entry : manifest.groups.entrySet()) {
                String name = entry.getKey();
                if (!selectedGroup.isEmpty() && !selectedGroup.equals(name)) continue;
                safe(name);
                Group group = entry.getValue();
                Path directory = root.resolve("patches/" + name);
                Path work = root.resolve(".paper-work/" + name);
                switch (operation) {
                    case "apply", "prepare" -> {
                        Path output = operation.equals("apply") ? root.resolve("build/paper-patched/" + name) : work;
                        if (operation.equals("prepare") && Files.exists(work)) throw new GradleException("Patch editing directory already exists: " + work);
                        temporary(output.getParent(), tmp -> {
                            Path staged = tmp.resolve("sources");
                            replay(upstream, manifest, group, directory, staged);
                            publish(staged, output, tmp);
                        });
                    }
                    case "check" -> check(upstream, manifest, group, directory, work);
                    case "rebuild" -> rebuild(upstream, manifest, group, directory, work);
                    default -> throw new GradleException("Unknown Paper operation: " + operation);
                }
            }
        }
        getLogger().lifecycle("Paper sources {}: {}", operation, manifest.commit);
    }
}
