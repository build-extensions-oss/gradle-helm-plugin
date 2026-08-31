package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.kotest.matchers.file.exist
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * The test does a smoke check of the dependent helm chart, which would be used in another test.
 * This one must do anything complex, because the goal is to briefly check that everything is ok.
 */
internal class HelmRepositoryDependentChartQuickRenderTest {

    private val sourceDirectory = File("./src/functionalTest/resources/test/render-with-repository")

    @TempDir
    private lateinit var testProjectDir: File

    private lateinit var helmRegistryDirectory: File

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)

        helmRegistryDirectory = testProjectDir.resolve("helm-registry")
        check(helmRegistryDirectory.mkdir()) {
            "Unable to create Helm registry directory at $helmRegistryDirectory"
        }
    }

    @ParameterizedTest
    @MethodSource("io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters#getDefaultParameterSet")
    fun dependentChartShouldBePackagedIntoHelmRegistry(parameters: DefaultGradleRunnerParameters) {
        // given
        val gradleRunner = GradleRunnerProvider.createRunner(
            parameters = parameters,
            projectDir = testProjectDir.resolve("dependent-chart"),
            arguments = listOf("copyDependentChartToHelmRegistry", "--stacktrace"),
        )

        // when
        val result = gradleRunner.build()

        // then
        result.output shouldContain "BUILD SUCCESSFUL"
        result.output shouldContain "Task :helmPackageMainChart"
        result.output shouldContain "Task :copyDependentChartToHelmRegistry"

        helmRegistryDirectory.resolve("dependent-chart-1.2.3.tgz") should exist()
    }
}
