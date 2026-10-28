package io.github.build.extensions.oss.gradle.plugins.helm.command.tasks

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import build.extensions.oss.gradle.pluginutils.property
import io.github.build.extensions.oss.gradle.plugins.helm.command.internal.HelmCommandRetry
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import java.time.Duration


/**
 * Updates the chart dependencies from the `Chart.yaml` file, and creates a `Chart.lock` file that
 * fixes the versions of chart dependencies.
 *
 * Corresponds to the `helm dependency update` CLI command.
 */
@DisableCachingByDefault(because = "See https://github.com/build-extensions-oss/gradle-helm-plugin/issues/208")
abstract class HelmUpdateDependencies : AbstractHelmDependenciesTask() {

    /**
     * If set to `true`, do not refresh the local repository cache.
     *
     * Corresponds to the `--skip-refresh` CLI option.
     */
    @Internal
    val skipRefresh: Property<Boolean> =
        project.objects.property()


    /**
     * The number of attempts `helm dependency update` makes before its failure is reported.
     *
     * Defaults to [HelmCommandRetry.DEFAULT_ATTEMPTS], which makes one attempt. Raise it to retry a transient failure.
     */
    @get:Internal
    val attempts: Property<Int> =
        project.objects.property<Int>()
            .convention(HelmCommandRetry.DEFAULT_ATTEMPTS)


    /**
     * The pause between attempts of `helm dependency update`, after a failed one.
     *
     * Defaults to [HelmCommandRetry.DEFAULT_DELAY].
     */
    @get:Internal
    val retryDelay: Property<Duration> =
        project.objects.property<Duration>()
            .convention(HelmCommandRetry.DEFAULT_DELAY)


    @get:[InputFile Optional]
    final override val dependencyDescriptorFile: Provider<RegularFile>
        // let's consider value files are defined in the same repository - and not in the shared file at the computer
        // Alternatively we will have a cache miss (which is correct)
        @PathSensitive(PathSensitivity.RELATIVE)
        get() = super.dependencyDescriptorFile


    @get:[OutputFile]
    override val lockFile: Provider<RegularFile>
        get() = super.lockFile


    init {
        @Suppress("LeakingThis")
        onlyIf {
            // skip if the chart has no declared external dependencies
            readModelDependencies().dependencies
                .any { it.repository != null }
        }
    }


    @TaskAction
    fun updateDependencies() {
        loginToDependencyRegistries()
        execHelmWithRetry("dependency", "update", attempts.get(), retryDelay.get()) {
            args(chartDir)
            flag("--skip-refresh", skipRefresh)
        }
    }
}
