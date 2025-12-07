package io.github.build.extensions.oss.gradle.plugins.helm.publishing.publishers

import java.io.Serializable


internal interface PublisherParams : Serializable {

    fun createPublisher(): HelmChartPublisher
}
