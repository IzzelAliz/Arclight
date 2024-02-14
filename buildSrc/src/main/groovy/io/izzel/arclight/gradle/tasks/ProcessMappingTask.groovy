package io.izzel.arclight.gradle.tasks

import io.izzel.arclight.gradle.util.AwWriter
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.configuration.providers.mappings.MappingConfiguration
import net.fabricmc.lorenztiny.TinyMappingsReader
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.tree.MemoryMappingTree
import net.md_5.specialsource.InheritanceMap
import net.md_5.specialsource.Jar
import net.md_5.specialsource.provider.JarProvider
import org.cadixdev.at.AccessTransformSet
import org.cadixdev.at.io.AccessTransformFormats
import org.cadixdev.bombe.type.MethodDescriptor
import org.cadixdev.bombe.type.ObjectType
import org.cadixdev.lorenz.MappingSet
import org.cadixdev.lorenz.io.srg.SrgWriter
import org.cadixdev.lorenz.io.srg.csrg.CSrgReader
import org.cadixdev.lorenz.io.srg.tsrg.TSrgReader
import org.cadixdev.lorenz.io.srg.tsrg.TSrgWriter
import org.cadixdev.lorenz.model.ClassMapping
import org.cadixdev.lorenz.model.FieldMapping
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.objectweb.asm.Type

import java.util.stream.Collectors

class ProcessMappingTask implements Runnable {

    private final Project project

    ProcessMappingTask(Project project) {
        this.project = project
    }

    private File buildData
    private File inJar
    private String mcVersion
    private String bukkitVersion
    private File outDir

