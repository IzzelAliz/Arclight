package io.izzel.arclight.gradle.tasks

import net.md_5.specialsource.SpecialSource
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar

abstract class RenameJarTask extends Jar {

    private File mappings

    @InputFile
    File getMappings() {
        return mappings
    }

    void setMappings(File mappings) {
        this.mappings = mappings
    }

    @InputFile
    abstract RegularFileProperty getInputJar()

    @Input
    private boolean reverse

    boolean getReverse() {
        return reverse
    }

    void setReverse(boolean reverse) {
        this.reverse = reverse
    }

    @TaskAction
    void run() {
        def args = [
                '-i', inputJar.get().asFile.canonicalPath,
                '-o', outputs.files.singleFile.canonicalPath,
                '-m', mappings.canonicalPath,
        ]
        if (reverse) {
            args.add('--reverse')
        }
        SpecialSource.main(args.toArray(new String[0]) as String[])
    }
}
