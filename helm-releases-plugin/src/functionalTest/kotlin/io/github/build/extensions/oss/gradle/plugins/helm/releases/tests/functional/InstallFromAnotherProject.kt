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
    // we limit our testing to the most recent versions of Gradle
    @MethodSource("io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters#getLatestParameterSet")
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
        val result = gradleRunner.buildAndFail()

        // then
        val output = result.output

        // this is a hack, however it is the best which we can do right now.
        // We can prepare helm chart, however we can't run honest installation without Kubernetes running.
        // The fix should be done via https://github.com/build-extensions-oss/gradle-helm-plugin/issues/62
        // However, as of now we run the plugin until helmInstall command and then we stop.
        output shouldContain "FAILURE: Build failed with an exception."
        output shouldContain "Task :project-which-installs:helmInstallTestToDefault FAILED"
        output shouldContain "at io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmInstallOrUpgrade.shouldUseInstallReplace"
        output shouldContain "finished with non-zero exit value 1"
    }
}
