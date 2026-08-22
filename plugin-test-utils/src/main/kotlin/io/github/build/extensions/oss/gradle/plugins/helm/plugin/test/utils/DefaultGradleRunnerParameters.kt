package io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils

import java.util.stream.Stream
import org.junit.jupiter.params.provider.Arguments

data class DefaultGradleRunnerParameters(
    override val distribution: GradleDistribution,
    override val helmVersion: String?
) : GradleRunnerParameters {
    companion object {
        val allWithoutHelmVersion = GradleDistribution.all.map { gradleDistribution ->
            DefaultGradleRunnerParameters(gradleDistribution, null)
        }

        val all = allWithoutHelmVersion.flatMap { gradleDistribution ->
            listOf(
                gradleDistribution.copy(helmVersion = HelmVersionToTest.defaultHelmVersionV3),
                gradleDistribution.copy(helmVersion = HelmVersionToTest.defaultHelmVersionV4),
            )
        }

        val onlyLatestWithoutHelmVersion =
            allWithoutHelmVersion.filter { it.distribution is GradleDistribution.Current }

        /**
         * Older version of Gradle don't support convenient accessors like propertyA = value.
         * They only support propertyA.set(value)
         */
        val onlyWithNewKotlinDslSupport = allWithoutHelmVersion.filter { it.distribution is GradleDistribution.Current }

        @JvmStatic
        fun getDefaultParameterSetWithoutHelmVersion(): Stream<Arguments> {
            return allWithoutHelmVersion.map { Arguments.of(it) }
                .stream()
        }

        @JvmStatic
        fun getDefaultParameterSet(): Stream<Arguments> {
            return all.map { Arguments.of(it) }
                .stream()
        }

        @JvmStatic
        fun getLatestParameterSet(): Stream<Arguments> {
            return onlyLatestWithoutHelmVersion.map { Arguments.of(it) }
                .stream()
        }
    }
}
