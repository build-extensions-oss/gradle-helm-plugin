package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional

import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.DefaultGradleRunnerParameters
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.GradleRunnerProvider
import io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils.HelmExecutable
import io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils.HelmInvocation
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Covers the registry login variants.
 */
internal class HelmRegistryLoginOptionsTest {

    private companion object {
        const val GRADLE_VERSIONS =
            "io.github.build.extensions.oss.gradle.plugins.helm.plugin.test.utils." +
                    "DefaultGradleRunnerParameters#getDefaultParameterSetWithoutHelmVersion"

        val CREDENTIAL_OPTIONS = listOf("--ca-file", "--cert-file", "--key-file", "--username", "--password-stdin")
    }

    private val sourceDirectory = File("./src/functionalTest/resources/test/registry-login-options")

    @TempDir
    private lateinit var testProjectDir: File

    private val invocationLog: File
        get() = File(testProjectDir, "helm-invocations.log")

    private val invocations: List<HelmInvocation>
        get() = HelmInvocation.readAll(invocationLog)

    private val logins: List<HelmInvocation>
        get() = invocations.filter { it.command == "registry login" }

    @BeforeEach
    fun setup() {
        sourceDirectory.copyRecursively(target = testProjectDir)
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun registriesShouldBeLoggedInToWithTheirOwnCredentials(parameters: DefaultGradleRunnerParameters) {
        // when
        val output = build(parameters, "helmUpdateMainChartDependencies")

        // then
        output shouldContain "BUILD SUCCESSFUL"
        // the alias and the classic repository need no login, and no dependency comes from "oci.example.com"
        logins.map { it.arguments.last() } shouldContainExactly listOf(
            "anonymous.example.com",
            "certificate.example.com"
        )

        val anonymousLogin = logins.single { it.arguments.last() == "anonymous.example.com" }
        CREDENTIAL_OPTIONS.forEach { anonymousLogin.arguments shouldNotContain it }

        val certificateLogin = logins.single { it.arguments.last() == "certificate.example.com" }
        certificateLogin.shouldHaveTlsFiles()
        certificateLogin.arguments shouldNotContain "--username"
        certificateLogin.arguments shouldNotContain "--password-stdin"
        certificateLogin.stdin shouldBe ""

        // read the log once: HelmInvocation has no equals, so indexOf needs the very same instances
        val allInvocations = invocations
        val dependencyUpdate = allInvocations.single { it.command == "dependency update" }
        allInvocations.filter { it.command == "registry login" }.forEach { login ->
            allInvocations.indexOf(login) shouldBeLessThan allInvocations.indexOf(dependencyUpdate)
        }
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun chartWithApiVersionV1ShouldReadItsDependenciesFromRequirementsYaml(parameters: DefaultGradleRunnerParameters) {
        // when
        val output = build(parameters, "helmUpdateLegacyChartDependencies")

        // then
        output shouldContain "BUILD SUCCESSFUL"
        logins.map { it.arguments.last() } shouldContainExactly listOf("anonymous.example.com")
        invocations.map { it.command } shouldContain "dependency update"
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun chartWithApiVersionV1WithoutRequirementsYamlShouldHaveNoDependencies(
        parameters: DefaultGradleRunnerParameters
    ) {
        // when
        val output = build(parameters, "helmUpdateEmptyChartDependencies")

        // then
        output shouldContain "BUILD SUCCESSFUL"
        output shouldContain "Task :helmUpdateEmptyChartDependencies SKIPPED"
        logins.shouldBeEmpty()
        invocations.map { it.command } shouldNotContain "dependency update"
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun ociRepositoryShouldBeLoggedInToWithItsTlsFiles(parameters: DefaultGradleRunnerParameters) {
        // when
        val output = build(parameters, "helmAddOciWithCertificateRepository")

        // then
        output shouldContain "BUILD SUCCESSFUL"
        val login = logins.single()
        login.arguments.last() shouldBe "oci.example.com"
        login.shouldHaveTlsFiles()
        login.arguments shouldNotContain "--username"
        login.arguments shouldNotContain "--password-stdin"
        invocations.map { it.command } shouldNotContain "repo add"
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun adHocRegistryLoginShouldPassTlsFilesOnly(parameters: DefaultGradleRunnerParameters) {
        // when
        val output = build(parameters, "helmTlsRegistryLogin")

        // then
        output shouldContain "BUILD SUCCESSFUL"
        val login = logins.single()
        login.arguments.last() shouldBe "tls.example.com:8443"
        login.shouldHaveTlsFiles()
        login.arguments shouldNotContain "--username"
        login.arguments shouldNotContain "--password-stdin"
        login.stdin shouldBe ""
    }

    @ParameterizedTest
    @MethodSource(GRADLE_VERSIONS)
    fun onlyClassicRepositoriesShouldBeAddedAndUpdated(parameters: DefaultGradleRunnerParameters) {
        // when
        val output = build(parameters, "helmUpdateRepositories")

        // then
        output shouldContain "BUILD SUCCESSFUL"
        output shouldContain "Task :helmAddClassicRepository"
        output shouldNotContain "Task :helmAddOciWithCertificateRepository"

        val repoAdd = invocations.single { it.command == "repo add" }
        repoAdd.arguments shouldContain "classic"
        repoAdd.arguments shouldContain "https://charts.example.com"
        logins.shouldBeEmpty()
    }

    private fun HelmInvocation.shouldHaveTlsFiles() {
        optionValue("--ca-file") shouldEndWith "ca.pem"
        optionValue("--cert-file") shouldEndWith "client.pem"
        optionValue("--key-file") shouldEndWith "client.key"
    }

    private fun HelmInvocation.optionValue(name: String): String {
        arguments shouldContain name
        return arguments[arguments.indexOf(name) + 1]
    }

    private fun build(parameters: DefaultGradleRunnerParameters, vararg arguments: String): String =
        GradleRunnerProvider.createRunner(
            parameters = parameters,
            projectDir = testProjectDir,
            arguments = listOf(
                *arguments,
                "--stacktrace",
                HelmExecutable.getRecordingExecutableParameter(testProjectDir, invocationLog).parameterValue
            ),
        ).build().output
}
