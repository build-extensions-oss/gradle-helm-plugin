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
 * Checks that a `helmAdd<X>Repository` task with `failIfExists` fails when the repository is already registered, and
 * that it does so without invoking Helm.
 */
internal class HelmAddRepositoryFailIfExistsTest {

    private val sourceDirectory = File("./src/functionalTest/resources/test/repository-fail-if-exists")

    @TempDir(deletionStrategy = TempDirDeletionStrategy.IgnoreFailures::class)
    private lateinit var testProjectDir: File

    @TempDir(deletionStrategy = TempDirDeletionStrategy.IgnoreFailures::class)
    private lateinit var executableDir: File

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)
    }

    @ParameterizedTest
    @MethodSource("io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters#getDefaultParameterSetWithoutHelmVersion")
    fun helmAddRepositoryShouldFailWhenTheRepositoryIsRegistered(parameters: DefaultGradleRunnerParameters) {
        // given the repository registered in the plugin's own configuration directory
        val configDirectory = testProjectDir.resolve("build/helm/config/helm")
        check(configDirectory.mkdirs()) { "Unable to create $configDirectory" }
        configDirectory.resolve("repositories.yaml").writeText(
            """
                repositories:
                  - name: example
                    url: http://127.0.0.1:9/repository
            """.trimIndent()
        )

        val invocationLog = testProjectDir.resolve("helm-invocations.log")
        val executable = HelmExecutable.getRecordingExecutableParameter(executableDir, invocationLog)

        // when
        val result = GradleRunnerProvider.createRunner(
            parameters = parameters,
            projectDir = testProjectDir,
            arguments = listOf("helmAddExampleRepository", "--stacktrace", executable.parameterValue)
        ).buildAndFail()

        // then
        result.output shouldContain "already registered"
        result.output shouldContain "example"
        invocationLog.exists() shouldBe false
    }
}
