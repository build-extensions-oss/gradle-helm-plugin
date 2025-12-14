package io.github.build.extensions.oss.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import io.github.build.extensions.oss.gradle.plugins.helm.HELM_GROUP
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmInstallOrUpgrade
import io.github.build.extensions.oss.gradle.plugins.helm.release.dsl.HelmRelease
import build.extensions.oss.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmInstall<Release>")


/**
 * The name of the [HelmInstallOrUpgrade] task associated with this release.
 */
val HelmRelease.installTaskName: String
    get() = namePattern.mapName(name)


/**
 * A rule that creates a task to install a release to the active target.
 */
internal class HelmInstallReleaseTaskRule(
    tasks: TaskContainer,
    releases: NamedDomainObjectCollection<HelmRelease>,
    private val activeTargetName: Provider<String>
) : AbstractHelmReleaseTaskRule<Task>(
    Task::class.java, tasks, releases, namePattern
) {

    override fun Task.configureFrom(release: HelmRelease) {
        group = HELM_GROUP
        description = "Installs or upgrades the ${release.name} release to the active target."

        dependsOn(
            activeTargetName.map { release.installToTargetTaskName(it) }
        )
    }
}
