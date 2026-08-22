package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
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
    }
}
