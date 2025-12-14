package io.github.build.extensions.oss.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import io.github.build.extensions.oss.gradle.plugins.helm.HELM_GROUP
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmUninstall
import io.github.build.extensions.oss.gradle.plugins.helm.release.dsl.HelmRelease
import build.extensions.oss.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmUninstall<Release>")


/**
 * The name of the [HelmUninstall] task associated with this release.
 */
val HelmRelease.uninstallTaskName: String
    get() = namePattern.mapName(name)


/**
 * A rule that creates a task to uninstall a release from the active target.
 */
internal class HelmUninstallReleaseTaskRule(
    tasks: TaskContainer,
    releases: NamedDomainObjectCollection<HelmRelease>,
    private val activeTargetName: Provider<String>
) : AbstractHelmReleaseTaskRule<Task>(
    Task::class.java, tasks, releases, namePattern
) {

    override fun Task.configureFrom(release: HelmRelease) {

        group = HELM_GROUP
        description = "Uninstalls the ${release.name} release."

        dependsOn(
            activeTargetName.map { release.uninstallFromTargetTaskName(it) }
        )
    }
}
