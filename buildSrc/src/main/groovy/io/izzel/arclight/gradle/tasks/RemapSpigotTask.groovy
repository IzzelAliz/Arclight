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

    private File ssJar
    private File inJar
    private File inSrg
    private File inExtraSrg
    private File inSrgToStable
    private File inheritanceMap
    private File outJar
    private File outDeobf
    private List<String> includes
    private List<String> excludes
    private String bukkitVersion
    private File inAt

    @Override
    void run() {
        def tmp = Files.createTempFile("arclight", "jar")
        SpecialSource.main(new String[]{
                '-i', inJar.canonicalPath,
                '-o', tmp.toFile().canonicalPath,
                '-m', inSrg.canonicalPath,
                '-h', inheritanceMap.canonicalPath})
        if (inExtraSrg) {
            def tmp2 = Files.createTempFile("arclight", "jar")
            copy(tmp, tmp2, ['*'], [
                    'net/minecraft/world/level/block/entity/LecternBlockEntity$1.class',
                    'net/minecraft/world/level/block/ChestBlock$2$1.class'
            ])
            SpecialSource.main(new String[]{
                    '-i', tmp2.toFile().canonicalPath,
                    '-o', tmp.toFile().canonicalPath,
                    '-m', inExtraSrg.canonicalPath})
        }
        def tmpDeobf = Files.createTempFile("arclight", "jar")
        def args = [
                '-i', tmp.toFile().canonicalPath,
                '-o', tmpDeobf.toFile().canonicalPath,
                '-m', inSrgToStable.canonicalPath,
                '-h', inheritanceMap.canonicalPath
        ]
        Path tmpSrg
        if (bukkitVersion) {
            tmpSrg = Files.createTempFile("arclight", "srg")
            tmpSrg.text = "PK: org/bukkit/craftbukkit/$bukkitVersion org/bukkit/craftbukkit/v"
            args.add('-m')
            args.add(tmpSrg.toFile().canonicalPath)
        }
        if (inAt) {
            args.add('--access-transformer')
            args.add(inAt.canonicalPath)
        }
        SpecialSource.main(args.toArray(new String[0]))
        copy(tmp, outJar.toPath(), includes, excludes)
        copy(tmpDeobf, outDeobf.toPath(), includes, excludes)
        Files.delete(tmp)
        Files.delete(tmpDeobf)
        if (tmpSrg) Files.delete(tmpSrg)
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

    @InputFile
    File getInSrg() {
        return inSrg
    }

    void setInSrg(File inSrg) {
        this.inSrg = inSrg
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

    @InputFile
    File getSsJar() {
        return ssJar
    }

    void setSsJar(File ssJar) {
        this.ssJar = ssJar
    }

    @InputFile
    File getInSrgToStable() {
        return inSrgToStable
    }

    void setInSrgToStable(File inSrgToStable) {
        this.inSrgToStable = inSrgToStable
    }

    @InputFile
    File getInheritanceMap() {
        return inheritanceMap
    }

    void setInheritanceMap(File inheritanceMap) {
        this.inheritanceMap = inheritanceMap
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
