@file:JvmName("HelmPublishPluginExtensionAccessors")
package org.gradle.kotlin.dsl

import org.gradle.api.provider.Property
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmChart
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl.publishConvention


/**
 * Indicates whether tasks for publishing this chart should be created automatically.
 *
 * Defaults to `true`.
 */
val HelmChart.publish: Property<Boolean>
    get() = publishConvention.publish
