package io.github.build.extensions.oss.gradle.plugins.helm.command.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import build.extensions.oss.gradle.pluginutils.listProperty
import io.github.build.extensions.oss.gradle.plugins.helm.command.internal.RegistryLogin
import io.github.build.extensions.oss.gradle.plugins.helm.model.ChartDescriptor
import io.github.build.extensions.oss.gradle.plugins.helm.model.ChartDescriptorYaml
import io.github.build.extensions.oss.gradle.plugins.helm.model.ChartModelDependencies
import io.github.build.extensions.oss.gradle.plugins.helm.model.ChartRequirementsYaml
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "See https://github.com/build-extensions-oss/gradle-helm-plugin/issues/208")
abstract class AbstractHelmDependenciesTask : AbstractHelmCommandTask() {

    /**
     * The chart directory.
     */
    @get:Internal("Represented as part of other properties")
    open val chartDir: DirectoryProperty =
        project.objects.directoryProperty()


    /**
     * Path to the `Chart.yaml` file of the chart (read-only).
     */
    private val chartYamlFile: Provider<RegularFile> =
        chartDir.file("Chart.yaml")


    /**
     * Path to the `requirements.yaml` file of the chart (read-only).
     */
    private val requirementsYamlFile: Provider<RegularFile> =
        chartDir.file("requirements.yaml")


    /**
     * The chart descriptor, as parsed from the `Chart.yaml` file.
     */
    @get:Internal
    internal val chartDescriptor: Provider<ChartDescriptor> =
        ChartDescriptorYaml.loading(chartYamlFile)


    /**
     * Provides the correct name of the file containing the chart's dependencies, as indicated by the
     * API version specified in the Chart.yaml file.
     */
    private val dependencyDescriptorFileName: Provider<String> =
        chartDescriptor.map { descriptor ->
            if (descriptor.apiVersion == "v1") "requirements.yaml" else "Chart.yaml"
        }


    /**
     * Provides the file containing the chart's dependencies, as indicated by the API version specified
     * in the Chart.yaml file.
     */
    @get:Internal
    internal open val dependencyDescriptorFile: Provider<RegularFile> =
        chartDir.file(dependencyDescriptorFileName)


    /**
     * Provides the correct name of the lock file, as indicated by the API version specified in the
     * Chart.yaml file.
     */
    private val lockFileName: Provider<String> =
        chartDescriptor.map { descriptor ->
            if (descriptor.apiVersion == "v1") "requirements.lock" else "Chart.lock"
        }


    /**
     * A [FileCollection] containing the `Chart.lock` _or_ the `requirements.lock` file, depending on the
     * Chart API version and only if it is present.
     */
    @get:Internal
    internal open val lockFile: Provider<RegularFile> =
        chartDir.file(lockFileName)


    /**
     * Reads the chart's dependencies from the `Chart.yaml` (or, for API version v1, the `requirements.yaml`)
     * file, as it is right now.
     */
    internal fun readModelDependencies(): ChartModelDependencies {
        val descriptor = ChartDescriptorYaml.load(chartYamlFile.get().asFile)
        if (descriptor.apiVersion != "v1") {
            return descriptor
        }
        val requirementsFile = requirementsYamlFile.get().asFile
        return if (requirementsFile.exists()) {
            ChartRequirementsYaml.load(requirementsYamlFile.get())
        } else {
            ChartModelDependencies.empty
        }
    }


    /**
     * The OCI registries that chart dependencies may be pulled from, with their credentials.
     *
     * Wired by the `helm` plugin from `helm.registries`, and from `helm.repositories` entries with an
     * `oci://` URL. Deliberately not a task input: new credentials do not make the dependencies outdated.
     */
    @get:Internal
    internal val registryLogins: ListProperty<RegistryLogin> =
        project.objects.listProperty()


    /**
     * Logs in to each of the [registryLogins] that a dependency of the chart is pulled from.
     *
     * Called from the task action, right before Helm resolves the dependencies: an up-to-date or skipped task
     * doesn't execute the login.
     */
    internal fun loginToDependencyRegistries() {
        val repositories = readModelDependencies().dependencies.mapNotNull { it.repository }
        registryLogins.get()
            .filter { login -> repositories.any(login::servesRepository) }
            .distinctBy { it.host.lowercase() }
            .forEach(::loginToRegistry)
    }


    /**
     * The _charts_ sub-directory; this is where sub-charts will be placed by the command (read-only).
     */
    @get:OutputDirectory
    @Suppress("unused")
    val subchartsDir: Provider<Directory> =
        chartDir.dir("charts")
}
