package io.github.build.extensions.oss.gradle.plugins.helm.publishing.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import io.github.build.extensions.oss.gradle.plugins.helm.HELM_GROUP
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl.HelmPublishingRepository
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl.HelmPublishingRepositoryInternal
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.publishers.PublisherParams
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault


/**
 * Publishes a packaged helm chart to a repository.
 */
@DisableCachingByDefault(because = "See https://github.com/build-extensions-oss/gradle-helm-plugin/issues/208")
abstract class HelmPublishChart : DefaultTask() {

    init {
        group = HELM_GROUP
    }


    @get:Inject
    internal open val workerExecutor: WorkerExecutor
        get() = throw UnsupportedOperationException()


    /**
     * The chart name.
     */
    @get:Input
    abstract val chartName: Property<String>


    /**
     * The chart version.
     */
    @get:Input
    abstract val chartVersion: Property<String>


    /**
     * The chart package file to be published.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val chartFile: RegularFileProperty


    /**
     * The target repository.
     */
    @get:Internal
    var targetRepository: HelmPublishingRepository? = null


    /**
     * Publishes the chart.
     */
    @TaskAction
    fun publish() {

        val publisherParams = checkNotNull(targetRepository) { "Target repository must be set." }
            .let { targetRepository ->
                (targetRepository as HelmPublishingRepositoryInternal).publisherParams
            }

        workerExecutor.noIsolation().submit(PublishChartWorkAction::class.java) { params ->
            params.chartName.set(chartName)
            params.chartVersion.set(chartVersion)
            params.chartFile.set(chartFile)
            params.publisherParams.set(publisherParams)
        }
    }
}


internal interface PublishChartWorkParameters : WorkParameters {

    val chartName: Property<String>

    val chartVersion: Property<String>

    val chartFile: RegularFileProperty

    val publisherParams: Property<PublisherParams>
}


internal abstract class PublishChartWorkAction : WorkAction<PublishChartWorkParameters> {

    override fun execute() {

        val parameters = this.parameters
        val publisherParams = parameters.publisherParams.get()
        val chartName = parameters.chartName.get()
        val chartVersion = parameters.chartVersion.get()
        val chartFile = parameters.chartFile.get().asFile

        val publisher = publisherParams.createPublisher()

        publisher.publish(
            chartName = chartName,
            chartVersion = chartVersion,
            chartFile = chartFile
        )
    }
}
