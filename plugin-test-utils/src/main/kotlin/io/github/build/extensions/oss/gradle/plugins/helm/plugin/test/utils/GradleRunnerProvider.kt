package io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils

import java.net.URI
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import java.io.File
import java.lang.management.ManagementFactory

object GradleRunnerProvider {
    private const val distributionUrlPrefixProperty =
        "io.github.build.extensions.oss.gradle.helm.plugin.distribution.url.prefix"
    private const val defaultDistributionUrlPrefix = "https://services.gradle.org/distributions"

    /**
     * Gradle might start a subprocess to run code in a separate Gradle configuration. So, let's copy Kover agent there.
     *
     * It seems like Kover has a single jvm agent entry, so let's copy it as-is.
     * At the time of writing, Kover always merges new and old code coverage data, so it is safe to put everything into a single file.
     */
    private val koverJavaAgentArguments by lazy {
        val allInputArguments = ManagementFactory.getRuntimeMXBean().inputArguments

        val javaAgentArgument = allInputArguments.singleOrNull {
            it.contains("kover")
        }
        // value is like:
        // -javaagent:xxx/build/kover/kover-jvm-agent-0.9.3.jar=file:xxx/functionalTest/kover-agent.args
        val rawArgument = requireNotNull(javaAgentArgument) {
            "Unable to find Kover input arguments in $allInputArguments"
        }

        return@lazy rawArgument
    }

    /**
     * Create a specific runner for plugin testing. It can take into account plugin parameters (which can be iterated in parameterised tests).
     * Additionally, it injects code coverage metrics, so we will collect code coverage from child process as well.
     */
    fun createRunner(
        parameters: GradleRunnerParameters,
        arguments: List<String>,
        projectDir: File
    ): WrappedGradleRunner {
        val runner = GradleRunner.create()
            // strictly don't use plugin classpath - use the jars published into the local repository
            // .withPluginClasspath()
            // set arguments here. With a positive probability someone can override that - let's ignore it for now, because
            .withArguments(arguments)
            .withProjectDir(projectDir)
            .let { runner ->
                applyDistribution(runner, parameters.distribution)
            } as DefaultGradleRunner

        runner.withJvmArguments(koverJavaAgentArguments)

        return WrappedGradleRunner(runner)
    }

    private fun applyDistribution(runner: GradleRunner, distribution: GradleDistribution): GradleRunner {
        return when (distribution) {
            GradleDistribution.Current -> runner
            is GradleDistribution.Custom -> applyCustomDistribution(runner, distribution)
        }
    }

    private fun applyCustomDistribution(runner: GradleRunner, distribution: GradleDistribution.Custom): GradleRunner {
        val distributionUrl = System.getProperty(distributionUrlPrefixProperty, defaultDistributionUrlPrefix)
        val url = "$distributionUrl/gradle-${distribution.version}-all.zip"

        return runner.withGradleDistribution(URI.create(url))
    }
}
