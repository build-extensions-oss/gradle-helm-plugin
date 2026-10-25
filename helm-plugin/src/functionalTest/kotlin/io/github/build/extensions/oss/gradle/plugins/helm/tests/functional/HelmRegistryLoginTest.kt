package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.HelmExecutable
import io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils.HelmInvocation
import io.kotest.matchers.collections.shouldBeEmpty
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
 * Covers `helm.registries`: OCI registries that a chart pulls from without them being declared in
 * `helm.repositories` - the registry only appears as an `oci://` dependency URL in `Chart.yaml`.
 *
 * The dependency tasks must log in by themselves, from their own task action: so only when they actually run,
 * and only to registries that the chart's dependencies come from. The password must reach helm on the standard
 * input (`--password-stdin`), never on the command line. And none of it may need a `doFirst` in the build
 * script, which captured the Project and so broke the configuration cache.
 *
 * Also covers the standalone `HelmRegistryLogin` task, which must not be wired to anything automatically.
 *
 * What helm is asked to do is only observable in how the CLI ends up being called, so every build here runs
 * against a fake helm that records each invocation and its standard input, and the assertions are on that record.
 */
internal class HelmRegistryLoginTest {

    private companion object {
        const val GRADLE_VERSIONS =
            "io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils." +
                    "DefaultGradleRunnerParameters#getDefaultParameterSetWithoutHelmVersion"
    }

    private val sourceDirectory = File("./src/functionalTest/resources/test/registry-login")

    @TempDir
    private lateinit var testProjectDir: File

    private val invocationLog: File
        get() = File(testProjectDir, "helm-invocations.log")

    private val logins: List<HelmInvocation>
        get() = HelmInvocation.readAll(invocationLog).filter { it.command == "registry login" }

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun updateDependenciesShouldLoginToTheRegistryOfItsDependencies(parameters: DefaultGradleRunnerParameters) {
        // when
        val output = build(parameters, "helmUpdateMainChartDependencies")

        // then
        output shouldContain "BUILD SUCCESSFUL"
        // neither the ad-hoc HelmRegistryLogin task nor any other task was pulled in to log in
        output shouldNotContain "Task :helmRegistryLogin"

        val invocations = HelmInvocation.readAll(invocationLog)
        // only the registry that Chart.yaml pulls from - not "unused.example.com"
        val login = logins.single()
        login.arguments shouldContain "registry.example.com"
        login shouldHaveOption ("--username" to "ci-user")

        val dependencyUpdate = invocations.single { it.command == "dependency update" }
        invocations.indexOf(login) shouldBeLessThan invocations.indexOf(dependencyUpdate)
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun passwordShouldBePassedOnTheStandardInput(parameters: DefaultGradleRunnerParameters) {
        // when
        build(parameters, "helmUpdateMainChartDependencies")

        // then - helm warns that --password on the command line is insecure: other processes can read it
        val login = logins.single()
        login.arguments shouldContain "--password-stdin"
        login.arguments shouldNotContain "--password"
        login.arguments shouldNotContain "s3cr3t"
        login.stdin shouldBe "s3cr3t"

        // and no other command gets the password
        HelmInvocation.readAll(invocationLog)
            .filter { it.command != "registry login" }
            .forEach { it.stdin shouldBe "" }
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun upToDateDependenciesShouldNotLogin(parameters: DefaultGradleRunnerParameters) {
        // when - the same build twice, nothing changed in between
        val firstRun = build(parameters, "helmUpdateMainChartDependencies")
        val secondRun = build(parameters, "helmUpdateMainChartDependencies")

        // then - the second run has nothing to download, so it must not reach out to the registry either
        firstRun shouldContain "BUILD SUCCESSFUL"
        secondRun shouldContain "BUILD SUCCESSFUL"
        secondRun shouldContain "Task :helmUpdateMainChartDependencies UP-TO-DATE"
        logins shouldHaveSize 1
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun adHocRegistryLoginTaskShouldLoginEveryTime(parameters: DefaultGradleRunnerParameters) {
        // when - the same build twice, nothing changed in between
        val firstRun = build(parameters, "helmRegistryLogin")
        val secondRun = build(parameters, "helmRegistryLogin")

        // then - a login leaves nothing we could check, and the token may have expired, so log in every time
        firstRun shouldContain "BUILD SUCCESSFUL"
        secondRun shouldContain "BUILD SUCCESSFUL"
        secondRun shouldNotContain "helmRegistryLogin UP-TO-DATE"

        logins shouldHaveSize 2
        logins.forEach { login ->
            login.arguments shouldContain "adhoc.example.com"
            login.arguments shouldContain "--password-stdin"
            login.stdin shouldBe "s3cr3t"
        }
        // it is a login only; it does not drag the chart tasks in
        HelmInvocation.readAll(invocationLog).filter { it.command != "registry login" }.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun helmPackageWithRegistryLoginShouldBeCompatibleWithTheConfigurationCache(
        parameters: DefaultGradleRunnerParameters
    ) {
        // when
        val firstRun = build(parameters, "helmPackage", "--configuration-cache")
        // A clean build must update the dependencies right away. Chart.yaml is generated during the build, so
        // this fails if the task decides from what the chart looked like when the cache entry was stored.
        HelmInvocation.readAll(invocationLog).map { it.command } shouldContain "dependency update"
        // As in HelmConfigurationCacheTest: the first run generates the chart's Chart.yaml, which the
        // dependency task configuration reads, so the entry is invalidated exactly once.
        val secondRun = build(parameters, "helmPackage", "--configuration-cache")
        // Remove the dependency task's output, so that the run from the cache entry has to update the
        // dependencies - and so to log in - again. The login is then built entirely from the cache entry.
        File(testProjectDir, "build/helm/charts/registry-login/charts").deleteRecursively()
        val thirdRun = build(parameters, "helmPackage", "--configuration-cache")

        // then
        firstRun shouldContain "BUILD SUCCESSFUL"
        firstRun shouldContain "Configuration cache entry stored"

        secondRun shouldContain "BUILD SUCCESSFUL"

        thirdRun shouldContain "BUILD SUCCESSFUL"
        thirdRun shouldContain "Configuration cache entry reused"
        thirdRun shouldNotContain "Task :helmUpdateMainChartDependencies UP-TO-DATE"

        // the first run and the run from the cache entry log in; the credentials survive the round trip
        logins shouldHaveSize 2
        logins.forEach { login ->
            login.arguments shouldContain "registry.example.com"
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
}
