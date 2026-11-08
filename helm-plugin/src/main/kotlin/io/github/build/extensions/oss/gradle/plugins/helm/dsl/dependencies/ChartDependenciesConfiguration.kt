package io.github.build.extensions.oss.gradle.plugins.helm.dsl.dependencies

import org.gradle.api.artifacts.Configuration
import java.util.Locale.getDefault


/**
 * Gets the name of the [Configuration] that contains the chart dependencies for the given chart.
 *
 * @param name the chart name
 * @return the name of the chart's dependencies configuration
 */
internal fun chartDependenciesConfigurationName(name: String): String {
    // make chart Aaa from aaa
    val updatedChartName = name.replaceFirstChar {
        if (it.isLowerCase()) {
            it.titlecase(getDefault())
        } else it.toString()
    }

    return "helm${updatedChartName}Dependencies"
}

