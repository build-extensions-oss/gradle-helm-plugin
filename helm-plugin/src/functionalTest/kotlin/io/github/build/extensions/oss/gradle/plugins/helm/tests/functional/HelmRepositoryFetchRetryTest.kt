package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.HelmExecutable
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.io.TempDirDeletionStrategy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource


/**
 * Checks that a repository fetch task retries a transient failure, and still fails once its attempts are exhausted,
 * by driving the task against a helm executable which fails a configured number of times.
 */
internal class HelmRepositoryFetchRetryTest {

    private val sourceDirectory = File("./src/functionalTest/resources/test/repository-retry")

    @TempDir(deletionStrategy = TempDirDeletionStrategy.IgnoreFailures::class)
    private lateinit var testProjectDir: File

    @TempDir(deletionStrategy = TempDirDeletionStrategy.IgnoreFailures::class)
    private lateinit var executableDir: File

    private lateinit var invocationLog: File

    private lateinit var attemptCounter: File

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)
        invocationLog = testProjectDir.resolve("helm-invocations.log")
        attemptCounter = testProjectDir.resolve("helm-attempts.txt")
    }

    @ParameterizedTest
    @MethodSource("io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters#getDefaultParameterSetWithoutHelmVersion")
    fun helmAddRepositoryShouldRetryATransientFailure(parameters: DefaultGradleRunnerParameters) {
        // given a helm which fails the first attempt and succeeds afterwards
        val executable = HelmExecutable.getFailingExecutableParameter(
            temporaryFolder = executableDir,
            invocationLog = invocationLog,
            attemptCounter = attemptCounter,
            failures = 1
        )

        // when
        val result = createRunner(parameters, executable.parameterValue).build()

        // then
        result.output shouldContain "BUILD SUCCESSFUL"
        invocations() shouldBe 2
    }

    @ParameterizedTest
    @MethodSource("io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters#getDefaultParameterSetWithoutHelmVersion")
    fun helmAddRepositoryShouldFailOnceTheAttemptsAreExhausted(parameters: DefaultGradleRunnerParameters) {
        // given a helm which fails more often than the task attempts
        val executable = HelmExecutable.getFailingExecutableParameter(
            temporaryFolder = executableDir,
            invocationLog = invocationLog,
            attemptCounter = attemptCounter,
            failures = 10
        )

        // when
        val result = createRunner(parameters, executable.parameterValue).buildAndFail()

        // then
        result.output shouldContain "BUILD FAILED"
        result.output shouldContain "simulated transient failure"
        invocations() shouldBe 3
    }

    private fun createRunner(parameters: DefaultGradleRunnerParameters, executableParameter: String) =
        GradleRunnerProvider.createRunner(
            parameters = parameters,
            projectDir = testProjectDir,
            arguments = listOf("helmAddExampleRepository", "--stacktrace", executableParameter)
        )

    private fun invocations(): Int =
        invocationLog.takeIf { it.exists() }?.readLines()?.count { it.startsWith("repo add") } ?: 0
}
