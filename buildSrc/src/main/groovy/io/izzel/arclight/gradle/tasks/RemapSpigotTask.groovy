package io.izzel.arclight.gradle.tasks

import io.izzel.arclight.gradle.Utils
import net.md_5.specialsource.SpecialSource
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class RemapSpigotTask implements Runnable {

    private final Project project

    RemapSpigotTask(Project project) {
        this.project = project
        includes = new ArrayList<>()
        includes.add('configurations')
        includes.add('META-INF/maven/org.spigotmc')
        includes.add('org/spigotmc')
        includes.add('org/bukkit/craftbukkit')
        includes.add('version.json')
        excludes = new ArrayList<>()
        excludes.add('org/bukkit/craftbukkit/libs/it')
        excludes.add('org/bukkit/craftbukkit/libs/org/apache')
        excludes.add('org/bukkit/craftbukkit/libs/org/codehaus')
        excludes.add('org/bukkit/craftbukkit/libs/org/eclipse')
        excludes.add('org/bukkit/craftbukkit/libs/jline')
    }

    private File inJar
    private File inExtraSrg
    private File outJar
    private File outDeobf
    private List<String> includes
    private List<String> excludes
    private String bukkitVersion
    private File inAt

    @Override
    void run() {
        Path official = inJar.toPath()
        Path extra = null
        Path staged = null
        try {
            if (inExtraSrg) {
                extra = Files.createTempFile('arclight-extra-input', '.jar')
                staged = Files.createTempFile('arclight-extra-output', '.jar')
                copy(official, extra, ['*'], [
                        'net/minecraft/world/level/block/entity/LecternBlockEntity$1.class',
                        'net/minecraft/world/level/block/ChestBlock$2$1.class'
                ])
                SpecialSource.main([
                        '-i', extra.toFile().canonicalPath,
                        '-o', staged.toFile().canonicalPath,
                        '-m', inExtraSrg.canonicalPath
                ] as String[])
                official = staged
            }
            // The upstream jar has official NMS names and unversioned CB.
            // Compilation and runtime both use the upstream CraftBukkit package.
            copy(official, outJar.toPath(), includes, excludes)
            def transformed = Files.createTempFile('arclight-spigot', '.jar')
            // SpecialSource's access-transformer pass uses an empty mapping, not a rename table.
            def emptyMapping = Files.createTempFile('arclight-access-only', '.srg')
            try {
                def args = [
                        '-i', official.toFile().canonicalPath,
                        '-o', transformed.toFile().canonicalPath,
                        '-m', emptyMapping.toFile().canonicalPath
                ]
                if (inAt) {
                    args.add('--access-transformer')
                    args.add(inAt.canonicalPath)
                }
                SpecialSource.main(args as String[])
                copy(transformed, outDeobf.toPath(), includes, excludes)
            } finally {
                Files.deleteIfExists(transformed)
                Files.deleteIfExists(emptyMapping)
            }
        } finally {
            if (extra != null) Files.deleteIfExists(extra)
            if (staged != null) Files.deleteIfExists(staged)
        }
    }

    private static void copy(Path inJar, Path outJar, List<String> includes, List<String> excludes) {
        def fileIn = new JarFile(inJar.toFile())
        def entries = fileIn.entries().collect { it.name }
        entries.removeIf { name ->
            !(includes.any { it == '*' || name.startsWith(it) } && !excludes.any { name.startsWith(it) })
        }
        Utils.using(new JarOutputStream(new FileOutputStream(outJar.toFile()))) { out ->
            entries.each { entry ->
                out.putNextEntry(new JarEntry(entry))
                def is = fileIn.getInputStream(new JarEntry(entry))
                Utils.write(is, out)
                is.close()
            }
        }
        fileIn.close()
    }

    @InputFile
    File getInJar() {
        return inJar
    }

    void setInJar(File inJar) {
        this.inJar = inJar
    }

    File getInExtraSrg() {
        return inExtraSrg
    }

    void setInExtraSrg(File inExtraSrg) {
        this.inExtraSrg = inExtraSrg
    }

    @Input
    List<String> getIncludes() {
        return includes
    }

    void setIncludes(List<String> includes) {
        this.includes = includes
    }

    @Input
    List<String> getExcludes() {
        return excludes
    }

    void setExcludes(List<String> excludes) {
        this.excludes = excludes
    }

    @OutputFile
    File getOutJar() {
        return outJar
    }

    void setOutJar(File outJar) {
        this.outJar = outJar
    }

    @OutputFile
    File getOutDeobf() {
        return outDeobf
    }

    void setOutDeobf(File outDeobf) {
        this.outDeobf = outDeobf
    }

    @Input
    @Optional
    String getBukkitVersion() {
        return bukkitVersion
    }

    void setBukkitVersion(String bukkitVersion) {
        this.bukkitVersion = bukkitVersion
    }

    @InputFile
    @Optional
    File getInAt() {
        return inAt
    }

    void setInAt(File inAt) {
        this.inAt = inAt
    }
}
