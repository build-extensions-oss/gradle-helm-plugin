package io.github.build.extensions.oss.gradle.plugins.helm.releases.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.HelmExecutable
import io.kotest.matchers.string.shouldContain
import java.io.File
import kotlin.io.path.Path
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class InstallFromAnotherProject {
    private val sourceDirectory = File("./src/functionalTest/resources/test/install-from-another-project")

    @TempDir
    private lateinit var testProjectDir: File

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)
    }

    @ParameterizedTest
    @MethodSource("io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters#getDefaultParameterSet")
    fun shouldSupportBuildingChartInFirstProjectAndPublishInAnother(parameters: DefaultGradleRunnerParameters) {
        // given
        val destinationChartArchive =
            Path(testProjectDir.path, "build", "helm", "charts", "${testProjectDir.name}-unspecified.tgz")
                .toFile()
        val helmExecutableParameter =
            HelmExecutable.getExecutableParameterForChartCreation(testProjectDir, destinationChartArchive)

        val arguments = listOf(
            "helmInstall",
            "--stacktrace",
            helmExecutableParameter.parameterValue
        )
        val gradleRunner = GradleRunnerProvider.createRunner(
            parameters = parameters,
            projectDir = testProjectDir,
            arguments = arguments,
        )

        // when
        val result = gradleRunner.build()

        // then
        val output = result.output

        output shouldContain "BUILD SUCCESSFUL"
        output shouldContain "helmInstall"
    }
}
