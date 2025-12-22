package io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

/**
 * The main goal of the wrapper runner - prohibit overriding arguments.
 *
 * We implicitly add code coverage argument here, so it is better to avoid any arguments correction next.
 */
class WrappedGradleRunner(private val gradleRunner: GradleRunner) {
    fun build(): BuildResult {
        return gradleRunner.build()
    }

    fun buildAndFail(): BuildResult {
        return gradleRunner.buildAndFail()
    }
}