package io.github.build.extensions.oss.gradle.plugins.helm.publishing.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.HelmExecutable
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.io.path.Path

/**
 * Test checks HelmChartPublishConvention class. We add new property for existing Gradle extension, so we have to check that.
 *
 * This test doesn't check anything except the existence of the particular task
 */
internal class PublishExtensionTest {
    private val sourceDirectory = File("./src/functionalTest/resources/test/publish-extension-property")

    companion object {
        @JvmStatic
        fun createTestCases(): Stream<Arguments> {
            return listOf(true, false).flatMap { createPublishingTask ->
                DefaultGradleRunnerParameters.onlyWithNewKotlinDslSupport.map { parameters ->
                    Arguments.of(
                        createPublishingTask, parameters
                    )
                }
            }.stream()
        }
    }

    @TempDir
    private lateinit var testProjectDir: File

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)
    }

    /**
     * The test goal is to be sure that we can read publishing property. So, we need to check both conditions -
     * if user wants to publish a chart or not.
     *
     * The logic is:
     * 1. If users sets "publish = true" - we will try to parse URL and then we will fail.
     * 2. If users sets "publish = false" then we shouldn't even try to understand what should we publish
     */
    @ParameterizedTest
    @MethodSource("createTestCases")
    fun helmPublishShouldCallArtifactoryWithCredentials(
        createPublishingTask: Boolean,
        parameters: DefaultGradleRunnerParameters
    ) {
        // given
        val destinationChartArchive =
            Path(testProjectDir.path, "build", "helm", "charts", "${testProjectDir.name}-unspecified.tgz")
                .toFile()
        val helmExecutableParameter =
            HelmExecutable.getExecutableParameterForChartCreation(testProjectDir, destinationChartArchive)

        val gradleRunner = GradleRunnerProvider.createRunner(
            parameters = parameters,
            projectDir = testProjectDir,
            arguments = listOf(
                "helmPublish",
                "--stacktrace",
                helmExecutableParameter.parameterValue,
                "-PpublishHelmPlugin=$createPublishingTask"
            ),
        )

        // then
        // different options - if we publish or not
        if (createPublishingTask) {
            // if we asked to create publishing task - we should fail, because the port isn't correct.
            val result = gradleRunner.buildAndFail()
            val output = result.output
            // check if we failed
            output shouldContain "Invalid URL port: \"999999\""
        } else {
            val result = gradleRunner.build()
            val output = result.output
            // check that we were able to run despite and invalid port.
            // E.g. it means that the problematic lambda wasn't even executed.
            output shouldContain "BUILD SUCCESSFUL"
            // check that helm publishing task doesn't exist - if we did not ask
            output shouldNotContain "helmPublish - Publishes all Helm charts."
        }
    }
}
