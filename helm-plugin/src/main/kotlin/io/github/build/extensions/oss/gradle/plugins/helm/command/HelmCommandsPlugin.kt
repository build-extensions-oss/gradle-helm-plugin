package io.github.build.extensions.oss.gradle.plugins.helm.command

import build.extensions.oss.gradle.pluginutils.booleanProviderFromProjectProperty
import build.extensions.oss.gradle.pluginutils.durationProviderFromProjectProperty
import io.github.build.extensions.oss.gradle.plugins.helm.HELM_EXTENSION_NAME
import io.github.build.extensions.oss.gradle.plugins.helm.HELM_LINT_EXTENSION_NAME
import io.github.build.extensions.oss.gradle.plugins.helm.command.internal.conventionsFrom
import io.github.build.extensions.oss.gradle.plugins.helm.command.rules.extractClientTaskName
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.AbstractHelmCommandTask
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.AbstractHelmInstallationCommandTask
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.AbstractHelmServerCommandTask
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.AbstractHelmServerOperationCommandTask
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmExtractClient
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmDownloadClient
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmExtension
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.Linting
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.createHelmExtension
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.createLinting
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskDependency


/**
 * The name of the Helm executable to use if neither an explicit path nor an automatic download is configured.
 */
private const val DEFAULT_HELM_EXECUTABLE = "helm"


class HelmCommandsPlugin
    : Plugin<Project> {

    override fun apply(project: Project) {

        val helmExtension = project.createHelmExtension()
        project.extensions.add(HelmExtension::class.java, HELM_EXTENSION_NAME, helmExtension)

        // Install the HelmDownloadClientPlugin on the root project, this allows us to sync the download
        // between multiple subprojects that need the Helm client
        project.rootProject.pluginManager.apply(HelmDownloadClientPlugin::class.java)

        project.objects.createLinting()
            .apply {
                enabled.convention(
                    project.booleanProviderFromProjectProperty("helm.lint.enabled", defaultValue = true)
                )
                strict.convention(
                    project.booleanProviderFromProjectProperty("helm.lint.strict")
                )

                (helmExtension as ExtensionAware).extensions
                    .add(Linting::class.java, HELM_LINT_EXTENSION_NAME, this)
            }


        // Apply the global Helm options as defaults to each command task
        val extractClientTask = project.extractClientTask(helmExtension.downloadClient)

        project.tasks.withType(AbstractHelmCommandTask::class.java).configureEach { task ->
            task.conventionsFrom(helmExtension)

            task.downloadedExecutable.set(
                extractClientTask.flatMap { it.executable }.map { it.asFile.absolutePath }
            )

            // An explicitly configured executable wins; otherwise use the downloaded client if the
            // download is enabled, and fall back to whatever is on the PATH.
            task.executable.convention(
                helmExtension.executable
                    .orElse(task.downloadedExecutable)
                    .orElse(DEFAULT_HELM_EXECUTABLE)
            )

            task.dependsOn(TaskDependency { setOfNotNull(extractClientTask.orNull) })
        }

        project.tasks.withType(AbstractHelmServerCommandTask::class.java).configureEach { task ->
            task.conventionsFrom(helmExtension as ConfigurableHelmServerOptions)
        }

        project.tasks.withType(AbstractHelmServerOperationCommandTask::class.java).configureEach { task ->
            task.dryRun.convention(
                project.booleanProviderFromProjectProperty("helm.dryRun")
            )
            task.noHooks.convention(
                project.booleanProviderFromProjectProperty("helm.noHooks")
            )
            task.remoteTimeout.convention(
                project.durationProviderFromProjectProperty("helm.remoteTimeout")
            )
        }

        project.tasks.withType(AbstractHelmInstallationCommandTask::class.java).configureEach { task ->
            task.atomic.convention(
                project.booleanProviderFromProjectProperty("helm.atomic")
            )
            task.wait.convention(
                project.booleanProviderFromProjectProperty("helm.wait")
            )
            task.waitForJobs.convention(
                project.booleanProviderFromProjectProperty("helm.waitForJobs")
            )
        }
    }


    /**
     * Locates the task in the root project that extracts the configured version of the Helm client.
     *
     * The provider has no value if the automatic client download is not enabled. Everything inside it is
     * resolved lazily, so that simply applying the plugin does not create the download tasks.
     *
     * This deliberately lives in the plugin rather than on the [HelmDownloadClient] DSL object: the resulting
     * provider yields a [HelmExtractClient] task, and a task is not something that may be reachable from the
     * state of another task.
     */
    private fun Project.extractClientTask(downloadClient: HelmDownloadClient): Provider<HelmExtractClient> =
        downloadClient.enabled.flatMap { enabled ->
            if (enabled) {
                downloadClient.version.flatMap { version ->
                    rootProject.tasks.named(extractClientTaskName(version), HelmExtractClient::class.java)
                }
            } else {
                provider { null }
            }
        }
}
