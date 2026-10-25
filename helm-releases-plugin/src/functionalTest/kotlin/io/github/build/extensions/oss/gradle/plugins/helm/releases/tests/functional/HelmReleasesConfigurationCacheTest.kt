package io.github.build.extensions.oss.gradle.plugins.helm.releases.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.HelmExecutable
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Checks that a build using the Helm releases plugin works with the Gradle configuration cache.
 *
 * `--configuration-cache` fails the build on any configuration cache problem by default, so a successful
 * first run already asserts that the plugin reports none. That is what covers the release DSL objects: a
 * `HelmRelease` holds a `Project`, so anything that makes one reachable from task state - an `onlyIf` spec,
 * most of all - shows up here immediately.
 *
 * Re-running matters just as much: an entry that stores fine can still fail to load back, which is a
 * separate class of bug and is not visible from a single run.
 *
 * Unlike [InstallFromAnotherProject], this test uses a helm executable which succeeds for every command, so
 * that the build actually reaches the point of storing and reusing an entry.
 */
internal class HelmReleasesConfigurationCacheTest {

    private val sourceDirectory = File("./src/functionalTest/resources/test/configuration-cache")

    @TempDir
    private lateinit var testProjectDir: File

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)
    }

    @ParameterizedTest
    @MethodSource(
        "io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils." +
                "DefaultGradleRunnerParameters#getDefaultParameterSetWithoutHelmVersion"
    )
    fun helmInstallShouldBeCompatibleWithTheConfigurationCache(parameters: DefaultGradleRunnerParameters) {
        assertStoresAndReusesTheEntry(parameters, task = "helmInstall")
    }

    @ParameterizedTest
    @MethodSource(
        "io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils." +
                "DefaultGradleRunnerParameters#getDefaultParameterSetWithoutHelmVersion"
    )
    fun helmUninstallShouldBeCompatibleWithTheConfigurationCache(parameters: DefaultGradleRunnerParameters) {
        assertStoresAndReusesTheEntry(parameters, task = "helmUninstall")
    }

    @ParameterizedTest
    @MethodSource(
        "io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils." +
                "DefaultGradleRunnerParameters#getDefaultParameterSetWithoutHelmVersion"
    )
    fun helmTestShouldBeCompatibleWithTheConfigurationCache(parameters: DefaultGradleRunnerParameters) {
        assertStoresAndReusesTheEntry(parameters, task = "helmTest")
    }

    @ParameterizedTest
    @MethodSource(
        "io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils." +
                "DefaultGradleRunnerParameters#getDefaultParameterSetWithoutHelmVersion"
    )
    fun helmStatusShouldBeCompatibleWithTheConfigurationCache(parameters: DefaultGradleRunnerParameters) {
        assertStoresAndReusesTheEntry(parameters, task = ":project-which-installs:helmStatusAwesome")
    }

    private fun assertStoresAndReusesTheEntry(parameters: DefaultGradleRunnerParameters, task: String) {
        // given / when
        val firstRun = run(parameters, task)
        // Configuring the chart dependency tasks reads the *generated*
        // build/helm/charts/<chart>/Chart.yaml, to work out whether the chart uses Chart.lock or
        // requirements.lock. Producing that file during the first run therefore invalidates the entry
        // exactly once; from the third run onwards it settles and is reused.
        val secondRun = run(parameters, task)
        val thirdRun = run(parameters, task)

        // then
        firstRun shouldContain "BUILD SUCCESSFUL"
        firstRun shouldContain "Configuration cache entry stored"

        secondRun shouldContain "BUILD SUCCESSFUL"

        thirdRun shouldContain "BUILD SUCCESSFUL"
        thirdRun shouldContain "Configuration cache entry reused"
    }

    private fun run(parameters: DefaultGradleRunnerParameters, task: String): String {
        val helmExecutableParameter = HelmExecutable.getNoOpExecutableParameter(testProjectDir)

        return GradleRunnerProvider.createRunner(
            parameters = parameters,
            projectDir = testProjectDir,
            arguments = listOf(
                task,
                "--configuration-cache",
                "--stacktrace",
                helmExecutableParameter.parameterValue
            ),
        ).build().output
    }
}
