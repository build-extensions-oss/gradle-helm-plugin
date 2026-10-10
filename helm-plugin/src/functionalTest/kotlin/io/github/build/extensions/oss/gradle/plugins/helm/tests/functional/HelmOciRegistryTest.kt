package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.HelmExecutable
import io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils.HelmInvocation
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Covers repositories whose URL is an OCI registry (`oci://...`).
 *
 * Such a registry cannot be registered with `helm repo add`, and has no index for `helm repo update` - but
 * `helm dependency update` can only pull from it after a `helm registry login`. So the dependency tasks must
 * log in to it themselves, right before they resolve the dependencies, without any manual `doFirst` in the
 * build script - which is what used to break the configuration cache. `helmAdd<X>Repository` logs in instead
 * of `helm repo add` when it is run on its own, and is no longer part of the dependency tasks' graph.
 *
 * What helm is asked to do is only observable in how the CLI ends up being called, so every build here runs
 * against a fake helm that records each invocation, and the assertions are on that record.
 */
internal class HelmOciRegistryTest {

    private companion object {
        const val GRADLE_VERSIONS =
            "io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils." +
                    "DefaultGradleRunnerParameters#getDefaultParameterSetWithoutHelmVersion"
    }

    private val sourceDirectory = File("./src/functionalTest/resources/test/oci-registry")

    @TempDir
    private lateinit var testProjectDir: File

    private val invocationLog: File
        get() = File(testProjectDir, "helm-invocations.log")

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun updateDependenciesShouldLoginToTheOciRegistryFirst(parameters: DefaultGradleRunnerParameters) {
        // when
        val output = build(parameters, "helmUpdateMainChartDependencies")

        // then
        output shouldContain "BUILD SUCCESSFUL"

        val invocations = recordedInvocations()
        // a single login: the one of the dependency task, not another one by helmAddArtifactoryRepository
        val login = invocations.single { it.command == "registry login" }
        login.arguments shouldContain "registry.example.com:5000"
        login shouldHaveOption ("--username" to "ci-user")
        login.arguments shouldContain "--password-stdin"
        login.arguments shouldNotContain "s3cr3t"
        login.stdin shouldBe "s3cr3t"

        val dependencyUpdate = invocations.single { it.command == "dependency update" }
        invocations.indexOf(login) shouldBeLessThan invocations.indexOf(dependencyUpdate)
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun ociRegistryShouldNeverBeAddedOrUpdatedAsAClassicRepository(parameters: DefaultGradleRunnerParameters) {
        // when
        val output = build(parameters, "helmUpdateMainChartDependencies")

        // then - `helm repo add oci://...` fails in a real helm, and there is no index to `helm repo update`
        output shouldContain "BUILD SUCCESSFUL"
        output shouldNotContain "Task :helmAddArtifactoryRepository"
        val commands = recordedInvocations().map { it.command }
        commands shouldNotContain "repo add"
        commands shouldNotContain "repo update"
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun registryLoginShouldNeverBeUpToDate(parameters: DefaultGradleRunnerParameters) {
        // when - the same build twice, nothing changed in between
        val firstRun = build(parameters, "helmAddArtifactoryRepository")
        val secondRun = build(parameters, "helmAddArtifactoryRepository")

        // then - a login leaves nothing we could check, and the token may have expired, so log in every time
        firstRun shouldContain "BUILD SUCCESSFUL"
        secondRun shouldContain "BUILD SUCCESSFUL"
        secondRun shouldNotContain "helmAddArtifactoryRepository UP-TO-DATE"
        val logins = recordedInvocations().filter { it.command == "registry login" }
        logins shouldHaveSize 2
        logins.forEach { login ->
            login.arguments shouldContain "--password-stdin"
            login.stdin shouldBe "s3cr3t"
        }
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun ociRegistryLoginShouldBeCompatibleWithTheConfigurationCache(parameters: DefaultGradleRunnerParameters) {
        // when
        val firstRun = build(parameters, "helmUpdateMainChartDependencies", "--configuration-cache")
        // A clean build must update the dependencies right away. Chart.yaml is generated during the build, so
        // this fails if the task decides from what the chart looked like when the cache entry was stored.
        HelmInvocation.readAll(invocationLog).map { it.command } shouldContain "dependency update"
        // As in HelmConfigurationCacheTest: the first run generates the chart's Chart.yaml, which the
        // dependency task configuration reads, so the entry is invalidated exactly once.
        val secondRun = build(parameters, "helmUpdateMainChartDependencies", "--configuration-cache")
        // Remove the dependency task's output, so that the run from the cache entry has to update the
        // dependencies - and so to log in - again. The login is then built entirely from the cache entry.
        File(testProjectDir, "build/helm/charts/oci-registry/charts").deleteRecursively()
        val thirdRun = build(parameters, "helmUpdateMainChartDependencies", "--configuration-cache")

        // then
        firstRun shouldContain "BUILD SUCCESSFUL"
        firstRun shouldContain "Configuration cache entry stored"

        secondRun shouldContain "BUILD SUCCESSFUL"

        thirdRun shouldContain "BUILD SUCCESSFUL"
        thirdRun shouldContain "Configuration cache entry reused"
        thirdRun shouldNotContain "Task :helmUpdateMainChartDependencies UP-TO-DATE"

        // the first run and the run from the cache entry log in; the credentials survive the round trip
        val logins = recordedInvocations().filter { it.command == "registry login" }
        logins shouldHaveSize 2
        logins.forEach { login ->
            login shouldHaveOption ("--username" to "ci-user")
            login.stdin shouldBe "s3cr3t"
        }
    }

    private fun build(parameters: DefaultGradleRunnerParameters, vararg arguments: String): String =
        GradleRunnerProvider.createRunner(
            parameters = parameters,
            projectDir = testProjectDir,
            arguments = listOf(
                *arguments,
                "-PregistryUser=ci-user",
                "-PregistryPassword=s3cr3t",
                "--stacktrace",
                HelmExecutable.getRecordingExecutableParameter(testProjectDir, invocationLog).parameterValue
            ),
        ).build().output

    private fun recordedInvocations(): List<HelmInvocation> =
        HelmInvocation.readAll(invocationLog)
}
