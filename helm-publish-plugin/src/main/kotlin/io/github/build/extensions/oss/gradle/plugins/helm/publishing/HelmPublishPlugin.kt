package io.github.build.extensions.oss.gradle.plugins.helm.publishing

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskDependency
import org.gradle.internal.reflect.Instantiator
import io.github.build.extensions.oss.gradle.plugins.helm.HELM_GROUP
import io.github.build.extensions.oss.gradle.plugins.helm.HelmPlugin
import io.github.build.extensions.oss.gradle.plugins.helm.command.HelmCommandsPlugin
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmChart
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.internal.charts
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.internal.helm
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl.HelmChartPublishExtension
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl.HelmPublishingExtension
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl.createHelmChartPublishExtension
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl.createHelmPublishingExtension
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl.internal.repositories
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.rules.HelmPublishChartTaskRule
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.rules.HelmPublishChartToRepositoryTaskRule
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.rules.publishTaskName
import javax.inject.Inject


/**
 * A Gradle plugin that adds publishing capabilities for Helm charts.
 */
class HelmPublishPlugin
@Inject constructor(
    private val instantiator: Instantiator
) : Plugin<Project> {

    override fun apply(project: Project) {

        project.plugins.apply(HelmCommandsPlugin::class.java)

        val publishingExtension = createPublishingExtension(project)

        project.plugins.withType(HelmPlugin::class.java) {

            val charts = project.helm.charts
            charts.all { chart ->
                addChartPublishExtension(chart, project)
            }

            project.tasks.run {
                val repositories = publishingExtension.repositories
                addRule(HelmPublishChartToRepositoryTaskRule(this, charts, repositories))
                addRule(HelmPublishChartTaskRule(this, charts, repositories))
            }

            createPublishAllTask(project, charts)
        }
    }


    /**
     * Creates and registers a `helm.publishing` sub-extension with the DSL.
     */
    private fun createPublishingExtension(project: Project) =
        project.objects.createHelmPublishingExtension(instantiator)
            .apply {
                (project.helm as ExtensionAware).extensions.add(
                    HelmPublishingExtension::class.java,
                    HELM_PUBLISHING_EXTENSION_NAME,
                    this
                )
            }


    /**
     * Adds a convention object to a chart for holding publishing-related properties.
     *
     * @see HelmChartPublishExtension
     */
    private fun addChartPublishExtension(chart: HelmChart, project: Project) {
        (chart as ExtensionAware).extensions.add(
            HELM_CHART_PUBLISHING_CONVENTION_NAME,
            project.objects.createHelmChartPublishExtension()
        )
    }


    /**
     * Create a task that publishes all charts in the project to all publishing repositories.
     */
    private fun createPublishAllTask(project: Project, charts: NamedDomainObjectContainer<HelmChart>) {
        project.tasks.register("helmPublish") { task ->
            task.group = HELM_GROUP
            task.description = "Publishes all Helm charts."

            task.dependsOn(TaskDependency {
                charts.map { chart -> project.tasks.getByName(chart.publishTaskName) }
                    .toSet()
            })
        }
    }
}
