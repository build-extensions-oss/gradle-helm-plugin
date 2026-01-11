package io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils

import java.util.stream.Stream
import org.junit.jupiter.params.provider.Arguments

data class DefaultGradleRunnerParameters(override val distribution: GradleDistribution) : GradleRunnerParameters {
    companion object {
        val all = GradleDistribution.all.map { gradleDistribution ->
            DefaultGradleRunnerParameters(gradleDistribution)
        }

        val onlyLatest = all.filter { it.distribution is GradleDistribution.Current }

        /**
         * Older version of Gradle don't support convenient accessors like propertyA = value.
         * They only support propertyA.set(value)
         */
        val onlyWithNewKotlinDslSupport = all.filter { it.distribution is GradleDistribution.Current }

        @JvmStatic
        fun getDefaultParameterSet(): Stream<Arguments> {
            return all.map { Arguments.of(it) }
                .stream()
        }

        @JvmStatic
        fun getLatestParameterSet(): Stream<Arguments> {
            return onlyLatest.map { Arguments.of(it) }
                .stream()
        }
    }
}
