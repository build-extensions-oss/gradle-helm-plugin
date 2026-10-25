package io.github.build.extensions.oss.gradle.plugins.helm.command.tasks

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault


/**
 * Reconstructs the chart dependencies from the lock file, or mirrors the behavior of [HelmUpdateDependencies]
 * if the lock file does not exist.
 *
 * Corresponds to the `helm dependency build` CLI command.
 */
@DisableCachingByDefault(because = "See https://github.com/build-extensions-oss/gradle-helm-plugin/issues/208")
abstract class HelmBuildDependencies : AbstractHelmDependenciesTask() {

    @get:[InputFile Optional]
    final override val lockFile: Provider<RegularFile>
        // let's consider value files are defined in the same repository - and not in the shared file at the computer
        // Alternatively we will have a cache miss (which is correct)
        @PathSensitive(PathSensitivity.RELATIVE)
        get() = super.lockFile

    init {
        @Suppress("LeakingThis")
        onlyIf {
            val lockFile = this.lockFile.get().asFile
            if (lockFile.exists()) {
                // regular helm dep build behavior
                true

            } else {
                // helm dep update behavior
                // skip if the chart has no declared external dependencies
                readModelDependencies().dependencies
                    .any { it.repository != null }
            }
        }
    }


    @TaskAction
    fun buildDependencies() {
        loginToDependencyRegistries()
        execHelm("dependency", "build") {
            args(chartDir)
        }
    }
}
