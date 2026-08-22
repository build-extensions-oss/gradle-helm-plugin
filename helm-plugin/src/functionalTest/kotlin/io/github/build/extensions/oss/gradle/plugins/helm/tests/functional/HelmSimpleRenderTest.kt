package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.kotest.matchers.file.containFile
import io.kotest.matchers.file.exist
import io.kotest.matchers.should
import io.kotest.matchers.string.contain
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class HelmSimpleRenderTest {

    private val sourceDirectory = File("./src/functionalTest/resources/test/render-simple")

    @TempDir
    private lateinit var testProjectDir: File

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)
    }

    @ParameterizedTest
    @MethodSource("io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters#getDefaultParameterSet")
    fun helmRenderShouldRenderTheChart(parameters: DefaultGradleRunnerParameters) {
        // given
        val gradleRunner = GradleRunnerProvider.createRunner(
            parameters = parameters,
            projectDir = testProjectDir,
            arguments = listOf("helmRender", "--stacktrace"),
        )

        // when
        val result = gradleRunner.build()

        // then
        val output = result.output

        output shouldContain "BUILD SUCCESSFUL"
        // check that the desired task had been executed
        output shouldContain "Task :helmRender"
        // check that the linting task had been executed
        output shouldContain "Task :helmLintMainChart"

        val folderWithChart = testProjectDir.resolve("build/helm/charts/helmRenderProjectName")
        folderWithChart should exist()
        // check that the main yaml file was copied
        folderWithChart should containFile("Chart.yaml")

        val renderedServiceYaml = testProjectDir.resolve("build/helm/render/main/default/helmRenderProjectName/templates/service.yaml")
        renderedServiceYaml should exist()
        // check that we render the project name into the file.
        // e.g. the line 'name: xxxxx{{ .Chart.Name }}' should be converted to what we check below
        renderedServiceYaml.readText() should contain("name: xxxxxhelmRenderProjectName")
    }
}
