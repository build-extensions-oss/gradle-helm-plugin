package io.github.build.extensions.oss.gradle.plugins.helm.releases.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.HelmExecutable
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Covers the two task rules behind `helm status` for a release:
 *
 * - `HelmStatusReleaseOnTargetTaskRule` creates `helmStatus<Release>On<Target>`, a `HelmStatus` task
 *   configured from the release *as resolved for that target* - its release name and the target's server
 *   options - and skipped when the release's tags do not match the target's selection.
 * - `HelmStatusReleaseTaskRule` creates `helmStatus<Release>`, a lifecycle task that runs the former for
 *   whichever target is active, as chosen by the `helm.release.target` property.
 *
 * What the rules produce is only observable in how the helm CLI ends up being called, so every build here
 * runs against a fake helm that records each invocation, and the assertions are on that record.
 */
internal class HelmStatusReleaseTest {

    private companion object {
        const val GRADLE_VERSIONS =
            "io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils." +
                    "DefaultGradleRunnerParameters#getDefaultParameterSetWithoutHelmVersion"
    }

    private val sourceDirectory = File("./src/functionalTest/resources/test/release-status")

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
    fun statusTaskShouldRunAgainstTheDefaultTargetWhenNoneIsSelected(parameters: DefaultGradleRunnerParameters) {
        // when
        val output = build(parameters, "helmStatusAwesome")

        // then
        output shouldContain "BUILD SUCCESSFUL"
        statusTasksIn(output) shouldBe setOf("helmStatusAwesome", "helmStatusAwesomeOnDefault")

        val invocation = recordedInvocations().single()
        invocation.command shouldBe "status"
        invocation.arguments shouldContain "awesome-release"
        invocation shouldHaveOption ("--namespace" to "default-ns")
        invocation.arguments shouldNotContain "--kube-context"
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun statusTaskShouldFollowTheActiveTargetProperty(parameters: DefaultGradleRunnerParameters) {
        // when
        val output = build(parameters, "helmStatusAwesome", "-Phelm.release.target=staging")

        // then
        output shouldContain "BUILD SUCCESSFUL"
        statusTasksIn(output) shouldBe setOf("helmStatusAwesome", "helmStatusAwesomeOnStaging")

        val invocation = recordedInvocations().single()
        invocation.command shouldBe "status"
        // the target-specific release, not the plain one: forTarget('staging') renames it
        invocation.arguments shouldContain "awesome-on-staging"
        invocation.arguments shouldNotContain "awesome-release"
        // and the server options of the staging target, each by its own route into the CLI
        invocation shouldHaveOption ("--namespace" to "staging-ns")
        invocation shouldHaveOption ("--kube-context" to "staging-ctx")
        File(invocation.kubeConfig).canonicalFile shouldBe File(testProjectDir, "staging.kubeconfig").canonicalFile
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun statusTaskForASpecificTargetShouldBeRunnableDirectly(parameters: DefaultGradleRunnerParameters) {
        // when - the active target is still "default"; the per-target task must not depend on it
        val output = build(parameters, "helmStatusAwesomeOnStaging")

        // then
        output shouldContain "BUILD SUCCESSFUL"
        statusTasksIn(output) shouldBe setOf("helmStatusAwesomeOnStaging")

        val invocation = recordedInvocations().single()
        invocation.command shouldBe "status"
        invocation.arguments shouldContain "awesome-on-staging"
        invocation shouldHaveOption ("--namespace" to "staging-ns")
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun statusTaskShouldNeverBeUpToDate(parameters: DefaultGradleRunnerParameters) {
        // when - the same build twice, nothing changed in between
        val firstRun = build(parameters, "helmStatusAwesome")
        val secondRun = build(parameters, "helmStatusAwesome")

        // then - a status check queries the live cluster, so the second run must ask helm again rather
        // than report the first answer as up-to-date
        firstRun shouldContain "BUILD SUCCESSFUL"
        secondRun shouldContain "BUILD SUCCESSFUL"
        secondRun shouldNotContain "helmStatusAwesomeOnDefault UP-TO-DATE"

        recordedInvocations() shouldHaveSize 2
        recordedInvocations().forEach { invocation ->
            invocation.command shouldBe "status"
            invocation.arguments shouldContain "awesome-release"
        }
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun statusTaskShouldBeSkippedWhenTheReleaseTagsDoNotMatch(parameters: DefaultGradleRunnerParameters) {
        // when - the release is tagged "smoke"; select something else
        val output = build(parameters, "helmStatusAwesomeOnDefault", "-Phelm.release.tags=nomatch")

        // then
        output shouldContain "BUILD SUCCESSFUL"
        output shouldContain "Task :helmStatusAwesomeOnDefault SKIPPED"
        recordedInvocations().shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun statusTaskShouldFailForAnUnknownActiveTarget(parameters: DefaultGradleRunnerParameters) {
        // when
        val output = buildAndFail(parameters, "helmStatusAwesome", "-Phelm.release.target=nope")

        // then - resolving the lifecycle task's dependency is what surfaces the error
        output shouldContain "The Helm release target \"nope\" does not exist"
        output shouldContain "Available release targets are: [default, staging]"
        recordedInvocations().shouldBeEmpty()
    }

    private fun build(parameters: DefaultGradleRunnerParameters, vararg arguments: String): String =
        runner(parameters, *arguments).build().output

    private fun buildAndFail(parameters: DefaultGradleRunnerParameters, vararg arguments: String): String =
        runner(parameters, *arguments).buildAndFail().output

    private fun runner(parameters: DefaultGradleRunnerParameters, vararg arguments: String) =
        GradleRunnerProvider.createRunner(
            parameters = parameters,
            projectDir = testProjectDir,
            arguments = listOf(
                *arguments,
                "--stacktrace",
                HelmExecutable.getRecordingExecutableParameter(testProjectDir, invocationLog).parameterValue
            ),
        )

    /**
     * The names of every `helmStatus*` task Gradle reported on, whatever its outcome.
     */
    private fun statusTasksIn(output: String): Set<String> =
        Regex("""Task :(helmStatus\w+)""").findAll(output)
            .map { it.groupValues[1] }
            .toSet()

    /**
     * One line per helm call, as written by the recording executable: the arguments, then `KUBECONFIG=<value>`.
     */
    private fun recordedInvocations(): List<HelmInvocation> =
        if (!invocationLog.exists()) {
            emptyList()
        } else {
            invocationLog.readLines()
                .filter { it.isNotBlank() }
                .map { line ->
                    HelmInvocation(
                        arguments = line.substringBeforeLast(" KUBECONFIG=").trim().split(Regex("\\s+")),
                        kubeConfig = line.substringAfterLast(" KUBECONFIG=").trim()
                    )
                }
        }

    private class HelmInvocation(val arguments: List<String>, val kubeConfig: String) {

        val command: String
            get() = arguments.first()
    }

    private infix fun HelmInvocation.shouldHaveOption(option: Pair<String, String>) {
        val (name, value) = option
        arguments shouldContain name
        arguments.getOrNull(arguments.indexOf(name) + 1) shouldBe value
    }
}
