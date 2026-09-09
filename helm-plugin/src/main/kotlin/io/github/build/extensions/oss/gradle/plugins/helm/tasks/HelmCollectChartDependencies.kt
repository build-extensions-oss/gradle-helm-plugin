package io.github.build.extensions.oss.gradle.plugins.helm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject


open class HelmCollectChartDependencies : DefaultTask() {

    @get:Inject
    internal open val fileSystemOperations: FileSystemOperations
        get() = throw UnsupportedOperationException()


    @get:Inject
    internal open val archiveOperations: ArchiveOperations
        get() = throw UnsupportedOperationException()


    @get:InputFiles
    var dependencies: FileCollection =
        project.layout.files()


    @get:OutputDirectory
    val outputDir: DirectoryProperty =
        project.objects.directoryProperty()


    @TaskAction
    fun collectDependencies() {

        val result = fileSystemOperations.sync { spec ->
            spec.includeEmptyDirs = false
            spec.into(outputDir)

            dependencies.forEach { dependencyPackageFile ->
                spec.from(archiveOperations.tarTree(dependencyPackageFile))
            }
        }

        didWork = result.didWork
    }
}
