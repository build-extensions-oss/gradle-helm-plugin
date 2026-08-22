package io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils

interface GradleRunnerParameters {
    val distribution: GradleDistribution
    // optional helm version. Gradle runner should override it, so we will download different helms in that case
    val helmVersion: String?
}