    @Override
    void run() {
        def tree = new MemoryMappingTree()
        MappingReader.read(LoomGradleExtension.get(project).mappingConfiguration.tinyMappingsWithSrg, tree)
        def mcp = new TinyMappingsReader(tree, "named", "srg").read()
        def intermediaryRev = new TinyMappingsReader(tree, "intermediary", "official").read()

        def mojmapTree = new MemoryMappingTree()
        MappingReader.read(MappingConfiguration.getMojmapSrgFileIfPossible(project), mojmapTree)
        def official = new TinyMappingsReader(mojmapTree, "official", "named").read()
        def officialRev = official.reverse()

        if (!outDir.isDirectory()) {
            outDir.mkdirs()
        }
        new File(outDir, "srg_to_named.srg").withWriter {
            new SrgWriter(it) {
                @Override
                void write(final MappingSet mappings) {
                    mappings.getTopLevelClassMappings().stream()
                            .filter { !it.hasMappings() }
                            .sorted(this.getConfig().getClassMappingComparator())
                            .forEach(this::writeClassMapping)
                    super.write(mappings)
                }

                @Override
                protected void writeClassMapping(final ClassMapping<?, ?> mapping) {
                    if (!mapping.hasDeobfuscatedName()) {
                        this.writer.println(String.format("CL: %s %s", mapping.getFullObfuscatedName(), mapping.getFullDeobfuscatedName()))
                    }
                    super.writeClassMapping(mapping)
                }
            }.write(new TinyMappingsReader(tree, "srg", "named").read())
        }

        def srg = MappingSet.create()
        LoomGradleExtension.get(project).srgProvider.mergedMojangRaw.toFile().withReader {
            def data = it.lines().filter { String s -> !(s.startsWith('\t\t') || s.startsWith('tsrg2')) }.collect(Collectors.joining('\n'))
            new TSrgReader(new StringReader(data.toString())).read(srg)
        }

        def csrg = MappingSet.create()
        def clFile = new File(buildData, "mappings/bukkit-$mcVersion-cl.csrg")
        clFile.withReader {
            new CSrgReader(it).read(csrg)
        }
        def srgRev = srg.reverse()
        def finalMap = srgRev.merge(csrg).reverse()
        def neoforgeMap = officialRev.merge(csrg).reverse()
        def fabricMap = intermediaryRev.merge(csrg).reverse()

        def im = new InheritanceMap()
        def classes = [] as ArrayList<String>
        csrg.topLevelClassMappings.each {
            classes.add(it.fullDeobfuscatedName)
            it.innerClassMappings.each {
                classes.add(it.fullDeobfuscatedName)
            }
        }
        im.generate(new JarProvider(Jar.init(this.inJar)), classes)
        new File(outDir, 'inheritanceMap.txt').withWriter { w ->
            for (def className : classes) {
                def parents = im.getParents(className).collect { finalMap.getOrCreateClassMapping(it).fullDeobfuscatedName }
                if (!parents.isEmpty()) {
                    w.print(finalMap.getOrCreateClassMapping(className).fullDeobfuscatedName)
                    w.print(' ')
                    w.println(parents.join(' '))
                }
            }
        }
        new File(outDir, 'inheritanceMap_intermediary.txt').withWriter { w ->
            for (def className : classes) {
                def parents = im.getParents(className).collect { fabricMap.getOrCreateClassMapping(it).fullDeobfuscatedName }
                if (!parents.isEmpty()) {
                    w.print(fabricMap.getOrCreateClassMapping(className).fullDeobfuscatedName)
                    w.print(' ')
                    w.println(parents.join(' '))
                }
            }
        }
        new File(outDir, 'bukkit_srg.srg').withWriter {
            new TSrgWriter(it) {
                @Override
                void write(final MappingSet mappings) {
                    mappings.getTopLevelClassMappings().stream()
                            .sorted(this.getConfig().getClassMappingComparator())
                            .forEach(this::writeClassMapping)
                }

                @Override
                protected void writeClassMapping(ClassMapping<?, ?> mapping) {
                    if (!mapping.hasMappings()) {
                        this.writer.println(String.format("%s %s", mapping.getFullObfuscatedName(), mapping.getFullDeobfuscatedName()));
                    } else if (mapping.fullObfuscatedName.contains('/')) {
                        super.writeClassMapping(mapping)
                    }
                }

                @Override
                protected void writeFieldMapping(FieldMapping mapping) {
                    def cl = srgRev.getClassMapping(mapping.parent.fullDeobfuscatedName).get()
                    def field = cl.getFieldMapping(mapping.deobfuscatedName).get().deobfuscatedName
                    def nmsCl = official.getClassMapping(cl.fullDeobfuscatedName)
                            .get().getFieldMapping(field).get().signature.type.get()
                    def sig = Type.getType(csrg.deobfuscate(nmsCl).toString()).getClassName()
                    this.writer.println(String.format("    %s %s -> %s", sig, mapping.getDeobfuscatedName(), mapping.getObfuscatedName()))
                }
            }.write(finalMap)
        }
        new File(outDir, 'bukkit_moj.srg').withWriter {
            new TSrgWriter(it) {
                @Override
                void write(final MappingSet mappings) {
                    mappings.getTopLevelClassMappings().stream()
                            .sorted(this.getConfig().getClassMappingComparator())
                            .forEach(this::writeClassMapping)
                }

                @Override
                protected void writeClassMapping(ClassMapping<?, ?> mapping) {
                    if (!mapping.hasMappings()) {
                        this.writer.println(String.format("%s %s", mapping.getFullObfuscatedName(), mapping.getFullDeobfuscatedName()));
                    } else if (mapping.fullObfuscatedName.contains('/')) {
                        super.writeClassMapping(mapping)
                    }
                }

                @Override
                protected void writeFieldMapping(FieldMapping mapping) {
                    def cl = officialRev.getClassMapping(mapping.parent.fullDeobfuscatedName).get()
                    def field = cl.getFieldMapping(mapping.deobfuscatedName).get().deobfuscatedName
                    def nmsCl = official.getClassMapping(cl.fullDeobfuscatedName)
                            .get().getFieldMapping(field).get().signature.type.get()
                    def sig = Type.getType(csrg.deobfuscate(nmsCl).toString()).getClassName()
                    this.writer.println(String.format("    %s %s -> %s", sig, mapping.getDeobfuscatedName(), mapping.getObfuscatedName()))
                }
            }.write(neoforgeMap)
        }
        new File(outDir, 'bukkit_intermediary.srg').withWriter {
            new TSrgWriter(it) {
                @Override
                void write(final MappingSet mappings) {
                    mappings.getTopLevelClassMappings().stream()
                            .sorted(this.getConfig().getClassMappingComparator())
                            .forEach(this::writeClassMapping)
                }

                @Override
                protected void writeClassMapping(ClassMapping<?, ?> mapping) {
                    if (!mapping.hasMappings()) {
                        this.writer.println(String.format("%s %s", mapping.getFullObfuscatedName(), mapping.getFullDeobfuscatedName()));
                    } else if (mapping.fullObfuscatedName.contains('/')) {
                        super.writeClassMapping(mapping)
                    }
                }

                @Override
                protected void writeFieldMapping(FieldMapping mapping) {
                    def cl = intermediaryRev.getClassMapping(mapping.parent.fullDeobfuscatedName).get()
                    def field = cl.getFieldMapping(mapping.deobfuscatedName).get().deobfuscatedName
                    def nmsCl = official.getClassMapping(cl.fullDeobfuscatedName)
                            .get().getFieldMapping(field).get().signature.type.get()
                    def sig = Type.getType(csrg.deobfuscate(nmsCl).toString()).getClassName()
                    this.writer.println(String.format("    %s %s -> %s", sig, mapping.getDeobfuscatedName(), mapping.getObfuscatedName()))
                }
            }.write(fabricMap)
        }
        new File(outDir, 'bukkit_at.at').withWriter { w ->
            new File(buildData, "mappings/bukkit-${mcVersion}.at").eachLine { l ->
                if (l.trim().isEmpty() || l.startsWith('#')) {
                    w.writeLine(l)
                    return
                }
                def split = l.split(' ', 2)
                def i = split[1].indexOf('(')
                if (i == -1) {
                    def name = split[1].substring(split[1].lastIndexOf('/') + 1)
                    if (name.charAt(0).isUpperCase() && name.charAt(1).isLowerCase()) {
                        w.writeLine("${split[0].replace('inal', '')} ${(finalMap.deobfuscate(new ObjectType(split[1])) as ObjectType).className.replace('/', '.')}")
                    } else {
                        def cl = split[1].substring(0, split[1].lastIndexOf('/'))
                        def f = finalMap.getClassMapping(cl)
                                .flatMap { mcp.getClassMapping(it.fullDeobfuscatedName) }
                                .flatMap { it.getFieldMapping(name) }
                                .map { it.deobfuscatedName }
                        if (f.isEmpty()) {
                            w.writeLine("# TODO ${split[0].replace('inal', '')} ${(finalMap.deobfuscate(new ObjectType(cl)) as ObjectType).className.replace('/', '.')} $name")
                        } else {
                            w.writeLine("${split[0].replace('inal', '')} ${(finalMap.deobfuscate(new ObjectType(cl)) as ObjectType).className.replace('/', '.')} ${f.get()}")
                        }
                    }
                } else {
                    def desc = split[1].substring(i)
                    def s = split[1].substring(0, i)
                    def cl = s.substring(0, s.lastIndexOf('/'))
                    def name = s.substring(s.lastIndexOf('/') + 1)
                    def m = finalMap.getClassMapping(cl)
                            .flatMap { mcp.getClassMapping(it.fullDeobfuscatedName) }
                            .map() { it.methodMappings.find { it.obfuscatedName == name } }
                    if (m.isEmpty()) {
                        w.writeLine("${name == '<init>' ? '' : '# TODO '}${split[0].replace('inal', '')} ${(finalMap.deobfuscate(new ObjectType(cl)) as ObjectType).className.replace('/', '.')} $name${finalMap.deobfuscate(MethodDescriptor.of(desc))}")
                    } else {
                        w.writeLine("${split[0].replace('inal', '')} ${(finalMap.deobfuscate(new ObjectType(cl)) as ObjectType).className.replace('/', '.')} ${m.get().deobfuscatedName}${m.get().deobfuscatedDescriptor}")
                    }
                }
            }
        }
        new File(outDir, 'bukkit_at.at').withReader { r ->
            def at = AccessTransformSet.create()
            AccessTransformFormats.FML.read(r, at)
            def srgToIntermediate = new TinyMappingsReader(tree, "srg", "named").read()
            def remapped = at.remap(srgToIntermediate)
            new File(outDir, 'bukkit_aw.aw').withWriter { w ->
                new AwWriter(w, LoomGradleExtension.get(project).namedMinecraftProvider).write(remapped)
            }
        }
        new File(outDir, 'reobf_bukkit.srg').text = "PK: org/bukkit/craftbukkit/v org/bukkit/craftbukkit/$bukkitVersion"
    }

    @InputDirectory
    File getBuildData() {
        return buildData
    }

    void setBuildData(File buildData) {
        this.buildData = buildData
    }

    @InputFile
    File getInJar() {
        return inJar
    }

    void setInJar(File inJar) {
        this.inJar = inJar
    }

    @Input
    String getMcVersion() {
        return mcVersion
    }

    void setMcVersion(String mcVersion) {
        this.mcVersion = mcVersion
    }

    @Input
    String getBukkitVersion() {
        return bukkitVersion
    }

    void setBukkitVersion(String bukkitVersion) {
        this.bukkitVersion = bukkitVersion
    }

    @OutputDirectory
    File getOutDir() {
        return outDir
    }

    void setOutDir(File outDir) {
        this.outDir = outDir
    }
}
